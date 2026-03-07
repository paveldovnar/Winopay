package com.winopay.payments

import android.util.Log
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import com.winopay.solana.SolanaAddressUtils
import com.winopay.solana.pay.PaymentDetectionResult
import com.winopay.solana.pay.SolanaPayPaymentDetector
import com.winopay.solana.pay.SolanaPayUrlBuilder
import com.winopay.solana.pay.TransactionValidator
import com.winopay.solana.rpc.RpcCallResult
import com.winopay.solana.rpc.RpcProviderManager
import com.winopay.solana.rpc.RpcResult
import com.winopay.solana.rpc.SolanaRpcProvider
import com.winopay.solana.transaction.SolanaTransactionBuilder

/**
 * Solana implementation of PaymentRail.
 *
 * Thin wrapper around existing Solana-specific components:
 * - SolanaPayUrlBuilder: builds Solana Pay URLs
 * - SolanaAddressUtils: derives ATAs
 * - SolanaTokenPolicy: dual stablecoin acceptance policy
 * - TransactionValidator: validates transactions
 * - SolanaPayPaymentDetector: foreground detection
 *
 * Behavior is 1:1 with current application.
 */
class SolanaPaymentRail : PaymentRail {

    companion object {
        private const val TAG = "SolanaPaymentRail"
        const val RAIL_ID = "solana"

        /** Network IDs that use Solana rail */
        val SUPPORTED_NETWORKS = setOf("devnet", "mainnet", "mainnet-beta", "testnet")

        fun supportsNetwork(networkId: String): Boolean {
            return networkId.lowercase() in SUPPORTED_NETWORKS
        }
    }

    override val railId: String = RAIL_ID

    override val networkId: String = com.winopay.BuildConfig.SOLANA_CLUSTER

    override val tokenPolicy: TokenPolicyProvider = SolanaTokenPolicy

    override fun getSupportedTokens(): List<SupportedToken> {
        return listOf(
            SupportedToken(
                tokenId = SolanaTokenPolicy.USDC_MINT,
                symbol = "USDC",
                displayName = "USDC on Solana",
                decimals = 6,
                isEnabled = true
            ),
            SupportedToken(
                tokenId = SolanaTokenPolicy.USDT_MINT,
                symbol = "USDT",
                displayName = "USDT on Solana",
                decimals = 6,
                isEnabled = true // Both enabled on Solana
            )
        )
    }

    /**
     * Detector is lazy to break circular dependency.
     * Detector needs PaymentRail; Rail owns Detector.
     * Lazy ensures detector is created AFTER rail is fully constructed.
     */
    private val detector by lazy { SolanaPayPaymentDetector(this) }

    override fun buildPaymentRequest(invoice: InvoiceEntity): String {
        return SolanaPayUrlBuilder.buildUrl(invoice)
    }

    override fun generateReference(): String {
        return SolanaPayUrlBuilder.generateReference()
    }

    override fun deriveTokenAddress(ownerWallet: String, tokenMint: String): String {
        return SolanaAddressUtils.deriveAssociatedTokenAddress(ownerWallet, tokenMint)
    }

    override fun derivePollingTargets(invoice: InvoiceEntity): List<PollingTarget> {
        val expectedMint = invoice.splTokenMint

        // SOL payment - poll wallet directly
        if (expectedMint.isNullOrBlank()) {
            return listOf(PollingTarget(
                address = invoice.recipientAddress,
                tokenMint = null,
                label = "wallet",
                strategy = PollingStrategy.DERIVED_ATA
            ))
        }

        // SPL payment - get allowed mints from policy and derive ATAs
        val allowedMints = tokenPolicy.getAllowedTokens(expectedMint)

        return allowedMints.map { mint ->
            val ata = SolanaAddressUtils.deriveAssociatedTokenAddress(invoice.recipientAddress, mint)
            val label = when (mint) {
                SolanaTokenPolicy.USDC_MINT -> "USDC_ATA"
                SolanaTokenPolicy.USDT_MINT -> "USDT_ATA"
                else -> "ATA"
            }
            PollingTarget(
                address = ata,
                tokenMint = mint,
                label = label,
                strategy = PollingStrategy.DERIVED_ATA
            )
        }
    }

