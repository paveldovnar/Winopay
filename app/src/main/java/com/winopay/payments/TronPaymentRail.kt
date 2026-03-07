package com.winopay.payments

import android.util.Log
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.tron.TronAddressUtils
import com.winopay.tron.TronConstants
import com.winopay.tron.TronTransactionBuilder
import com.winopay.tron.rpc.Trc20Transaction
import com.winopay.tron.rpc.TronProviderManager
import com.winopay.tron.rpc.TronRpcProvider
import com.winopay.tron.rpc.TronRpcResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * TRON implementation of PaymentRail.
 *
 * Detects TRC20 payments (USDT/USDC) on TRON network.
 *
 * KEY DIFFERENCES FROM SOLANA:
 * 1. No ATAs - token balances tracked by contract, poll wallet address directly
 * 2. Uses TronGrid API instead of JSON-RPC
 * 3. Transaction validation checks TRC20 Transfer events
 * 4. Block time ~3 seconds (vs ~400ms on Solana)
 *
 * DETECTION FLOW:
 * 1. Poll /v1/accounts/{address}/transactions/trc20 endpoint
 * 2. Filter by contract address (USDT/USDC) and min_timestamp
 * 3. Validate incoming transfers: recipient, amount, token
 * 4. Check confirmation status
 */
