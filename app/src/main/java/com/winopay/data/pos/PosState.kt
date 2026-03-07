package com.winopay.data.pos

/**
 * Payment method for POS transactions.
 *
 * MULTICHAIN SUPPORT:
 * - railId: Payment rail (solana, tron, evm)
 * - tokenId: Token contract/mint address
 * - symbol: Token symbol (USDC, USDT)
 * - displayName: Human-readable name for UI
 *
 * The id format is "{railId}:{tokenId}" for unique identification.
 */
data class PaymentMethod(
    val railId: String,
    val tokenId: String,
    val symbol: String,
    val displayName: String,
    val networkDisplayName: String  // "on Solana", "on TRON"
) {
    /**
     * Unique method ID for storage and comparison.
     * Format: "{railId}:{tokenId}"
     */
    val id: String get() = "$railId:$tokenId"

    companion object {
        /**
         * Create PaymentMethod from method ID string.
         * Format: "{railId}:{tokenId}"
         */
        fun fromId(id: String, symbol: String = "USDC", displayName: String = "USDC"): PaymentMethod {
            val parts = id.split(":", limit = 2)
            val railId = parts.getOrNull(0) ?: "solana"
            val tokenId = parts.getOrNull(1) ?: ""
            val networkDisplayName = when (railId) {
                "solana" -> "on Solana"
                "tron" -> "on TRON"
                else -> "on $railId"
            }
            return PaymentMethod(
                railId = railId,
                tokenId = tokenId,
                symbol = symbol,
                displayName = displayName,
                networkDisplayName = networkDisplayName
            )
        }

        /**
         * Map PaymentCurrency to PaymentMethod.
         * Used when restoring invoices from Room.
         *
         * NOTE: For legacy support - new code should use fromId().
         */
        fun fromCurrency(currency: com.winopay.data.local.PaymentCurrency, railId: String, tokenId: String): PaymentMethod {
            val symbol = when (currency) {
                com.winopay.data.local.PaymentCurrency.USDC -> "USDC"
                com.winopay.data.local.PaymentCurrency.USDT -> "USDT"
                com.winopay.data.local.PaymentCurrency.SOL -> "SOL"
            }
            val networkDisplayName = when (railId) {
                "solana" -> "on Solana"
                "tron" -> "on TRON"
                else -> "on $railId"
            }
            return PaymentMethod(
                railId = railId,
                tokenId = tokenId,
                symbol = symbol,
                displayName = "$symbol $networkDisplayName",
                networkDisplayName = networkDisplayName
            )
        }

        // Legacy support: Predefined methods for backward compatibility
        val USDC = PaymentMethod(
            railId = "solana",
            tokenId = com.winopay.BuildConfig.USDC_MINT,
            symbol = "USDC",
            displayName = "USDC on Solana",
            networkDisplayName = "on Solana"
        )

        val USDT = PaymentMethod(
            railId = "solana",
            tokenId = com.winopay.BuildConfig.USDT_MINT,
            symbol = "USDT",
            displayName = "USDT on Solana",
            networkDisplayName = "on Solana"
        )
    }
}

/**
 * Sealed class representing all possible POS flow states.
 */
sealed class PosState {

    /**
     * Initial state: User enters payment amount.
     */
    data object EnterAmount : PosState()

    /**
     * User selects payment method after entering amount.
     */
    data class SelectPayment(
        val amount: Double,
        val invoiceId: String
    ) : PosState()

    /**
     * QR code displayed, waiting for payment.
     */
    data class Qr(
        val invoiceId: String,
        val amount: Double,          // Display amount in local currency
        val usdcAmount: Double,      // Actual USDC amount to be paid
        val method: PaymentMethod,
        val qrData: String,
        val expiresAt: Long
    ) : PosState()

    /**
     * Payment detected, awaiting confirmation.
     */
    data class Pending(
        val invoiceId: String,
        val amount: Double,
        val signature: String?
    ) : PosState()

    /**
     * Payment confirmed successfully.
     */
    data class Success(
        val invoiceId: String,
        val amount: Double,
        val signature: String
    ) : PosState()

    /**
     * Payment failed.
     */
    data class Failed(
        val invoiceId: String,
        val amount: Double,
        val reason: String?
    ) : PosState()

    /**
     * Invoice timeout reached - awaiting merchant decision (soft-expire).
     *
     * SOFT-EXPIRE BEHAVIOR:
     * - Detection continues in background
     * - Merchant can:
     *   - "Wait 5 more minutes" -> extends detection, returns to QR
     *   - "Revoke invoice" -> marks as EXPIRED, stops detection
     * - Payment arriving during soft-expire is still detected
     */
    data class ExpiredAwaitingDecision(
        val invoiceId: String,
        val amount: Double,
        val elapsedSeconds: Int,
        val qrData: String
    ) : PosState()

    /**
     * Invoice expired (merchant chose to revoke, or detection timed out after extension).
     */
    data class Expired(
        val invoiceId: String,
        val amount: Double
    ) : PosState()

    /**
     * Exchange rate error - invoice creation blocked.
     * CRITICAL: Shown when live FX rates are unavailable.
     */
    data class RateError(
        val message: String
    ) : PosState()

    /**
     * Blocked by active invoice - new invoice creation not allowed.
     *
     * STRICT BLOCKING MODEL:
     * When an active invoice (CREATED or PENDING) exists, new invoice
     * creation is BLOCKED. Merchant must explicitly:
     * - Resume the active payment, OR
     * - Cancel the active payment
     *
     * This prevents accidental loss of in-progress payment state.
     */
    data class BlockedByActiveInvoice(
        val activeInvoiceId: String,
        val activeAmount: Double,
        val activeStatus: String  // "CREATED" or "PENDING"
    ) : PosState()
}
