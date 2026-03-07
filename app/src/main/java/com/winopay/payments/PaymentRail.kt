package com.winopay.payments

import com.winopay.data.local.InvoiceEntity

/**
 * Abstraction for blockchain payment networks.
 *
 * This interface allows the business layer (PosManager, Worker) to work with
 * any blockchain network without direct dependencies on specific implementations.
 *
 * CURRENTLY SUPPORTED:
 * - Solana (via SolanaPaymentRail)
 * - TRON (via TronPaymentRail)
 *
 * FUTURE NETWORKS (interface-ready, not implemented):
 * - ERC-20 (Ethereum, Polygon, etc.)
 * - BEP-20 (BSC)
 */
interface PaymentRail {

    /**
     * Unique identifier for this rail (e.g., "solana", "ethereum", "bsc").
     */
    val railId: String

    /**
     * Network identifier (e.g., "devnet", "tron-nile", "mainnet-beta").
     */
    val networkId: String

    /**
     * Token acceptance policy for this rail.
     * Defines which tokens are accepted and dual acceptance rules.
     */
    val tokenPolicy: TokenPolicyProvider

    /**
     * Get supported payment tokens for this rail.
     *
     * Returns tokens that can be used for payment on this rail,
     * including their display info and enabled status.
     *
     * @return List of supported payment tokens
     */
    fun getSupportedTokens(): List<SupportedToken>

    /**
     * Build a payment request URL/QR code data for the given invoice.
     *
     * @param invoice The invoice to generate payment request for
     * @return Payment request string (URL, deep link, or QR data)
     */
    fun buildPaymentRequest(invoice: InvoiceEntity): String

    /**
     * Generate a unique reference for payment detection.
     * For Solana: base58 public key
     * For EVM: could be a nonce or hash
     *
     * @return Unique reference string
     */
    fun generateReference(): String

    /**
     * Derive the token receiving address for a given wallet and token.
     *
     * CHAIN-SPECIFIC BEHAVIOR:
     * - Solana: Returns ATA (Associated Token Account) for SPL tokens
     * - EVM: Returns the wallet address itself (ERC-20 balances tracked by contract)
     * - Tron: Returns wallet address (TRC-20 balances tracked by contract)
     *
     * @param ownerWallet Owner wallet address
     * @param tokenMint Token contract/mint address
     * @return Address to use for token payment detection
     */
    fun deriveTokenAddress(ownerWallet: String, tokenMint: String): String

    /**
     * Get polling targets for payment detection (Strategy A: derived ATAs).
     *
     * For Solana:
     * - SOL payments: wallet address
     * - SPL payments: ATA(s) - may include multiple for dual stablecoin acceptance
     *
     * @param invoice The invoice to detect payment for
     * @return List of addresses/accounts to poll
     */
    fun derivePollingTargets(invoice: InvoiceEntity): List<PollingTarget>

    /**
     * Discover actual token accounts for fallback detection (Strategy B).
     *
     * Uses RPC to find ALL token accounts owned by the recipient,
     * filtered by the allowed mints for this invoice.
     *
     * WHEN TO USE:
     * - After Strategy A (derived ATAs) fails to find payment
     * - Catches non-standard token accounts, accounts created by other programs
     *
     * @param invoice The invoice to detect payment for
     * @return List of discovered token account targets
     */
    suspend fun discoverTokenAccountTargets(invoice: InvoiceEntity): List<PollingTarget>

    /**
     * List candidate transactions for validation.
     * Fetches recent transactions from the network for the given target.
     *
     * @param target Polling target (address to check)
     * @param limit Max number of candidates to return
     * @return Result containing candidates and provider info
     */
    suspend fun listCandidates(
        target: PollingTarget,
        limit: Int = 10
    ): CandidateListResult

    /**
     * Validate a candidate transaction against invoice requirements.
     *
     * @param params Validation parameters
     * @return Validation result (Valid, Invalid, or Error)
     */
    suspend fun validateCandidate(params: ValidationParams): ValidationResult

    /**
     * Check if a transaction is confirmed on-chain.
     *
     * @param transactionId Transaction signature/hash
     * @param providerHint Optional hint for which provider to use
     * @return true if confirmed, false otherwise
     */
    suspend fun checkConfirmation(transactionId: String, providerHint: Any? = null): Boolean