class TronPaymentRail(
    override val networkId: String = TronConstants.NETWORK_MAINNET
) : PaymentRail {

    companion object {
        private const val TAG = "TronPaymentRail"
        const val RAIL_ID = "tron"

        /** Poll interval for foreground detection (3 seconds - TRON block time) */
        private const val POLL_INTERVAL_MS = 3000L

        /** Number of transactions to fetch per poll (uses ProviderManager constants) */
        private const val TX_FETCH_LIMIT = 50 // TronProviderManager.PAGE_SIZE

        fun supportsNetwork(networkId: String): Boolean {
            return TronConstants.supportsNetwork(networkId)
        }
    }

    override val railId: String = RAIL_ID

    override val tokenPolicy: TokenPolicyProvider = TronTokenPolicy(networkId)

    override fun getSupportedTokens(): List<SupportedToken> {
        val tronPolicy = tokenPolicy as TronTokenPolicy

        // TRON supports only USDT (no USDC)
        val tokens = listOf(
            SupportedToken(
                tokenId = tronPolicy.usdtContract,
                symbol = "USDT",
                displayName = "USDT on TRON (TRC-20)",
                decimals = TronConstants.USDT_DECIMALS,
                isEnabled = true
            )
        )

        Log.d(TAG, "METHODS|LIST|railId=$railId|networkId=$networkId|tokens=[USDT]")

        return tokens
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()
    private val checkedTxIds = mutableMapOf<String, MutableSet<String>>()

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PAYMENT REQUEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Build TRON payment request URL.
     *
     * Format: tron:{address}?amount={amount}&token={contract}
     * Note: This is a simplified format. Real TronLink deep links may vary.
     */
    override fun buildPaymentRequest(invoice: InvoiceEntity): String {
        val address = invoice.recipientAddress
        val amount = invoice.amount
        val tokenContract = invoice.splTokenMint // Using same field for token contract

        // Convert amount from minor units to display units
        val displayAmount = amount.toDouble() / 1_000_000.0

        return if (tokenContract != null) {
            // TRC20 payment
            "tron:$address?amount=$displayAmount&token=$tokenContract"
        } else {
            // TRX payment
            "tron:$address?amount=$displayAmount"
        }
    }

    override fun generateReference(): String {
        // TRON doesn't use references like Solana
        // Generate a UUID for tracking purposes
        return UUID.randomUUID().toString().replace("-", "").take(32)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ADDRESS DERIVATION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * TRON doesn't use ATAs - return wallet address directly.
     */
    override fun deriveTokenAddress(ownerWallet: String, tokenMint: String): String {
        // On TRON, TRC20 balances are tracked by the token contract
        // No separate token account needed - use wallet address directly
        return ownerWallet
    }

    override fun derivePollingTargets(invoice: InvoiceEntity): List<PollingTarget> {
        val expectedContract = invoice.splTokenMint

        // For TRX (native), poll wallet directly
        if (expectedContract.isNullOrBlank()) {
            return listOf(PollingTarget(
                address = invoice.recipientAddress,
                tokenMint = null,
                label = "wallet",
                strategy = PollingStrategy.DERIVED_ATA // Using same enum for consistency
            ))
        }

        // For TRC20, get all allowed tokens from policy
        val allowedContracts = tokenPolicy.getAllowedTokens(expectedContract)

        return allowedContracts.map { contract ->
            val label = when (contract) {
                TronConstants.getUsdtContract(networkId) -> "USDT"
                TronConstants.getUsdcContract(networkId) -> "USDC"
                else -> "TRC20"
            }
            PollingTarget(
                address = invoice.recipientAddress,
                tokenMint = contract,
                label = label,
                strategy = PollingStrategy.DERIVED_ATA
            )
        }
    }

    /**
     * TRON doesn't need fallback discovery - tokens tracked by contract.
     */
    override suspend fun discoverTokenAccountTargets(invoice: InvoiceEntity): List<PollingTarget> {
        // No fallback needed for TRON - TRC20 balances are contract-based
        return emptyList()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CANDIDATE LISTING & VALIDATION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    override suspend fun listCandidates(
        target: PollingTarget,
        limit: Int
    ): CandidateListResult {
        val result = TronProviderManager.getTrc20Transactions(
            address = target.address,
            contractAddress = target.tokenMint,
            networkId = networkId,
            limit = limit
        )

        return when (result) {
            is TronRpcResult.Success -> {
                val candidates = result.data.transactions.map { tx ->
                    TransactionCandidate(
                        transactionId = tx.txId,
                        blockTime = tx.blockTimestamp
                    )
                }
                CandidateListResult(
                    candidates = candidates,
                    providerHint = TronProviderManager.findProviderByName(result.data.providerName, networkId),
                    providerName = result.data.providerName
                )
            }
            is TronRpcResult.Failure -> {
                Log.e(TAG, "TRON|LIST_CANDIDATES|FAIL|${result.errorType}|${result.message}")
                CandidateListResult(
                    candidates = emptyList(),
                    providerName = result.providerName
                )
            }
        }
    }

    override suspend fun validateCandidate(params: ValidationParams): ValidationResult {
        Log.d(TAG, "TRON|VALIDATE|txId=${params.transactionId.take(12)}...|recipient=${params.expectedRecipient.take(12)}...")

        // Use paginated fetch to find the transaction - searches until:
        // 1. Found the txId we're looking for
        // 2. Reached transactions older than invoice creation
        // 3. Hit page limit
        val result = TronProviderManager.getTrc20TransactionsPaginated(
            address = params.expectedRecipient,
            contractAddress = params.expectedTokenMint,
            networkId = params.invoiceNetworkId,
            createdAt = params.invoiceCreatedAt,
            candidateMatcher = { tx -> tx.txId == params.transactionId }
        )

        when (result) {
            is TronRpcResult.Failure -> {
                return ValidationResult.Error("Failed to fetch transaction: ${result.message}")
            }
            is TronRpcResult.Success -> {
                // Find the transaction in results
                val tx = result.data.find { it.txId == params.transactionId }
                    ?: return ValidationResult.Invalid("Transaction not found in TRC20 history (searched ${result.data.size} txs)")

                return validateTrc20Transaction(tx, params)
            }
        }
    }

    /**
     * Validate a TRC20 transaction against invoice requirements.
     */
    private fun validateTrc20Transaction(
        tx: Trc20Transaction,
        params: ValidationParams
    ): ValidationResult {
        // 1. Check recipient
        if (tx.to != params.expectedRecipient) {
            Log.d(TAG, "TRON|VALIDATE|REJECT|recipient_mismatch|expected=${params.expectedRecipient.take(12)}|got=${tx.to.take(12)}")
            return ValidationResult.Invalid("Recipient mismatch")
        }

        // 2. Check block time (must be after invoice creation)
        if (tx.blockTimestamp < params.invoiceCreatedAt) {
            Log.d(TAG, "TRON|VALIDATE|REJECT|too_old|blockTime=${tx.blockTimestamp}|invoiceCreated=${params.invoiceCreatedAt}")
            return ValidationResult.Invalid("Transaction too old (before invoice creation)")
        }

        // 3. Check token contract
        val allowedContracts = tokenPolicy.getAllowedTokens(params.expectedTokenMint)
        if (tx.contractAddress !in allowedContracts) {
            Log.d(TAG, "TRON|VALIDATE|REJECT|wrong_token|expected=$allowedContracts|got=${tx.contractAddress}")
            return ValidationResult.Invalid("Wrong token contract")
        }

        // 4. Check amount (parse from string, compare in minor units)
        val receivedAmount = tx.value.toLongOrNull() ?: 0L
        if (receivedAmount < params.expectedAmountMinor) {
            val received = receivedAmount.toDouble() / 1_000_000.0
            val expected = params.expectedAmountMinor.toDouble() / 1_000_000.0
            Log.d(TAG, "TRON|VALIDATE|REJECT|amount_low|expected=$expected|received=$received")
            return ValidationResult.Invalid("Amount too low: received $received, expected $expected")
        }

        // 5. Get warning code if paid with different stablecoin
        val warningCode = tokenPolicy.getWarningCode(params.expectedTokenMint, tx.contractAddress)

        Log.i(TAG, "TRON|VALIDATE|VALID|txId=${tx.txId.take(16)}|amount=${receivedAmount}|from=${tx.from.take(12)}...")

        return ValidationResult.Valid(
            actualTokenUsed = tx.contractAddress,
            warningCode = warningCode,
            payerAddress = tx.from,
            payerTokenAccount = null // TRON doesn't have separate token accounts
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CONFIRMATION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    override suspend fun checkConfirmation(transactionId: String, providerHint: Any?): Boolean {
        val provider = providerHint as? TronRpcProvider

        val result = TronProviderManager.getTransactionInfo(
            txId = transactionId,
            networkId = networkId,
            preferredProvider = provider
        )

        return when (result) {
            is TronRpcResult.Success -> {
                val info = result.data
                if (info == null) {
                    Log.d(TAG, "TRON|CONFIRM|NOT_FOUND|txId=${transactionId.take(12)}...")
                    false
                } else {
                    // TRON confirmation: blockNumber > 0 and result is SUCCESS
                    val confirmed = info.blockNumber > 0 && info.result == "SUCCESS"
                    Log.d(TAG, "TRON|CONFIRM|txId=${transactionId.take(12)}...|block=${info.blockNumber}|result=${info.result}|confirmed=$confirmed")
                    confirmed
                }
            }
            is TronRpcResult.Failure -> {
                Log.e(TAG, "TRON|CONFIRM|ERROR|txId=${transactionId.take(12)}...|${result.message}")
                false
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FOREGROUND DETECTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    override fun startForegroundDetection(
        invoice: InvoiceEntity,
        timeoutMs: Long,
        onDetected: OnPaymentDetected
    ) {
        cancelForegroundDetection(invoice.id)

        checkedTxIds[invoice.id] = mutableSetOf()
        val pollingTargets = derivePollingTargets(invoice)

        Log.i(TAG, "TRON|START|FOREGROUND|${invoice.id.take(8)}|network=$networkId|targets=${pollingTargets.size}")

        val job = scope.launch {
            try {
                val result = detectPayment(invoice, pollingTargets, timeoutMs)
                onDetected.onDetected(invoice.id, result)
            } catch (e: CancellationException) {
                Log.d(TAG, "TRON|CANCEL|${invoice.id.take(8)}")
            } catch (e: Exception) {
                Log.e(TAG, "TRON|ERROR|${invoice.id.take(8)}|${e.message}")
                onDetected.onDetected(invoice.id, DetectionResult.Error(e.message ?: "Unknown error"))
            } finally {
                checkedTxIds.remove(invoice.id)
            }
        }

        activeJobs[invoice.id] = job
    }

    private suspend fun detectPayment(
        invoice: InvoiceEntity,
        pollingTargets: List<PollingTarget>,
        timeoutMs: Long
    ): DetectionResult {
        val startTime = System.currentTimeMillis()
        var pollCount = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            pollCount++

            for (target in pollingTargets) {
                val result = pollTarget(invoice, target, pollCount)
                if (result != null) {
                    return result
                }
            }

            delay(POLL_INTERVAL_MS)
        }

        Log.w(TAG, "TRON|TIMEOUT|${invoice.id.take(8)}|polls=$pollCount")
        return DetectionResult.Expired
    }

    private suspend fun pollTarget(
        invoice: InvoiceEntity,
        target: PollingTarget,
        pollCount: Int
    ): DetectionResult? {
        // Use paginated fetch for foreground detection - ensures we don't miss payments
        // even if merchant has high transaction volume
        val result = TronProviderManager.getTrc20TransactionsPaginated(
            address = target.address,
            contractAddress = target.tokenMint,
            networkId = invoice.networkId,
            createdAt = invoice.createdAt,
            candidateMatcher = null // Check all transactions
        )

        when (result) {
            is TronRpcResult.Failure -> {
                Log.w(TAG, "TRON|POLL|FAIL|${invoice.id.take(8)}|poll=$pollCount|${result.message}")
                return null
            }
            is TronRpcResult.Success -> {
                val transactions = result.data
                if (transactions.isEmpty()) return null

                Log.d(TAG, "TRON|SIGS|${invoice.id.take(8)}|poll=$pollCount|${target.label}|count=${transactions.size}")

                val checked = checkedTxIds[invoice.id] ?: return null

                for (tx in transactions) {
                    if (tx.txId in checked) continue
                    checked.add(tx.txId)

                    val validationParams = ValidationParams(
                        transactionId = tx.txId,
                        expectedRecipient = invoice.recipientAddress,
                        expectedAmountMinor = invoice.amount,
                        expectedTokenMint = invoice.splTokenMint,
                        invoiceNetworkId = invoice.networkId,
                        invoiceCreatedAt = invoice.createdAt,
                        providerHint = TronProviderManager.findProviderByName(result.providerName, networkId)
                    )

                    // Quick validation using the already-fetched tx data
                    val validationResult = validateTrc20Transaction(tx, validationParams)

                    when (validationResult) {
                        is ValidationResult.Valid -> {
                            Log.i(TAG, "TRON|✅ FOUND|${invoice.id.take(8)}|txId=${tx.txId.take(16)}|${target.label}|poll=$pollCount")

                            // Check confirmation
                            val isConfirmed = checkConfirmation(tx.txId, null)

                            return if (isConfirmed) {
                                DetectionResult.Confirmed(
                                    transactionId = tx.txId,
                                    actualTokenUsed = validationResult.actualTokenUsed,
                                    warningCode = validationResult.warningCode,
                                    payerAddress = validationResult.payerAddress,
                                    payerTokenAccount = validationResult.payerTokenAccount
                                )
                            } else {
                                DetectionResult.Found(
                                    transactionId = tx.txId,
                                    isConfirmed = false,
                                    actualTokenUsed = validationResult.actualTokenUsed,
                                    warningCode = validationResult.warningCode,
                                    payerAddress = validationResult.payerAddress,
                                    payerTokenAccount = validationResult.payerTokenAccount
                                )
                            }
                        }
                        is ValidationResult.Invalid -> {
                            Log.d(TAG, "TRON|REJECT|${invoice.id.take(8)}|txId=${tx.txId.take(12)}|${validationResult.reason}")
                        }
                        is ValidationResult.Error -> {
                            Log.e(TAG, "TRON|ERROR|${invoice.id.take(8)}|txId=${tx.txId.take(12)}|${validationResult.message}")
                        }
                    }
                }
            }
        }

        return null
    }

    override fun cancelForegroundDetection(invoiceId: String) {
        activeJobs[invoiceId]?.cancel()
        activeJobs.remove(invoiceId)
        checkedTxIds.remove(invoiceId)
    }

    override fun cancelAllForegroundDetections() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        checkedTxIds.clear()
    }

    override fun isDetecting(invoiceId: String): Boolean {
        return activeJobs[invoiceId]?.isActive == true
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REFUND METHODS (via WalletConnect v2)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    override fun canRefund(invoice: InvoiceEntity): Boolean {
        // TRON refunds require wallet signing via WalletConnect v2.
        // Currently disabled until WC v2 integration is implemented.
        // Supported wallets: Trust Wallet, SafePal, TokenPocket, Binance Web3, BitKeep, OKX
        // TronLink does NOT support WalletConnect v2.
        val signingAvailable = false // TODO: TronWalletConnector.isSigningAvailable()

        val eligible = invoice.status == InvoiceStatus.CONFIRMED &&
                invoice.payerAddress != null &&
                invoice.refundTxId == null &&
                invoice.railId == RAIL_ID

        val canRefund = signingAvailable && eligible

        Log.d(TAG, "TRON|REFUND|CAN_REFUND|invoice=${invoice.id.take(8)}|result=$canRefund|" +
                "signingAvailable=$signingAvailable|eligible=$eligible|" +
                "status=${invoice.status}|hasPayer=${invoice.payerAddress != null}|hasRefund=${invoice.refundTxId != null}")
        return canRefund
    }

    override suspend fun buildRefundTransaction(invoice: InvoiceEntity): RefundTransactionResult {
        Log.i(TAG, "REFUND|START|railId=$RAIL_ID|networkId=$networkId|invoice=${invoice.id.take(8)}")

        // Validate eligibility
        if (!canRefund(invoice)) {
            val reason = when {
                invoice.status != InvoiceStatus.CONFIRMED -> "Invoice not confirmed (status=${invoice.status})"
                invoice.payerAddress == null -> "Payer address not available"
                invoice.refundTxId != null -> "Already refunded (txId=${invoice.refundTxId})"
                invoice.railId != RAIL_ID -> "Wrong rail (railId=${invoice.railId})"
                else -> "Unknown eligibility issue"
            }
            Log.w(TAG, "REFUND|NOT_ELIGIBLE|invoice=${invoice.id.take(8)}|reason=$reason")
            return RefundTransactionResult.NotEligible(reason)
        }

        val payerAddress = invoice.payerAddress!!
        val recipientAddress = invoice.recipientAddress
        val amount = invoice.amount

        // Get USDT contract for this network
        val usdtContract = TronConstants.getUsdtContract(networkId)

        Log.d(TAG, "REFUND|BUILD|owner=${recipientAddress.take(12)}...|to=${payerAddress.take(12)}...|amount=$amount|contract=${usdtContract.take(12)}...")

        // Build TRC20 transfer transaction
        val buildResult = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = recipientAddress,
            recipientAddress = payerAddress,
            tokenContract = usdtContract,
            amountMinor = amount,
            feeLimit = TronTransactionBuilder.DEFAULT_FEE_LIMIT_SUN
        )

        when (buildResult) {
            is TronTransactionBuilder.BuildResult.Error -> {
                Log.e(TAG, "REFUND|BUILD_FAIL|invoice=${invoice.id.take(8)}|error=${buildResult.message}")
                return RefundTransactionResult.Error(buildResult.message)
            }
            is TronTransactionBuilder.BuildResult.Success -> {
                // Create unsigned transaction via provider
                val createResult = TronProviderManager.createTrc20Transaction(
                    ownerAddressHex = buildResult.ownerAddressHex,
                    contractAddressHex = buildResult.contractAddressHex,
                    functionSelector = buildResult.functionSelector,
                    parameter = buildResult.parameter,
                    feeLimit = buildResult.feeLimit,
                    networkId = networkId
                )

                when (createResult) {
                    is TronRpcResult.Failure -> {
                        Log.e(TAG, "REFUND|CREATE_FAIL|invoice=${invoice.id.take(8)}|provider=${createResult.providerName}|error=${createResult.message}")
                        return RefundTransactionResult.Error("Failed to create transaction: ${createResult.message}")
                    }
                    is TronRpcResult.Success -> {
                        val unsignedTx = createResult.data
                        Log.i(TAG, "REFUND|BUILD_OK|railId=$RAIL_ID|networkId=$networkId|provider=${createResult.providerName}|txId=${unsignedTx.txId.take(16)}...")

                        // Return transaction JSON as bytes for signing
                        return RefundTransactionResult.Success(
                            transactionBytes = unsignedTx.transactionJson.toByteArray(Charsets.UTF_8),
                            refundAmountMinor = amount,
                            refundMint = usdtContract,
                            recipientAddress = payerAddress
                        )
                    }
                }
            }
        }
    }

    override suspend fun sendRefundTransaction(signedTransaction: ByteArray): RefundSendResult {
        val signedTxJson = String(signedTransaction, Charsets.UTF_8)
        Log.i(TAG, "REFUND|SEND_START|railId=$RAIL_ID|networkId=$networkId|txLen=${signedTransaction.size}")

        val result = TronProviderManager.broadcastTransaction(
            signedTxHex = signedTxJson,
            networkId = networkId
        )

        return when (result) {
            is TronRpcResult.Failure -> {
                Log.e(TAG, "REFUND|SEND_FAIL|railId=$RAIL_ID|networkId=$networkId|provider=${result.providerName}|error=${result.message}")
                RefundSendResult.Failure(result.message)
            }
            is TronRpcResult.Success -> {
                val txId = result.data
                Log.i(TAG, "REFUND|SEND_OK|railId=$RAIL_ID|networkId=$networkId|provider=${result.providerName}|txId=$txId")
                RefundSendResult.Success(txId)
            }
        }
    }
}
