package com.winopay.data

import android.util.Log
import com.winopay.data.model.RatesSnapshot
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Centralized currency conversion utility using BigDecimal for precision.
 *
 * CRITICAL REQUIREMENTS:
 * 1. All rates come from real FX provider (Frankfurter API) - NO hardcoded fallbacks
 * 2. If rates unavailable, conversion FAILS explicitly - NO silent fallbacks
 * 3. All financial calculations use BigDecimal to avoid floating-point errors
 * 4. Rate direction is EXPLICIT: rates are "units of currency per 1 USD"
 *    Example: RUB = 90.5 means 1 USD = 90.5 RUB
 *    To convert RUB to USD: usdAmount = rubAmount / rubPerUsd
 */
object CurrencyConverter {

    private const val TAG = "CurrencyConverter"

    // BigDecimal scale for intermediate calculations (8 decimals for precision)
    private const val CALC_SCALE = 8

    // Final stablecoin display scale (2 decimal places for precise amounts)
    // Applies to both USDC and USDT equally (1:1 with USD)
    private const val STABLECOIN_DISPLAY_SCALE = 2

    /**
     * Round a stablecoin amount UP (CEILING) to 2 decimal places.
     *
     * This ensures merchant-favorable rounding for customer-facing amounts:
     * - 0.109 → 0.11
     * - 0.121 → 0.13
     * - 0.120 → 0.12
     *
     * IMPORTANT: This is applied AFTER all FX conversion and slippage calculations.
     * Works for both USDC and USDT since both are 1:1 with USD.
     *
     * @param amount The precise USD/stablecoin amount (e.g., 123.456789)
     * @return The rounded amount (e.g., 123.46)
     */
    fun roundUpToTwoDecimals(amount: BigDecimal): BigDecimal {
        return amount.setScale(STABLECOIN_DISPLAY_SCALE, RoundingMode.CEILING)
    }

    // Current rates snapshot (null if not loaded)
    @Volatile
    private var currentSnapshot: RatesSnapshot? = null

    // In-memory cache of rates as BigDecimal
    @Volatile
    private var currentRates: Map<String, BigDecimal> = emptyMap()

    private val initMutex = Mutex()

    /**
     * Immutable conversion result with full transparency.
     * All values stored in formats suitable for InvoiceEntity persistence.
     *
     * CRITICAL: This is the FROZEN FX snapshot for the invoice.
     * NEVER recompute after invoice creation.
     */
    data class ConversionResult(
        // Input (fiat)
        val fiatAmountMinor: Long,           // Original amount in minor units (e.g., kopeks)
        val fiatCurrency: String,            // Currency code (e.g., "RUB")
        val fiatDecimals: Int,               // Decimal places (e.g., 2)
        // Output (stablecoin - USDC or USDT, both 1:1 with USD)
        val usdcMinor: Long,                 // Final stablecoin in minor units (6 decimals)
        // Rate (stored as exact string)
        val rateUsed: String,                // BigDecimal.toPlainString() for exact storage
        val rateDirection: String,           // e.g., "1 USD = 76.65 RUB"
        val provider: String,
        val rateDate: String,
        val rateFetchedAt: Long,
        // Conversion details
        val usdPrecise: String,              // USD amount before CEILING (full precision)
        val slippageApplied: Boolean,
        val roundingMode: String             // Always "CEILING"
    )

    /**
     * Check if valid rates are available.
     * CRITICAL: Must be checked before any invoice creation.
     */
    fun hasValidRates(): Boolean {
        val snapshot = currentSnapshot ?: return false
        return snapshot.isValid() && currentRates.isNotEmpty()
    }

    /**
     * Get current snapshot info (for display).
     */
    fun getSnapshotInfo(): RatesSnapshot? = currentSnapshot