    /**
     * Start foreground payment detection.
     *
     * @param invoice Invoice to detect payment for
     * @param timeoutMs Detection timeout in milliseconds
     * @param onDetected Callback when payment is detected
     */
    fun startForegroundDetection(
        invoice: InvoiceEntity,
        timeoutMs: Long,
        onDetected: OnPaymentDetected
    )

    /**
     * Cancel foreground detection for an invoice.
     *
     * @param invoiceId Invoice identifier
     */
    fun cancelForegroundDetection(invoiceId: String)

    /**
     * Cancel all active foreground detections.
     */
    fun cancelAllForegroundDetections()

    /**
     * Check if detection is active for an invoice.
     */
    fun isDetecting(invoiceId: String): Boolean

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REFUND METHODS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Check if an invoice can be refunded.
     *
     * Requirements vary by chain:
     * - Solana: CONFIRMED status, payerAddress present, no existing refund
     * - EVM: Similar, may require additional gas balance check
     *
     * @param invoice Invoice to check
     * @return true if refund is possible
     */
    fun canRefund(invoice: InvoiceEntity): Boolean

    /**
     * Build an unsigned refund transaction.
     *
     * CHAIN-SPECIFIC BEHAVIOR:
     * - Solana: Builds SPL transfer (or SOL transfer) instruction to payer
     * - EVM: Builds ERC-20 transfer (or ETH transfer) to payer
     *
     * The returned transaction must be signed via MWA/WalletConnect before sending.
     *
     * @param invoice Invoice to refund (must have payerAddress set)
     * @return Serialized unsigned transaction bytes, or error
     */
    suspend fun buildRefundTransaction(invoice: InvoiceEntity): RefundTransactionResult

    /**
     * Send a signed refund transaction to the network.
     *
     * @param signedTransaction Signed transaction bytes
     * @return Transaction signature/hash, or error
     */
    suspend fun sendRefundTransaction(signedTransaction: ByteArray): RefundSendResult
}

/**
 * Polling strategy for payment detection.
 *
 * MULTI-STRATEGY DETECTION:
 * Different strategies have different trade-offs between speed and coverage.
 * Detector should try strategies in order: DERIVED_ATA → DISCOVERED_ACCOUNTS → WALLET_SCAN
 */
enum class PollingStrategy {
    /**
     * Poll derived ATA (Associated Token Account).
     * FAST: Uses deterministic PDA derivation.
     * LIMITATION: Only works if recipient uses standard ATA.
     */
    DERIVED_ATA,

    /**
     * Poll discovered token accounts via RPC.
     * FALLBACK: Uses getTokenAccountsByOwner to find ALL token accounts.
     * COVERAGE: Finds non-standard accounts, accounts created by other programs.
     */
    DISCOVERED_ACCOUNT,

    /**
     * Scan wallet signatures for SPL transfers.
     * LAST RESORT: Polls owner wallet and checks all recent transactions.
     * EXPENSIVE: More RPC calls, slower, but catches edge cases.
     */
    WALLET_SCAN
}

/**
 * Target address for payment polling.
 *
 * @property address The address to poll (wallet for native, token account for tokens)
 * @property tokenMint Token contract/mint address (null for native currency)
 * @property label Human-readable label for logging
 * @property strategy Which detection strategy produced this target
 */
data class PollingTarget(
    val address: String,
    val tokenMint: String?,
    val label: String,
    val strategy: PollingStrategy = PollingStrategy.DERIVED_ATA
)

/**
 * Result of listing candidate transactions.
 */
data class CandidateListResult(
    val candidates: List<TransactionCandidate>,
    val providerHint: Any? = null,
    val providerName: String = "unknown"
)

/**
 * A candidate transaction for validation.
 */
data class TransactionCandidate(
    val transactionId: String,
    val blockTime: Long
)

/**
 * Parameters for transaction validation.
 */
data class ValidationParams(
    val transactionId: String,
    val expectedRecipient: String,
    val expectedAmountMinor: Long,
    val expectedTokenMint: String?,
    val invoiceNetworkId: String,
    val invoiceCreatedAt: Long,
    val providerHint: Any? = null
)

/**
 * Result of transaction validation.
 */
