package com.winopay.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.winopay.BuildConfig

/**
 * Invoice status enum for Solana Pay POS flow.
 */
enum class InvoiceStatus {
    CREATED,    // Invoice created, QR displayed
    PENDING,    // Transaction detected, awaiting confirmation
    CONFIRMED,  // Transaction confirmed on-chain
    FAILED,     // Transaction failed
    EXPIRED,    // No payment within timeout
    CANCELED    // Explicitly canceled by merchant
}

/**
 * Payment currency.
 */
enum class PaymentCurrency {
    SOL,
    USDC,
    USDT
}

/**
 * Room entity representing a Solana Pay invoice.
 *
 * In Solana Pay flow:
 * 1. Merchant creates invoice with unique `reference` pubkey
 * 2. Customer scans QR, wallet includes `reference` as readonly account
 * 3. Merchant polls for payment:
 *    - SOL: getSignaturesForAddress(recipientAddress) - wallet directly receives SOL
 *    - SPL: getSignaturesForAddress(recipientTokenAccount) - ATA receives tokens
 * 4. When found, validates and stores `foundSignature`
 *
 * CRITICAL FOR SPL DETECTION:
 * For SPL tokens, the wallet pubkey is NOT in transaction accountKeys.
 * Only the ATA (Associated Token Account) appears. So we must poll the ATA.
 * See `getPollingAddress()` for correct address to poll.
 */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey
    val id: String,                     // UUID invoice identifier
    val reference: String,              // Base58 pubkey for payment detection
    val recipientAddress: String,       // Merchant wallet address (owner)
    val recipientTokenAccount: String?, // Merchant's ATA for SPL tokens (null for SOL)
    val amount: Long,                   // Amount in minor units (lamports or raw token)
    val currency: PaymentCurrency,      // SOL or USDC
    val splTokenMint: String?,          // Token mint address (null for SOL)
    val railId: String,                 // Payment rail identifier (e.g., "solana", "evm", "tron")
    val networkId: String,              // Network/cluster identifier (e.g., "devnet", "mainnet-beta", "1", "137")
    val status: InvoiceStatus,
    val foundSignature: String?,        // Transaction signature when detected
    val memo: String?,                  // Optional memo (usually invoiceId)
    val label: String?,                 // Business name
    val message: String?,               // Payment message
    val createdAt: Long,
    val updatedAt: Long,
    val confirmedAt: Long?,
    val failureReason: String? = null,  // Reason for FAILED status (detector rejection, RPC error, etc.)

    // ━━━━━ FX RATE TRANSPARENCY (IMMUTABLE AFTER CREATION) ━━━━━
    // CRITICAL: No Double for financial data. All monetary values as Long (minor units).
    // Rate stored as exact BigDecimal string to preserve precision.
    val fiatAmountMinor: Long? = null,   // Original amount in minor units (e.g., kopeks for RUB)
    val fiatCurrency: String? = null,    // Merchant's currency code (e.g., "RUB")
    val fiatDecimals: Int? = null,       // Decimal places for fiat (e.g., 2 for RUB)
    val rateUsed: String? = null,        // Exact rate as BigDecimal.toPlainString() (e.g., "76.65")
    val rateDirection: String? = null,   // Human readable (e.g., "1 USD = 76.65 RUB")
    val rateProvider: String? = null,    // Provider name (e.g., "Frankfurter")
    val rateDate: String? = null,        // Rate date from provider
    val rateFetchedAt: Long? = null,     // When rate was fetched (timestamp)

    // ━━━━━ DUAL STABLECOIN ACCEPTANCE ━━━━━
    // Tracks actual payment token when different from expected (USDC/USDT interchangeable)
    val actualMintUsed: String? = null,      // Actual token mint used for payment (null if same as expected or SOL)
    val paymentWarningCode: String? = null,  // Warning code if paid with different stablecoin

    // ━━━━━ SOFT-EXPIRE SUPPORT ━━━━━
    // Deadline stored in DB so worker can respect extensions
    val deadlineAt: Long = 0L,               // Detection deadline (0 = no deadline set yet)

    // ━━━━━ PAYER INFO (MULTICHAIN-READY) ━━━━━
    // Extracted from transaction at confirmation time for refund support.
    // Chain-specific extraction happens in PaymentRail/Validator.
    val payerAddress: String? = null,        // Wallet address of payer (owner for Solana SPL, sender for EVM)
    val payerTokenAccount: String? = null,   // Token account address (SPL ATA for Solana, wallet for EVM)
    val refundTxId: String? = null           // Transaction ID if refund was issued
) {
    companion object {
        const val LAMPORTS_PER_SOL = 1_000_000_000L
        const val USDC_DECIMALS = 6
        const val USDC_DIVISOR = 1_000_000L
        const val USDT_DECIMALS = 6
        const val USDT_DIVISOR = 1_000_000L
        /** Common divisor for stablecoins (USDC/USDT both use 6 decimals on Solana) */
        const val STABLECOIN_DIVISOR = 1_000_000L

        /**
         * Create a new invoice with IMMUTABLE FX snapshot.
         *
         * CRITICAL: FX data is frozen at creation time and NEVER recomputed.
         * All monetary values stored as Long (minor units), rates as exact String.
         *
         * @param recipientTokenAccount The ATA for SPL tokens, null for SOL.
         *        MUST be derived using SolanaAddressUtils.deriveAssociatedTokenAddress()
         *        at invoice creation time. This is the address we poll for SPL payments.
         */
        /**
         * Default rail ID for new invoices.
         * Used when creating invoices without explicit rail specification.
         */
        const val DEFAULT_RAIL_ID = "solana"

        fun create(
            id: String,
            reference: String,
            recipientAddress: String,
            recipientTokenAccount: String?,  // ATA for SPL, null for native
            amount: Long,
            currency: PaymentCurrency,
            splTokenMint: String?,
            railId: String = DEFAULT_RAIL_ID,  // Payment rail (solana, evm, tron)
            networkId: String = BuildConfig.SOLANA_CLUSTER,  // Network (devnet, mainnet-beta, 1, 137)
            label: String? = null,
            message: String? = null,
            // FX rate transparency fields (IMMUTABLE after creation)
            fiatAmountMinor: Long? = null,
            fiatCurrency: String? = null,
            fiatDecimals: Int? = null,
            rateUsed: String? = null,
            rateDirection: String? = null,
            rateProvider: String? = null,
            rateDate: String? = null,
            rateFetchedAt: Long? = null
        ): InvoiceEntity {
            val now = System.currentTimeMillis()
            return InvoiceEntity(
                id = id,
                reference = reference,
                recipientAddress = recipientAddress,
                recipientTokenAccount = recipientTokenAccount,
                amount = amount,
                currency = currency,
                splTokenMint = splTokenMint,
                railId = railId,
                networkId = networkId,
                status = InvoiceStatus.CREATED,
                foundSignature = null,
                memo = id,
                label = label,
                message = message,
                createdAt = now,
                updatedAt = now,
                confirmedAt = null,
                // FX rate metadata (frozen at creation)
                fiatAmountMinor = fiatAmountMinor,
                fiatCurrency = fiatCurrency,
                fiatDecimals = fiatDecimals,
                rateUsed = rateUsed,
                rateDirection = rateDirection,
                rateProvider = rateProvider,
                rateDate = rateDate,
                rateFetchedAt = rateFetchedAt
            )
        }
    }

    /**
     * Mark as pending (transaction detected, not yet confirmed).
     *
     * @param signature Transaction signature
     * @param actualMint Actual mint used (null if same as expected or SOL)
     * @param warningCode Warning code if paid with different stablecoin
     * @param payer Payer wallet address (owner for SPL, sender for native)
     * @param payerToken Payer's token account (SPL ATA for Solana)
     */
    fun markPending(
        signature: String,
        actualMint: String? = null,
        warningCode: String? = null,
        payer: String? = null,
        payerToken: String? = null
    ): InvoiceEntity {
        return copy(
            status = InvoiceStatus.PENDING,
            foundSignature = signature,
            actualMintUsed = actualMint,
            paymentWarningCode = warningCode,
            payerAddress = payer,
            payerTokenAccount = payerToken,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark as confirmed.
     *
     * @param signature Transaction signature
     * @param actualMint Actual mint used (null if same as expected or SOL)
     * @param warningCode Warning code if paid with different stablecoin
     * @param payer Payer wallet address (owner for SPL, sender for native)
     * @param payerToken Payer's token account (SPL ATA for Solana)
     */
    fun markConfirmed(
        signature: String,
        actualMint: String? = null,
        warningCode: String? = null,
        payer: String? = null,
        payerToken: String? = null
    ): InvoiceEntity {
        val now = System.currentTimeMillis()
        return copy(
            status = InvoiceStatus.CONFIRMED,
            foundSignature = signature,
            actualMintUsed = actualMint,
            paymentWarningCode = warningCode,
            payerAddress = payer,
            payerTokenAccount = payerToken,
            updatedAt = now,
            confirmedAt = now
        )
    }

    /**
     * Mark refund as sent.
     */
    fun markRefundSent(txId: String): InvoiceEntity {
        return copy(
            refundTxId = txId,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Check if refund is possible (confirmed payment with payer address).
     */
    fun canRefund(): Boolean {
        return status == InvoiceStatus.CONFIRMED &&
               payerAddress != null &&
               refundTxId == null
    }

    /**
     * Mark as failed with optional reason.
     */
    fun markFailed(reason: String? = null): InvoiceEntity {
        return copy(
            status = InvoiceStatus.FAILED,
            failureReason = reason,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark as expired.
     */
    fun markExpired(): InvoiceEntity {
        return copy(
            status = InvoiceStatus.EXPIRED,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark as canceled by merchant.
     */
    fun markCanceled(): InvoiceEntity {
        return copy(
            status = InvoiceStatus.CANCELED,
            failureReason = "Canceled by merchant",
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Extend deadline for soft-expire support.
     * Called when merchant clicks "Wait 5 more minutes".
     */
    fun extendDeadline(newDeadlineAt: Long): InvoiceEntity {
        return copy(
            deadlineAt = newDeadlineAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Set initial deadline.
     */
    fun withDeadline(deadlineAt: Long): InvoiceEntity {
        return copy(deadlineAt = deadlineAt)
    }

    /**
     * Get the address to poll for payment detection.
     *
     * CRITICAL FOR SPL DETECTION:
     * - SOL: Poll the wallet address directly (receives SOL)
     * - SPL: Poll the ATA (Associated Token Account) which receives tokens
     *
     * For SPL tokens, the wallet pubkey is NOT in transaction accountKeys.
     * Only the ATA appears. So polling wallet returns no signatures.
     *
     * @return The address to use with getSignaturesForAddress()
     * @throws IllegalStateException if SPL invoice has no recipientTokenAccount
     */
    fun getPollingAddress(): String {
        return when (currency) {
            PaymentCurrency.SOL -> recipientAddress
            PaymentCurrency.USDC, PaymentCurrency.USDT -> {
                requireNotNull(recipientTokenAccount) {
                    "SPL invoice must have recipientTokenAccount (ATA) for polling"
                }
                recipientTokenAccount
            }
        }
    }

    /**
     * Check if this invoice requires ATA polling (SPL token).
     */
    fun requiresAtaPolling(): Boolean = currency != PaymentCurrency.SOL

    /**
     * Get amount in display units.
     */
    fun getDisplayAmount(): Double {
        return when (currency) {
            PaymentCurrency.SOL -> amount.toDouble() / LAMPORTS_PER_SOL
            PaymentCurrency.USDC -> amount.toDouble() / USDC_DIVISOR
            PaymentCurrency.USDT -> amount.toDouble() / USDT_DIVISOR
        }
    }

    /**
     * Get amount as string for Solana Pay URL.
     *
     * USDC amounts are pre-rounded to 1 decimal place (CEILING) for clean
     * customer-facing amounts, so we format with 1 decimal.
     *
     * CRITICAL: Uses BigDecimal for exact division (no floating-point errors).
     * RoundingMode.UNNECESSARY ensures we fail fast if amount wasn't properly rounded.
     */
    fun getAmountString(): String {
        return when (currency) {
            PaymentCurrency.SOL -> String.format("%.9f", amount.toDouble() / LAMPORTS_PER_SOL)
            PaymentCurrency.USDC, PaymentCurrency.USDT -> {
                // Use BigDecimal for exact division - NEVER use Double for money
                val amountBD = java.math.BigDecimal.valueOf(amount)
                val divisor = java.math.BigDecimal.valueOf(STABLECOIN_DIVISOR)
                // RoundingMode.UNNECESSARY throws if rounding is needed (catches bugs!)
                amountBD.divide(divisor, 1, java.math.RoundingMode.UNNECESSARY).toPlainString()
            }
        }
    }

    /**
     * Get fiat amount in display units (from minor units).
     * Uses BigDecimal for precision.
     */
    fun getFiatDisplayAmount(): java.math.BigDecimal? {
        val minor = fiatAmountMinor ?: return null
        val decimals = fiatDecimals ?: 2
        return java.math.BigDecimal.valueOf(minor)
            .divide(java.math.BigDecimal.TEN.pow(decimals))
    }

    /**
     * Get rate as BigDecimal (from stored string).
     */
    fun getRateAsBigDecimal(): java.math.BigDecimal? {
        return rateUsed?.let { java.math.BigDecimal(it) }
    }

    /**
     * Get stablecoin amount as BigDecimal (for precise formatting).
     */
    fun getStablecoinAmountBigDecimal(): java.math.BigDecimal {
        val divisor = when (currency) {
            PaymentCurrency.SOL -> java.math.BigDecimal.valueOf(LAMPORTS_PER_SOL)
            PaymentCurrency.USDC -> java.math.BigDecimal.valueOf(USDC_DIVISOR)
            PaymentCurrency.USDT -> java.math.BigDecimal.valueOf(USDT_DIVISOR)
        }
        return java.math.BigDecimal.valueOf(amount).divide(divisor, 6, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Format fiat amount for display (e.g. "100.50").
     * Uses BigDecimal to avoid floating point errors.
     */
    fun formatFiatAmount(): String? {
        val fiatBD = getFiatDisplayAmount() ?: return null
        val decimals = fiatDecimals ?: 2
        return fiatBD.setScale(decimals, java.math.RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Format stablecoin amount for display (e.g. "1.30").
     * Uses BigDecimal to avoid floating point errors.
     */
    fun formatStablecoinAmount(): String {
        val stablecoinBD = getStablecoinAmountBigDecimal()
        return stablecoinBD.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Format rate for display (e.g. "1 USD = 35.50 THB").
     */
    fun formatRateDisplay(): String? {
        val rate = getRateAsBigDecimal() ?: return null
        val currency = fiatCurrency ?: return null
        // Format rate with 2 decimal places
        val formattedRate = rate.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
        return "1 USD = $formattedRate $currency"
    }

    /**
     * Get the stablecoin label (USDC or USDT).
     * For dual stablecoin acceptance, uses actualMintUsed if available.
     */
    fun getStablecoinLabel(): String {
        return when {
            actualMintUsed != null -> {
                // Use actual mint to determine label
                when {
                    actualMintUsed == BuildConfig.USDC_MINT -> "USDC"
                    actualMintUsed == BuildConfig.USDT_MINT -> "USDT"
                    else -> currency.name
                }
            }
            else -> currency.name
        }
    }
}