    /**
     * Initialize converter with rates from repository.
     * Returns true if rates were successfully loaded.
     */
    suspend fun initialize(repository: RatesRepository): Boolean {
        initMutex.withLock {
            val snapshot = repository.getRates()

            if (snapshot == null) {
                Log.e(TAG, "━━━━━ RATES INITIALIZATION FAILED ━━━━━")
                Log.e(TAG, "  No rates available from provider")
                Log.e(TAG, "  Invoice creation will be BLOCKED")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                currentSnapshot = null
                currentRates = emptyMap()
                return false
            }

            currentSnapshot = snapshot
            currentRates = snapshot.rates.mapValues { BigDecimal.valueOf(it.value) }

            Log.d(TAG, "━━━━━ RATES INITIALIZED ━━━━━")
            Log.d(TAG, "  Provider: ${snapshot.provider}")
            Log.d(TAG, "  Date: ${snapshot.date}")
            Log.d(TAG, "  Age: ${snapshot.getAgeString()}")
            Log.d(TAG, "  Currencies: ${currentRates.size}")
            Log.d(TAG, "  Fresh: ${snapshot.isFresh()}")
            // Log key rates for debugging
            listOf("RUB", "THB", "EUR", "AED", "GBP").forEach { code ->
                currentRates[code]?.let { rate ->
                    Log.d(TAG, "  $code: 1 USD = $rate $code")
                }
            }
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return true
        }
    }

    /**
     * Force refresh rates from network.
     * Returns true if refresh succeeded.
     */
    suspend fun refresh(repository: RatesRepository): Boolean {
        initMutex.withLock {
            val snapshot = repository.forceRefresh()

            if (snapshot == null) {
                Log.e(TAG, "Rate refresh failed - keeping existing rates if valid")
                return currentSnapshot?.isValid() == true
            }

            currentSnapshot = snapshot
            currentRates = snapshot.rates.mapValues { BigDecimal.valueOf(it.value) }
            Log.d(TAG, "Refreshed rates: ${currentRates.size} currencies (${snapshot.provider}, ${snapshot.date})")
            return true
        }
    }

    /**
     * Get rate for a currency.
     * Returns null if rate not available - NEVER falls back to hardcoded value.
     */
    fun getRate(currency: String): BigDecimal? {
        if (currency == "USD") return BigDecimal.ONE
        return currentRates[currency]
    }

    /**
     * Get rate direction string for display.
     * Example: "1 USD = 90.5 RUB"
     */
    fun getRateDisplayString(currency: String): String? {
        val rate = getRate(currency) ?: return null
        return "1 USD = $rate $currency"
    }