sealed class ValidationResult {
    /**
     * Transaction is valid and matches invoice requirements.
     *
     * @property actualTokenUsed Actual token mint used (may differ for interchangeable stablecoins)
     * @property warningCode Warning code if payment used different but acceptable token
     * @property payerAddress Wallet address of payer (owner for SPL, sender for native)
     * @property payerTokenAccount Token account that sent payment (source ATA for SPL)
     */
    data class Valid(
        val actualTokenUsed: String? = null,
        val warningCode: String? = null,
        val payerAddress: String? = null,
        val payerTokenAccount: String? = null
    ) : ValidationResult()

    /**
     * Transaction is invalid (wrong amount, recipient, etc.).
     */
    data class Invalid(val reason: String) : ValidationResult()

    /**
     * Error occurred during validation.
     */
    data class Error(val message: String) : ValidationResult()

    fun isValid() = this is Valid
}

/**
 * Result of payment detection.
 */
sealed class DetectionResult {
    data object Searching : DetectionResult()

    data class Found(
        val transactionId: String,
        val isConfirmed: Boolean,
        val actualTokenUsed: String? = null,
        val warningCode: String? = null,
        val payerAddress: String? = null,
        val payerTokenAccount: String? = null
    ) : DetectionResult()

    data class Confirmed(
        val transactionId: String,
        val actualTokenUsed: String? = null,
        val warningCode: String? = null,
        val payerAddress: String? = null,
        val payerTokenAccount: String? = null
    ) : DetectionResult()

    data class Invalid(val transactionId: String, val reason: String) : DetectionResult()

    data object Expired : DetectionResult()

    data class Error(val message: String) : DetectionResult()
}

/**
 * Callback for payment detection.
 */
fun interface OnPaymentDetected {
    suspend fun onDetected(invoiceId: String, result: DetectionResult)
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// REFUND TYPES
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Result of building a refund transaction.
 */
sealed class RefundTransactionResult {
    /**
     * Successfully built unsigned transaction.
     *
     * @property transactionBytes Serialized unsigned transaction (ready for signing)
     * @property refundAmountMinor Amount being refunded in minor units
     * @property refundMint Token mint being refunded (null for native currency)
     * @property recipientAddress Payer address receiving the refund
     */
    data class Success(
        val transactionBytes: ByteArray,
        val refundAmountMinor: Long,
        val refundMint: String?,
        val recipientAddress: String
    ) : RefundTransactionResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return transactionBytes.contentEquals(other.transactionBytes) &&
                    refundAmountMinor == other.refundAmountMinor &&
                    refundMint == other.refundMint &&
                    recipientAddress == other.recipientAddress
        }
        override fun hashCode(): Int {
            var result = transactionBytes.contentHashCode()
            result = 31 * result + refundAmountMinor.hashCode()
            result = 31 * result + (refundMint?.hashCode() ?: 0)
            result = 31 * result + recipientAddress.hashCode()
            return result
        }
    }

    /**
     * Cannot build refund - invoice not eligible.
     */
    data class NotEligible(val reason: String) : RefundTransactionResult()

    /**
     * Error building transaction.
     */
    data class Error(val message: String) : RefundTransactionResult()
}

/**
 * Result of sending a refund transaction.
 */
sealed class RefundSendResult {
    /**
     * Transaction sent successfully.
     *
     * @property transactionId Transaction signature/hash
     */
    data class Success(val transactionId: String) : RefundSendResult()

    /**
     * Transaction failed to send.
     */
    data class Failure(val message: String) : RefundSendResult()
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// SUPPORTED TOKEN INFO
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/**
 * Represents a payment token supported by a rail.
 *
 * @property tokenId Contract/mint address (unique identifier)
 * @property symbol Display symbol (USDC, USDT, etc.)
 * @property displayName Full display name (e.g., "USDT on TRON (TRC-20)")
 * @property decimals Token decimals (usually 6 for stablecoins)
 * @property isEnabled Whether this token is enabled for payments
 * @property disabledReason Reason if disabled (e.g., "Coming soon")
 */
data class SupportedToken(
    val tokenId: String,
    val symbol: String,
    val displayName: String,
    val decimals: Int = 6,
    val isEnabled: Boolean = true,
    val disabledReason: String? = null
)