    /**
     * Discover actual token accounts for fallback detection (Strategy B).
     *
     * WHEN TO USE:
     * - After Strategy A (derived ATAs) finds no signatures
     * - Catches tokens sent to non-standard accounts
     *
     * IMPLEMENTATION:
     * 1. Call getTokenAccountsByOwner for each allowed mint
     * 2. Filter to accounts not already in derived targets
     * 3. Return as DISCOVERED_ACCOUNT strategy targets
     */
    override suspend fun discoverTokenAccountTargets(invoice: InvoiceEntity): List<PollingTarget> {
        val expectedMint = invoice.splTokenMint

        // SOL doesn't have token accounts
        if (expectedMint.isNullOrBlank()) {
            return emptyList()
        }

        val allowedMints = tokenPolicy.getAllowedTokens(expectedMint)
        val derivedTargets = derivePollingTargets(invoice)
        val derivedAddresses = derivedTargets.map { it.address }.toSet()

        val discoveredTargets = mutableListOf<PollingTarget>()

        Log.d(TAG, "━━━━━ FALLBACK: Discovering token accounts ━━━━━")
        Log.d(TAG, "  owner: ${invoice.recipientAddress}")
        Log.d(TAG, "  allowedMints: ${allowedMints.joinToString { it.take(12) + "..." }}")

        for (mint in allowedMints) {
            val result = RpcProviderManager.getTokenAccountsByOwner(
                owner = invoice.recipientAddress,
                mint = mint
            )

            when (result) {
                is RpcCallResult.Success -> {
                    for (account in result.data) {
                        // Skip if this is already a derived target
                        if (account.address in derivedAddresses) {
                            Log.d(TAG, "  [SKIP] ${account.address.take(12)}... (already derived)")
                            continue
                        }

                        // Skip zero-balance accounts (unlikely to be payment destination)
                        if (account.balance == 0L) {
                            Log.d(TAG, "  [SKIP] ${account.address.take(12)}... (zero balance)")
                            continue
                        }

                        val label = when (account.mint) {
                            SolanaTokenPolicy.USDC_MINT -> "USDC_DISCOVERED"
                            SolanaTokenPolicy.USDT_MINT -> "USDT_DISCOVERED"
                            else -> "DISCOVERED"
                        }

                        Log.d(TAG, "  [ADD] ${account.address.take(12)}... mint=${account.mint.take(12)}... balance=${account.balance}")

                        discoveredTargets.add(PollingTarget(
                            address = account.address,
                            tokenMint = account.mint,
                            label = label,
                            strategy = PollingStrategy.DISCOVERED_ACCOUNT
                        ))
                    }
                }
                is RpcCallResult.Failure -> {
                    Log.w(TAG, "  [ERROR] Failed to discover accounts for mint ${mint.take(12)}...: ${result.message}")
                }
            }
        }

        Log.d(TAG, "  Total discovered: ${discoveredTargets.size}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return discoveredTargets
    }

    override suspend fun listCandidates(
        target: PollingTarget,
        limit: Int
    ): CandidateListResult {
        val result = TransactionValidator.getSignaturesForAddressWithProvider(
            address = target.address,
            limit = limit
        )

        val candidates = result.signatures.map { (signature, blockTime) ->
            TransactionCandidate(
                transactionId = signature,
                blockTime = blockTime
            )
        }

        return CandidateListResult(
            candidates = candidates,
            providerHint = result.provider,
            providerName = result.providerName
        )
    }

    override suspend fun validateCandidate(params: ValidationParams): ValidationResult {
        // Convert to Solana-specific params
        val solanaParams = TransactionValidator.ValidationParams(
            signature = params.transactionId,
            expectedRecipient = params.expectedRecipient,
            expectedAmountMinor = params.expectedAmountMinor,
            expectedCurrency = if (params.expectedTokenMint != null) PaymentCurrency.USDC else PaymentCurrency.SOL,
            expectedMint = params.expectedTokenMint,
            invoiceNetworkId = params.invoiceNetworkId,
            invoiceCreatedAt = params.invoiceCreatedAt,
            preferredProvider = params.providerHint as? SolanaRpcProvider
        )

        // Convert result back to neutral type (including payer info)
        return when (val result = TransactionValidator.validateTransaction(solanaParams)) {
            is TransactionValidator.Result.Valid -> ValidationResult.Valid(
                actualTokenUsed = result.actualMintUsed,
                warningCode = result.warningCode,
                payerAddress = result.payerAddress,
                payerTokenAccount = result.payerTokenAccount
            )
            is TransactionValidator.Result.Invalid -> ValidationResult.Invalid(result.reason)
            is TransactionValidator.Result.Error -> ValidationResult.Error(result.message)
        }
    }

    override suspend fun checkConfirmation(transactionId: String, providerHint: Any?): Boolean {
        val provider = providerHint as? SolanaRpcProvider
        return TransactionValidator.checkConfirmation(transactionId, provider)
    }

    override fun startForegroundDetection(
        invoice: InvoiceEntity,
        timeoutMs: Long,
        onDetected: OnPaymentDetected
    ) {
        // Adapter from Solana-specific callback to neutral callback
        detector.startDetection(invoice, timeoutMs) { invoiceId, result ->
            val neutralResult = convertDetectionResult(result)
            onDetected.onDetected(invoiceId, neutralResult)
        }
    }

    override fun cancelForegroundDetection(invoiceId: String) {
        detector.cancelDetection(invoiceId)
    }

    override fun cancelAllForegroundDetections() {
        detector.cancelAll()
    }

    override fun isDetecting(invoiceId: String): Boolean {
        return detector.isDetecting(invoiceId)
    }

    /**
     * Convert Solana-specific detection result to neutral type.
     */
    private fun convertDetectionResult(result: PaymentDetectionResult): DetectionResult {
        return when (result) {
            is PaymentDetectionResult.Searching -> DetectionResult.Searching
            is PaymentDetectionResult.Found -> DetectionResult.Found(
                transactionId = result.signature,
                isConfirmed = result.isConfirmed,
                actualTokenUsed = result.actualMintUsed,
                warningCode = result.warningCode,
                payerAddress = result.payerAddress,
                payerTokenAccount = result.payerTokenAccount
            )
            is PaymentDetectionResult.Confirmed -> DetectionResult.Confirmed(
                transactionId = result.signature,
                actualTokenUsed = result.actualMintUsed,
                warningCode = result.warningCode,
                payerAddress = result.payerAddress,
                payerTokenAccount = result.payerTokenAccount
            )
            is PaymentDetectionResult.Invalid -> DetectionResult.Invalid(
                transactionId = result.signature,
                reason = result.reason
            )
            is PaymentDetectionResult.Expired -> DetectionResult.Expired
            is PaymentDetectionResult.Error -> DetectionResult.Error(result.message)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REFUND METHODS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** Lazy transaction builder instance */
    private val transactionBuilder by lazy { SolanaTransactionBuilder() }

    /**
     * Check if an invoice can be refunded.
     *
     * Requirements:
     * - Invoice must be CONFIRMED
     * - payerAddress must be present (we need to know where to send refund)
     * - No existing refund (refundTxId must be null)
     */
    override fun canRefund(invoice: InvoiceEntity): Boolean {
        val canRefund = invoice.status == InvoiceStatus.CONFIRMED &&
                invoice.payerAddress != null &&
                invoice.refundTxId == null

        Log.d(TAG, "REFUND|CAN_REFUND|invoice=${invoice.id}|status=${invoice.status}|" +
                "hasPayer=${invoice.payerAddress != null}|hasRefundTx=${invoice.refundTxId != null}|result=$canRefund")

        return canRefund
    }

    /**
     * Build an unsigned refund transaction.
     *
     * For Solana SPL tokens:
     * - Builds TransferChecked instruction from merchant ATA to payer ATA
     * - Uses actual mint from payment (actualMintUsed or expected mint)
     * - Includes memo with invoice ID
     *
     * @param invoice Invoice to refund
     * @return Unsigned transaction bytes, or error
     */
    override suspend fun buildRefundTransaction(invoice: InvoiceEntity): RefundTransactionResult {
        // Determine token mint upfront for logging
        val tokenMint = invoice.actualMintUsed ?: invoice.splTokenMint
        val amountUsd = invoice.amount.toDouble() / 1_000_000.0 // Convert minor to display

        Log.i(TAG, "REFUND|START|invoiceId=${invoice.id}|railId=$RAIL_ID|networkId=${invoice.networkId}|" +
                "amountUsd=$amountUsd|mint=${tokenMint?.take(12) ?: "null"}|payer=${invoice.payerAddress?.take(12) ?: "null"}")

        // Validate eligibility
        if (invoice.status != InvoiceStatus.CONFIRMED) {
            Log.w(TAG, "REFUND|FAIL|stage=ELIGIBILITY|invoiceId=${invoice.id}|reason=not_confirmed|status=${invoice.status}")
            return RefundTransactionResult.NotEligible("Invoice is not confirmed (status: ${invoice.status})")
        }

        val payerAddress = invoice.payerAddress
        if (payerAddress == null) {
            Log.w(TAG, "REFUND|FAIL|stage=ELIGIBILITY|invoiceId=${invoice.id}|reason=no_payer_address")
            return RefundTransactionResult.NotEligible("Payer address not available")
        }

        if (invoice.refundTxId != null) {
            Log.w(TAG, "REFUND|FAIL|stage=ELIGIBILITY|invoiceId=${invoice.id}|reason=already_refunded|existingTx=${invoice.refundTxId}")
            return RefundTransactionResult.NotEligible("Invoice already refunded")
        }

        if (tokenMint == null) {
            Log.w(TAG, "REFUND|FAIL|stage=BUILD|invoiceId=${invoice.id}|reason=sol_payment_not_supported")
            return RefundTransactionResult.NotEligible("SOL refunds not yet supported")
        }

        val refundAmount = invoice.amount
        val decimals = 6 // USDC/USDT decimals

        // Build the transaction
        val result = transactionBuilder.buildRefundTransaction(
            merchantPublicKey = invoice.recipientAddress,
            payerPublicKey = payerAddress,
            tokenMint = tokenMint,
            amount = refundAmount,
            decimals = decimals,
            invoiceId = invoice.id
        )

        return when (result) {
            is RpcResult.Success -> {
                Log.i(TAG, "REFUND|BUILD_OK|invoiceId=${invoice.id}|txBytes=${result.data.size}|mint=$tokenMint|payer=$payerAddress")
                RefundTransactionResult.Success(
                    transactionBytes = result.data,
                    refundAmountMinor = refundAmount,
                    refundMint = tokenMint,
                    recipientAddress = payerAddress
                )
            }
            is RpcResult.Error -> {
                Log.e(TAG, "REFUND|FAIL|stage=BUILD|invoiceId=${invoice.id}|reason=${result.message}")
                RefundTransactionResult.Error(result.message)
            }
        }
    }

    /**
     * Send a signed refund transaction.
     *
     * NOTE: For Solana with MWA, the wallet handles both signing AND sending
     * via signAndSendTransactions(). This method is for alternative flows
     * where the app receives signed bytes and needs to send them.
     *
     * For MWA flow, use WalletConnector.signAndSendTransaction() directly.
     */
    override suspend fun sendRefundTransaction(signedTransaction: ByteArray): RefundSendResult {
        Log.w(TAG, "REFUND|SEND|Using MWA flow - this method should not be called directly")
        // For Solana with MWA, wallet handles send
        // This would be used if we had a custom signing flow (e.g., hardware wallet)
        return RefundSendResult.Failure("Use WalletConnector.signAndSendTransaction() for MWA flow")
    }
}