    /**
     * Convert an amount from the given currency to USD using BigDecimal.
     *
     * RATE DIRECTION: rates are "units of currency per 1 USD"
     * Formula: usdAmount = localAmount / rate
     *
     * @param amount The amount in the source currency
     * @param currency The source currency code (e.g., "RUB", "THB")
     * @return The equivalent amount in USD, or null if rate unavailable
     */
    fun convertToUsdBigDecimal(amount: BigDecimal, currency: String): BigDecimal? {
        // Zero always returns zero
        if (amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        // USD to USD is 1:1
        if (currency == "USD") return amount

        // Look up rate (currency per USD)
        val rate = currentRates[currency]
        if (rate == null) {
            Log.e(TAG, "❌ NO RATE AVAILABLE for '$currency' - conversion BLOCKED")
            return null // EXPLICIT FAILURE - no fallback
        }

        // Rate direction: rate = units of currency per 1 USD
        // To convert currency to USD: usdAmount = currencyAmount / rate
        val result = amount.divide(rate, CALC_SCALE, RoundingMode.HALF_UP)

        Log.d(TAG, "CONVERT: $amount $currency ÷ $rate = $result USD")

        return result
    }

    /**
     * Convert an amount from the given currency to USD (Double convenience method).
     * Returns null if rate unavailable - NEVER returns fallback value.
     */
    fun convertToUsd(amount: Double, currency: String): Double? {
        if (amount == 0.0) return 0.0
        if (currency == "USD") return amount

        val result = convertToUsdBigDecimal(BigDecimal.valueOf(amount), currency) ?: return null
        return result.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Convert an amount FROM USD TO the given currency using BigDecimal.
     *
     * RATE DIRECTION: rates are "units of currency per 1 USD"
     * Formula: localAmount = usdAmount * rate
     *
     * @param usdAmount The amount in USD
     * @param currency The target currency code (e.g., "RUB", "THB")
     * @return The equivalent amount in the target currency, or null if rate unavailable
     */
    fun convertFromUsdBigDecimal(usdAmount: BigDecimal, currency: String): BigDecimal? {
        // Zero always returns zero
        if (usdAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO

        // USD to USD is 1:1
        if (currency == "USD") return usdAmount

        // Look up rate (currency per USD)
        val rate = currentRates[currency]
        if (rate == null) {
            Log.e(TAG, "❌ NO RATE AVAILABLE for '$currency' - conversion BLOCKED")
            return null // EXPLICIT FAILURE - no fallback
        }

        // Rate direction: rate = units of currency per 1 USD
        // To convert USD to currency: currencyAmount = usdAmount * rate
        val result = usdAmount.multiply(rate).setScale(CALC_SCALE, RoundingMode.HALF_UP)

        Log.d(TAG, "CONVERT: $usdAmount USD × $rate = $result $currency")

        return result
    }

    /**
     * Convert an amount from USD to the given currency (Double convenience method).
     * Returns null if rate unavailable - NEVER returns fallback value.
     */
    fun convertFromUsd(usdAmount: Double, currency: String): Double? {
        if (usdAmount == 0.0) return 0.0
        if (currency == "USD") return usdAmount

        val result = convertFromUsdBigDecimal(BigDecimal.valueOf(usdAmount), currency) ?: return null
        return result.setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Convert fiat minor units to stablecoin (USDC/USDT) minor units with full transparency.
     *
     * STRICT BIGDECIMAL PIPELINE:
     * 1. fiatMinor (Long) → BigDecimal
     * 2. fiatMajor = fiatMinor / 10^decimals
     * 3. usdAmount = fiatMajor / rate (high precision, NO rounding)
     * 4. if slippage: usdAmount = usdAmount × 1.025
     * 5. usdcMinorPrecise = usdAmount × 1_000_000
     * 6. usdcMinor = CEILING to Long (ONLY rounding in entire pipeline)
     *
     * USD MERCHANTS: No FX, no slippage, 1 USD = 1 stablecoin (USDC or USDT)
     *
     * @param fiatAmountMinor Amount in minor units (e.g., kopeks for RUB)
     * @param fiatCurrency Currency code (e.g., "RUB")
     * @param fiatDecimals Decimal places for the currency (e.g., 2 for RUB)
     * @return ConversionResult with immutable FX snapshot, or null if conversion impossible
     */
    fun convertToUsdcMinorWithMetadata(
        fiatAmountMinor: Long,
        fiatCurrency: String,
        fiatDecimals: Int
    ): ConversionResult? {
        require(fiatAmountMinor > 0) { "Amount must be positive, got: $fiatAmountMinor" }

        val snapshot = currentSnapshot

        // ━━━━━ USD MERCHANT FAST PATH ━━━━━
        // No FX conversion needed, no slippage, 1 USD = 1 USDC
        if (fiatCurrency == "USD") {
            Log.d(TAG, "━━━━━ USD CONVERSION (NO FX) ━━━━━")
            Log.d(TAG, "  INPUT: $fiatAmountMinor cents USD")

            // Convert cents to USD (major units)
            val centsBD = BigDecimal.valueOf(fiatAmountMinor)
            val usdAmount = centsBD.divide(BigDecimal(100), CALC_SCALE, RoundingMode.UNNECESSARY)
            Log.d(TAG, "  USD_AMOUNT (precise): $usdAmount")

            // Round UP to 2 decimal places for precise amount
            val usdcRounded = roundUpToTwoDecimals(usdAmount)
            Log.d(TAG, "  USDC_ROUNDED (2 decimals): $usdcRounded")

            // Convert to minor units (× 1,000,000) - exact, no further rounding needed
            val usdcMinor = usdcRounded.multiply(BigDecimal(1_000_000)).longValueExact()
            Log.d(TAG, "  USDC_MINOR: $usdcMinor")
            Log.d(TAG, "  SLIPPAGE: NONE (USD merchant)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            return ConversionResult(
                fiatAmountMinor = fiatAmountMinor,
                fiatCurrency = fiatCurrency,
                fiatDecimals = fiatDecimals,
                usdcMinor = usdcMinor,
                rateUsed = "1",
                rateDirection = "1 USD = 1 USD",
                provider = "identity",
                rateDate = snapshot?.date ?: "",
                rateFetchedAt = System.currentTimeMillis(),
                usdPrecise = usdAmount.toPlainString(),
                slippageApplied = false,
                roundingMode = "CEILING_2_DECIMALS"
            )
        }

        // ━━━━━ NON-USD: REQUIRE VALID RATES ━━━━━
        if (snapshot == null || !snapshot.isValid()) {
            Log.e(TAG, "━━━━━ CONVERSION BLOCKED ━━━━━")
            Log.e(TAG, "  Reason: No valid rates available")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return null
        }

        val rate = getRate(fiatCurrency)
        if (rate == null) {
            Log.e(TAG, "━━━━━ CONVERSION BLOCKED ━━━━━")
            Log.e(TAG, "  Reason: No rate for currency '$fiatCurrency'")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return null
        }

        Log.d(TAG, "━━━━━ STABLECOIN CONVERSION ━━━━━")
        Log.d(TAG, "  INPUT: $fiatAmountMinor minor units ($fiatCurrency, $fiatDecimals decimals)")
        Log.d(TAG, "  RATE: 1 USD = $rate $fiatCurrency")
        Log.d(TAG, "  PROVIDER: ${snapshot.provider}")
        Log.d(TAG, "  DATE: ${snapshot.date}")
        Log.d(TAG, "  AGE: ${snapshot.getAgeString()}")

        // Step 1: fiatMinor → BigDecimal
        val fiatMinorBD = BigDecimal.valueOf(fiatAmountMinor)

        // Step 2: fiatMajor = fiatMinor / 10^decimals
        val divisor = BigDecimal.TEN.pow(fiatDecimals)
        val fiatMajor = fiatMinorBD.divide(divisor, CALC_SCALE, RoundingMode.UNNECESSARY)
        Log.d(TAG, "  FIAT_MAJOR: $fiatMajor $fiatCurrency")

        // Step 3: usdAmount = fiatMajor / rate (HIGH PRECISION, NO ROUNDING)
        val usdAmount = fiatMajor.divide(rate, CALC_SCALE, RoundingMode.HALF_UP)
        Log.d(TAG, "  USD_AMOUNT (precise): $usdAmount")

        // Step 4: Apply 2.5% slippage for FX protection (non-USD only)
        val slippageMultiplier = BigDecimal("1.025")
        val usdWithSlippage = usdAmount.multiply(slippageMultiplier)
        Log.d(TAG, "  USD_WITH_SLIPPAGE (+2.5%): $usdWithSlippage")

        // Step 5: Round UP to 2 decimal places for precise amount
        val usdcRounded = roundUpToTwoDecimals(usdWithSlippage)
        Log.d(TAG, "  USDC_ROUNDED (2 decimals): $usdcRounded")

        // Merchant is already protected by:
        // 1) 2.5% slippage (step 4 above)
        // 2) CEILING rounding (step 5 above)
        // Both bias in merchant's favor. No additional guard needed.
        //
        // NOTE: A previous guard compared usdcRounded (USD) with fiatMajor (local currency).
        // This was WRONG for currencies weaker than USD (THB, RUB, INR, etc.)
        // because fiatMajor >> usdcRounded numerically, causing massive overcharges.
        // Example: 1000 THB → 28.88 USDC correct, but guard forced 1000 USDC (35x overcharge).
        val finalUsdc = usdcRounded
        Log.d(TAG, "  USDC_FINAL: $finalUsdc")

        // Step 6: Convert to minor units (× 1,000,000) - exact, no further rounding needed
        val usdcMinor = finalUsdc.multiply(BigDecimal(1_000_000)).longValueExact()
        Log.d(TAG, "  USDC_MINOR (final): $usdcMinor")
        Log.d(TAG, "  ROUNDING: CEILING to 2 decimals (protects merchant)")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return ConversionResult(
            fiatAmountMinor = fiatAmountMinor,
            fiatCurrency = fiatCurrency,
            fiatDecimals = fiatDecimals,
            usdcMinor = usdcMinor,
            rateUsed = rate.toPlainString(),
            rateDirection = "1 USD = $rate $fiatCurrency",
            provider = snapshot.provider,
            rateDate = snapshot.date,
            rateFetchedAt = snapshot.lastUpdated,
            usdPrecise = usdAmount.toPlainString(),
            slippageApplied = true,
            roundingMode = "CEILING_2_DECIMALS_MERCHANT_PROTECTED"
        )
    }

    /**
     * Convert to stablecoin minor units (simplified method).
     * Returns null if conversion impossible.
     *
     * @param fiatAmountMinor Amount in minor units (e.g., cents, kopeks)
     * @param fiatCurrency Currency code
     * @param fiatDecimals Decimal places for the currency
     */
    fun convertToUsdcMinor(fiatAmountMinor: Long, fiatCurrency: String, fiatDecimals: Int): Long? {
        val result = convertToUsdcMinorWithMetadata(fiatAmountMinor, fiatCurrency, fiatDecimals) ?: return null
        return result.usdcMinor
    }

    /**
     * Check if a currency requires FX rates for conversion.
     *
     * CRITICAL: USD merchants NEVER need FX rates.
     * 1 USD = 1 stablecoin (USDC/USDT) exactly, no conversion needed.
     *
     * @param currency Currency code
     * @return true if rates are required, false for USD
     */
    fun requiresRates(currency: String): Boolean {
        return currency != "USD"
    }

    /**
     * Validate rates are available and reasonable.
     * Returns error message if validation fails, null if OK.
     *
     * IMPORTANT: Call requiresRates() first to check if validation is needed.
     * USD merchants should NEVER be blocked by rate validation.
     */
    fun validateRates(): String? {
        val snapshot = currentSnapshot
        if (snapshot == null) {
            return "Exchange rates not loaded"
        }
        if (!snapshot.isValid()) {
            return "Exchange rates expired (${snapshot.getAgeString()})"
        }
        if (currentRates.isEmpty()) {
            return "No exchange rates available"
        }
        return null // OK
    }

    /**
     * Validate rates for a specific currency.
     * Returns null for USD (never needs validation).
     * Returns error message for non-USD currencies if rates unavailable.
     */
    fun validateRatesForCurrency(currency: String): String? {
        // USD merchants NEVER need FX rates
        if (!requiresRates(currency)) {
            return null // Always OK for USD
        }
        return validateRates()
    }

    /**
     * Debug: Run basic sanity checks (no hardcoded expected values).
     */
    fun runSanityChecks(): Boolean {
        var passed = true

        Log.d(TAG, "━━━━━ SANITY CHECKS ━━━━━")

        // Check 1: Must have valid rates
        val validation = validateRates()
        if (validation != null) {
            Log.e(TAG, "❌ FAIL: $validation")
            passed = false
        } else {
            Log.d(TAG, "✓ Rates available and valid")
        }

        // Check 2: USD -> USD should be 1:1
        val usdCheck = convertToUsd(10.0, "USD")
        if (usdCheck != 10.0) {
            Log.e(TAG, "❌ FAIL: convertToUsd(10, USD) = $usdCheck (expected 10.0)")
            passed = false
        } else {
            Log.d(TAG, "✓ USD->USD: 10.0 -> $usdCheck")
        }

        // Check 3: Zero amount should return zero
        val zeroCheck = convertToUsd(0.0, "THB")
        if (zeroCheck != 0.0) {
            Log.e(TAG, "❌ FAIL: convertToUsd(0, THB) = $zeroCheck (expected 0.0)")
            passed = false
        } else {
            Log.d(TAG, "✓ Zero amount: 0.0 -> $zeroCheck")
        }

        // Check 4: All rates must be positive (no hardcoded expected ranges)
        currentRates.forEach { (code, rate) ->
            if (rate <= BigDecimal.ZERO) {
                Log.e(TAG, "❌ FAIL: Invalid rate for $code: $rate (must be > 0)")
                passed = false
            }
        }
        if (passed) {
            Log.d(TAG, "✓ All rates are positive")
        }

        // Check 5: USDC minor unit conversion (1000 cents = $10 USD = 10_000_000 USDC minor)
        val usdcMinor = convertToUsdcMinor(
            fiatAmountMinor = 1000L,  // 1000 cents = $10
            fiatCurrency = "USD",
            fiatDecimals = 2
        )
        if (usdcMinor != 10_000_000L) {
            Log.e(TAG, "❌ FAIL: 1000 cents -> $usdcMinor USDC minor (expected 10000000)")
            passed = false
        } else {
            Log.d(TAG, "✓ USDC minor: 1000 cents -> $usdcMinor")
        }

        Log.d(TAG, if (passed) "━━━━━ ALL CHECKS PASSED ━━━━━" else "━━━━━ CHECKS FAILED ━━━━━")
        return passed
    }

    /**
     * Get currently loaded rates (for debugging).
     */
    fun getCurrentRates(): Map<String, Double> = currentRates.mapValues { it.value.toDouble() }
}
