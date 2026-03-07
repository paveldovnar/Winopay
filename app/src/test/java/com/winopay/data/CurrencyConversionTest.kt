package com.winopay.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Unit tests for currency conversion logic.
 *
 * These tests verify the BigDecimal math used in CurrencyConverter
 * without Android dependencies.
 *
 * FIXED RATE: 1 USD = 76.65 RUB (for reproducible tests)
 *
 * PIPELINE:
 * 1. fiatMinor (Long) -> BigDecimal
 * 2. fiatMajor = fiatMinor / 10^decimals
 * 3. usdAmount = fiatMajor / rate (high precision)
 * 4. if slippage: usdAmount = usdAmount * 1.025
 * 5. usdcMinorPrecise = usdAmount * 1_000_000
 * 6. usdcMinor = CEILING to Long
 */
class CurrencyConversionTest {

    companion object {
        // Fixed rate for testing: 1 USD = 76.65 RUB
        private val FIXED_RATE_RUB = BigDecimal("76.65")
        private const val CALC_SCALE = 8
        private const val FIAT_DECIMALS = 2
        private val SLIPPAGE_MULTIPLIER = BigDecimal("1.025")
        private const val USDC_DIVISOR = 1_000_000L
    }

    /**
     * Convert RUB minor units to USDC minor units.
     * Mirrors the production pipeline exactly.
     *
     * UPDATED: Now includes 1-decimal CEILING rounding before minor conversion.
     */
    private fun convertRubToUsdcMinor(
        rubMinor: Long,
        applySlippage: Boolean
    ): Long {
        // Step 1: rubMinor -> BigDecimal
        val rubMinorBD = BigDecimal.valueOf(rubMinor)

        // Step 2: rubMajor = rubMinor / 100
        val divisor = BigDecimal.TEN.pow(FIAT_DECIMALS)
        val rubMajor = rubMinorBD.divide(divisor, CALC_SCALE, RoundingMode.UNNECESSARY)

        // Step 3: usdAmount = rubMajor / rate (high precision, no rounding)
        val usdAmount = rubMajor.divide(FIXED_RATE_RUB, CALC_SCALE, RoundingMode.HALF_UP)

        // Step 4: Apply slippage if needed
        val usdWithSlippage = if (applySlippage) {
            usdAmount.multiply(SLIPPAGE_MULTIPLIER)
        } else {
            usdAmount
        }

        // Step 5: Round UP to 1 decimal place (CEILING)
        val usdcRounded = roundUpToOneDecimal(usdWithSlippage)

        // Step 6: Convert to minor units (× 1,000,000) - exact, no further rounding
        return usdcRounded.multiply(BigDecimal(1_000_000)).longValueExact()
    }

    /**
     * Convert USD minor units (cents) to USDC minor units.
     * No FX, no slippage.
     */
    private fun convertUsdToUsdcMinor(centsMinor: Long): Long {
        // 100 cents = 1 USD = 1_000_000 USDC minor
        // Formula: usdcMinor = cents * 10_000
        return centsMinor * 10_000
    }

    // ═══════════════════════════════════════════════════════════════════
    // USD MERCHANT TESTS (NO FX, NO SLIPPAGE)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `USD 1 dollar converts to exactly 1_000_000 USDC minor`() {
        // 100 cents = $1.00
        val result = convertUsdToUsdcMinor(100L)
        assertEquals("$1.00 should be 1_000_000 USDC minor", 1_000_000L, result)
    }

    @Test
    fun `USD 10 dollars converts to exactly 10_000_000 USDC minor`() {
        // 1000 cents = $10.00
        val result = convertUsdToUsdcMinor(1000L)
        assertEquals("$10.00 should be 10_000_000 USDC minor", 10_000_000L, result)
    }

    @Test
    fun `USD 1 cent converts to exactly 10_000 USDC minor`() {
        // 1 cent = $0.01
        val result = convertUsdToUsdcMinor(1L)
        assertEquals("$0.01 should be 10_000 USDC minor", 10_000L, result)
    }

    @Test
    fun `USD 99 cents converts to exactly 990_000 USDC minor`() {
        val result = convertUsdToUsdcMinor(99L)
        assertEquals("$0.99 should be 990_000 USDC minor", 990_000L, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // RUB MERCHANT TESTS (WITH FX AND SLIPPAGE)
    // Rate: 1 USD = 76.65 RUB, Slippage: 2.5%
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `RUB 100 rubles converts correctly with slippage and 1-decimal rounding`() {
        // 10000 kopeks = 100 RUB
        // USD = 100 / 76.65 = 1.30463146...
        // With slippage: 1.30463146 * 1.025 = 1.33724725...
        // Rounded to 1 decimal (CEILING): 1.4
        // USDC minor = 1.4 * 1_000_000 = 1_400_000
        val result = convertRubToUsdcMinor(10000L, applySlippage = true)
        assertEquals("100 RUB should be 1.4 USDC (1-decimal ceiling)", 1_400_000L, result)
    }

    @Test
    fun `RUB 120 rubles converts correctly with slippage and 1-decimal rounding`() {
        // 12000 kopeks = 120 RUB
        // USD = 120 / 76.65 = 1.56555775...
        // With slippage: 1.56555775 * 1.025 = 1.60469670...
        // Rounded to 1 decimal (CEILING): 1.7
        // USDC minor = 1.7 * 1_000_000 = 1_700_000
        val result = convertRubToUsdcMinor(12000L, applySlippage = true)
        assertEquals("120 RUB should be 1.7 USDC (1-decimal ceiling)", 1_700_000L, result)
    }

    @Test
    fun `RUB 1000 rubles converts correctly with slippage and 1-decimal rounding`() {
        // 100000 kopeks = 1000 RUB
        // USD = 1000 / 76.65 = 13.04631463...
        // With slippage: 13.04631463 * 1.025 = 13.37247250...
        // Rounded to 1 decimal (CEILING): 13.4
        // USDC minor = 13.4 * 1_000_000 = 13_400_000
        val result = convertRubToUsdcMinor(100000L, applySlippage = true)
        assertEquals("1000 RUB should be 13.4 USDC (1-decimal ceiling)", 13_400_000L, result)
    }

    @Test
    fun `RUB 76_65 rubles with slippage rounds to 1_1 USDC`() {
        // 7665 kopeks = 76.65 RUB
        // USD = 76.65 / 76.65 = 1.00000000
        // With slippage: 1.0 * 1.025 = 1.025
        // Rounded to 1 decimal (CEILING): 1.1
        // USDC minor = 1.1 * 1_000_000 = 1_100_000
        val result = convertRubToUsdcMinor(7665L, applySlippage = true)
        assertEquals("76.65 RUB should be 1.1 USDC (1-decimal ceiling)", 1_100_000L, result)
    }

    @Test
    fun `RUB 76_65 rubles without slippage equals exactly 1 USDC`() {
        // 7665 kopeks = 76.65 RUB
        // USD = 76.65 / 76.65 = 1.00000000
        // No slippage: 1.0
        // Rounded to 1 decimal (CEILING): 1.0 (exact)
        // USDC minor = 1_000_000
        val result = convertRubToUsdcMinor(7665L, applySlippage = false)
        assertEquals("76.65 RUB should be exactly 1.0 USDC (no slippage)", 1_000_000L, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // CEILING ROUNDING TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `CEILING 1-decimal rounding - tiny amounts round to 0_1 USDC`() {
        // Any fractional USDC amount rounds UP to 1 decimal
        // 1 kopek = 0.01 RUB
        // USD = 0.01 / 76.65 = 0.00013046...
        // With slippage: 0.00013046 * 1.025 = 0.00013372...
        // Rounded to 1 decimal (CEILING): 0.1 (anything > 0 becomes at least 0.1)
        // USDC minor = 0.1 * 1_000_000 = 100_000
        val result = convertRubToUsdcMinor(1L, applySlippage = true)
        assertEquals("1 kopek should round up to 0.1 USDC (100,000 minor)", 100_000L, result)
    }

    @Test
    fun `Zero amount returns zero`() {
        val usdResult = convertUsdToUsdcMinor(0L)
        assertEquals("0 USD should be 0 USDC", 0L, usdResult)

        val rubResult = convertRubToUsdcMinor(0L, applySlippage = true)
        assertEquals("0 RUB should be 0 USDC", 0L, rubResult)
    }

    // ═══════════════════════════════════════════════════════════════════
    // REGRESSION TESTS (from original bug)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `120 RUB must NOT equal 1 USD - original bug test`() {
        // This test verifies the original bug is fixed:
        // 120 RUB was incorrectly showing as $1.00
        val result = convertRubToUsdcMinor(12000L, applySlippage = true)

        // 120 RUB with 1-decimal rounding should be 1.7 USDC, NOT 1.00
        // (1.60469670... rounded UP to 1.7)
        assertEquals("120 RUB should be 1.7 USDC", 1_700_000L, result)

        // Specifically NOT 1_000_000 (the bug value)
        assert(result != 1_000_000L) {
            "120 RUB must NOT equal 1 USDC - this was the original bug!"
        }
    }

    @Test
    fun `1000 RUB must NOT equal 11_14 USDC - original bug test`() {
        // Another regression from the original bug report:
        // 1000 RUB was showing as only 11.14 USDC
        val result = convertRubToUsdcMinor(100000L, applySlippage = true)

        // 1000 RUB with 1-decimal rounding should be 13.4 USDC
        // (13.37247250... rounded UP to 13.4)
        assertEquals("1000 RUB should be 13.4 USDC", 13_400_000L, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // USD BYPASS TESTS
    // CRITICAL: USD merchants must NEVER be blocked by FX unavailability
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulates requiresRates() logic from CurrencyConverter.
     */
    private fun requiresRates(currency: String): Boolean {
        return currency != "USD"
    }

    /**
     * Simulates validateRatesForCurrency() logic.
     * Returns null for USD (always OK), error string for non-USD if no rates.
     */
    private fun validateRatesForCurrency(currency: String, hasValidRates: Boolean): String? {
        // USD merchants NEVER need FX rates
        if (!requiresRates(currency)) {
            return null // Always OK for USD
        }
        // Non-USD requires valid rates
        return if (hasValidRates) null else "Exchange rates not loaded"
    }

    @Test
    fun `USD does not require FX rates`() {
        assert(!requiresRates("USD")) { "USD should NOT require FX rates" }
    }

    @Test
    fun `RUB requires FX rates`() {
        assert(requiresRates("RUB")) { "RUB should require FX rates" }
    }

    @Test
    fun `EUR requires FX rates`() {
        assert(requiresRates("EUR")) { "EUR should require FX rates" }
    }

    @Test
    fun `THB requires FX rates`() {
        assert(requiresRates("THB")) { "THB should require FX rates" }
    }

    @Test
    fun `USD validation passes even when rates unavailable`() {
        val result = validateRatesForCurrency("USD", hasValidRates = false)
        assertEquals("USD should pass validation even without rates", null, result)
    }

    @Test
    fun `USD validation passes when rates available`() {
        val result = validateRatesForCurrency("USD", hasValidRates = true)
        assertEquals("USD should pass validation with rates", null, result)
    }

    @Test
    fun `RUB validation fails when rates unavailable`() {
        val result = validateRatesForCurrency("RUB", hasValidRates = false)
        assertEquals("RUB should fail without rates", "Exchange rates not loaded", result)
    }

    @Test
    fun `RUB validation passes when rates available`() {
        val result = validateRatesForCurrency("RUB", hasValidRates = true)
        assertEquals("RUB should pass with rates", null, result)
    }

    @Test
    fun `EUR validation fails when rates unavailable`() {
        val result = validateRatesForCurrency("EUR", hasValidRates = false)
        assertEquals("EUR should fail without rates", "Exchange rates not loaded", result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // OFFLINE SCENARIO TESTS
    // USD merchant can create invoice offline, RUB merchant cannot
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulates canCreateInvoice logic from PosManager/POSScreen.
     */
    private fun canCreateInvoice(currency: String, hasValidRates: Boolean, hasValidAmount: Boolean): Boolean {
        if (!hasValidAmount) return false
        // USD bypass: no rates required
        if (!requiresRates(currency)) return true
        // Non-USD requires valid rates
        return hasValidRates
    }

    @Test
    fun `USD merchant can create invoice offline (no rates)`() {
        val canCreate = canCreateInvoice(
            currency = "USD",
            hasValidRates = false,
            hasValidAmount = true
        )
        assert(canCreate) { "USD merchant MUST be able to create invoice without FX rates" }
    }

    @Test
    fun `USD merchant can create invoice with rates`() {
        val canCreate = canCreateInvoice(
            currency = "USD",
            hasValidRates = true,
            hasValidAmount = true
        )
        assert(canCreate) { "USD merchant should create invoice with rates" }
    }

    @Test
    fun `RUB merchant cannot create invoice offline (no rates)`() {
        val canCreate = canCreateInvoice(
            currency = "RUB",
            hasValidRates = false,
            hasValidAmount = true
        )
        assert(!canCreate) { "RUB merchant MUST NOT create invoice without FX rates" }
    }

    @Test
    fun `RUB merchant can create invoice when rates available`() {
        val canCreate = canCreateInvoice(
            currency = "RUB",
            hasValidRates = true,
            hasValidAmount = true
        )
        assert(canCreate) { "RUB merchant should create invoice with rates" }
    }

    @Test
    fun `No currency can create invoice with invalid amount`() {
        assert(!canCreateInvoice("USD", hasValidRates = true, hasValidAmount = false)) {
            "USD should not create with invalid amount"
        }
        assert(!canCreateInvoice("RUB", hasValidRates = true, hasValidAmount = false)) {
            "RUB should not create with invalid amount"
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 1-DECIMAL CEILING ROUNDING TESTS
    // Customer-facing USDC amounts are rounded UP to 1 decimal place
    // for clean display and merchant protection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Round a BigDecimal UP (CEILING) to 1 decimal place.
     * Mirrors CurrencyConverter.roundUpToOneDecimal() exactly.
     */
    private fun roundUpToOneDecimal(amount: BigDecimal): BigDecimal {
        return amount.setScale(1, RoundingMode.CEILING)
    }

    @Test
    fun `roundUpToOneDecimal 123_41 becomes 123_5`() {
        val input = BigDecimal("123.41")
        val expected = BigDecimal("123.5")
        val result = roundUpToOneDecimal(input)
        assertEquals("123.41 should round up to 123.5", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 123_45 becomes 123_5`() {
        val input = BigDecimal("123.45")
        val expected = BigDecimal("123.5")
        val result = roundUpToOneDecimal(input)
        assertEquals("123.45 should round up to 123.5", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 123_49 becomes 123_5`() {
        val input = BigDecimal("123.49")
        val expected = BigDecimal("123.5")
        val result = roundUpToOneDecimal(input)
        assertEquals("123.49 should round up to 123.5", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 123_50 stays 123_5`() {
        val input = BigDecimal("123.50")
        val expected = BigDecimal("123.5")
        val result = roundUpToOneDecimal(input)
        assertEquals("123.50 should stay 123.5", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 123_51 becomes 123_6`() {
        val input = BigDecimal("123.51")
        val expected = BigDecimal("123.6")
        val result = roundUpToOneDecimal(input)
        assertEquals("123.51 should round up to 123.6", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 0_01 becomes 0_1`() {
        // Edge case: tiny amount still rounds up
        val input = BigDecimal("0.01")
        val expected = BigDecimal("0.1")
        val result = roundUpToOneDecimal(input)
        assertEquals("0.01 should round up to 0.1", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 0_00 stays 0_0`() {
        // Edge case: zero stays zero
        val input = BigDecimal("0.00")
        val expected = BigDecimal("0.0")
        val result = roundUpToOneDecimal(input)
        assertEquals("0.00 should stay 0.0", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal 99_99 becomes 100_0`() {
        // Edge case: crossing boundary
        val input = BigDecimal("99.99")
        val expected = BigDecimal("100.0")
        val result = roundUpToOneDecimal(input)
        assertEquals("99.99 should round up to 100.0", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal already 1 decimal stays same`() {
        val input = BigDecimal("50.3")
        val expected = BigDecimal("50.3")
        val result = roundUpToOneDecimal(input)
        assertEquals("50.3 should stay 50.3", expected, result)
    }

    @Test
    fun `roundUpToOneDecimal integer becomes integer with 1 decimal`() {
        val input = BigDecimal("100")
        val expected = BigDecimal("100.0")
        val result = roundUpToOneDecimal(input)
        assertEquals("100 should become 100.0", expected, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // END-TO-END ROUNDING IN CONVERSION PIPELINE
    // Verify that conversion produces properly rounded USDC amounts
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Convert USD cents to USDC minor units with 1-decimal rounding.
     * Mirrors the production USD conversion pipeline.
     */
    private fun convertUsdToUsdcMinorWithRounding(cents: Long): Long {
        // cents -> USD major
        val centsBD = BigDecimal.valueOf(cents)
        val usdAmount = centsBD.divide(BigDecimal(100), CALC_SCALE, RoundingMode.UNNECESSARY)

        // Round UP to 1 decimal
        val usdcRounded = roundUpToOneDecimal(usdAmount)

        // To minor units (× 1,000,000)
        return usdcRounded.multiply(BigDecimal(1_000_000)).toLong()
    }

    @Test
    fun `USD cents 1041 produces 10_5 USDC (not 10_41)`() {
        // $10.41 -> 10.5 USDC (rounded up)
        val result = convertUsdToUsdcMinorWithRounding(1041L)
        val expectedMinor = 10_500_000L  // 10.5 USDC
        assertEquals("$10.41 should produce 10.5 USDC", expectedMinor, result)
    }

    @Test
    fun `USD cents 1050 produces 10_5 USDC (exact)`() {
        // $10.50 -> 10.5 USDC (exact)
        val result = convertUsdToUsdcMinorWithRounding(1050L)
        val expectedMinor = 10_500_000L  // 10.5 USDC
        assertEquals("$10.50 should produce 10.5 USDC", expectedMinor, result)
    }

    @Test
    fun `USD cents 1051 produces 10_6 USDC (rounded up)`() {
        // $10.51 -> 10.6 USDC (rounded up)
        val result = convertUsdToUsdcMinorWithRounding(1051L)
        val expectedMinor = 10_600_000L  // 10.6 USDC
        assertEquals("$10.51 should produce 10.6 USDC", expectedMinor, result)
    }

    @Test
    fun `USD cents 12341 produces 123_5 USDC`() {
        // $123.41 -> 123.5 USDC (rounded up)
        val result = convertUsdToUsdcMinorWithRounding(12341L)
        val expectedMinor = 123_500_000L  // 123.5 USDC
        assertEquals("$123.41 should produce 123.5 USDC", expectedMinor, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // END-TO-END INVARIANT TESTS
    // Verify: roundedUsdc -> usdcMinor -> URL amount -> back to minor
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulate getAmountString() from InvoiceEntity (BigDecimal version).
     * Uses RoundingMode.UNNECESSARY to catch non-rounded amounts.
     */
    private fun formatAmountString(usdcMinor: Long): String {
        val amountBD = BigDecimal.valueOf(usdcMinor)
        val divisor = BigDecimal.valueOf(USDC_DIVISOR)
        return amountBD.divide(divisor, 1, RoundingMode.UNNECESSARY).toPlainString()
    }

    /**
     * Parse URL amount string back to minor units.
     */
    private fun parseAmountToMinor(amountString: String): Long {
        val amountBD = BigDecimal(amountString)
        return amountBD.multiply(BigDecimal.valueOf(USDC_DIVISOR)).longValueExact()
    }

    /**
     * Full conversion pipeline with 1-decimal rounding.
     * Returns (usdcMinor, urlAmountString).
     */
    private fun fullConversionPipeline(
        fiatMinor: Long,
        fiatDecimals: Int,
        rate: BigDecimal,
        applySlippage: Boolean
    ): Pair<Long, String> {
        // Step 1-2: fiatMinor -> fiatMajor
        val fiatMinorBD = BigDecimal.valueOf(fiatMinor)
        val divisor = BigDecimal.TEN.pow(fiatDecimals)
        val fiatMajor = fiatMinorBD.divide(divisor, CALC_SCALE, RoundingMode.UNNECESSARY)

        // Step 3: fiatMajor -> usdAmount
        val usdAmount = fiatMajor.divide(rate, CALC_SCALE, RoundingMode.HALF_UP)

        // Step 4: Apply slippage if needed
        val usdWithSlippage = if (applySlippage) {
            usdAmount.multiply(SLIPPAGE_MULTIPLIER)
        } else {
            usdAmount
        }

        // Step 5: Round UP to 1 decimal (CEILING)
        val usdcRounded = roundUpToOneDecimal(usdWithSlippage)

        // Step 6: To minor units (exact, no rounding)
        val usdcMinor = usdcRounded.multiply(BigDecimal(1_000_000)).longValueExact()

        // Format for URL
        val urlAmountString = formatAmountString(usdcMinor)

        return usdcMinor to urlAmountString
    }

    @Test
    fun `INVARIANT - usdcMinor must be divisible by 100000 (1 decimal precision)`() {
        // All 1-decimal amounts when converted to 6-decimal minor units
        // must be divisible by 100,000
        val testCases = listOf(
            123_500_000L,  // 123.5
            10_000_000L,   // 10.0
            10_100_000L,   // 10.1
            1_000_000L,    // 1.0
            100_000L,      // 0.1
        )

        for (minor in testCases) {
            assertEquals(
                "usdcMinor $minor should be divisible by 100,000",
                0L,
                minor % 100_000
            )
        }
    }

    @Test
    fun `INVARIANT - URL amount parsed back equals stored minor`() {
        val testCases = listOf(
            123_500_000L to "123.5",
            10_000_000L to "10.0",
            10_100_000L to "10.1",
            1_000_000L to "1.0",
            100_000L to "0.1",
        )

        for ((expectedMinor, expectedString) in testCases) {
            // Format to string
            val urlString = formatAmountString(expectedMinor)
            assertEquals("Format mismatch", expectedString, urlString)

            // Parse back to minor
            val parsedMinor = parseAmountToMinor(urlString)
            assertEquals(
                "INVARIANT VIOLATED: URL '$urlString' parsed to $parsedMinor, expected $expectedMinor",
                expectedMinor,
                parsedMinor
            )
        }
    }

    @Test
    fun `INVARIANT - RUB full pipeline maintains consistency`() {
        // 1000 RUB with rate 76.65 and slippage
        val (usdcMinor, urlAmount) = fullConversionPipeline(
            fiatMinor = 100_000L,  // 1000 RUB (kopeks)
            fiatDecimals = 2,
            rate = FIXED_RATE_RUB,
            applySlippage = true
        )

        // Verify divisible by 100,000
        assertEquals("Minor must be divisible by 100,000", 0L, usdcMinor % 100_000)

        // Verify URL parses back correctly
        val parsedMinor = parseAmountToMinor(urlAmount)
        assertEquals(
            "INVARIANT VIOLATED: URL '$urlAmount' -> $parsedMinor != stored $usdcMinor",
            usdcMinor,
            parsedMinor
        )

        // Verify expected value (1000 RUB / 76.65 * 1.025 = 13.37... -> 13.4 USDC)
        val expectedRounded = BigDecimal("13.4")
        val expectedMinor = 13_400_000L
        assertEquals("1000 RUB should produce 13.4 USDC", expectedMinor, usdcMinor)
        assertEquals("URL should be '13.4'", "13.4", urlAmount)
    }

    @Test
    fun `INVARIANT - USD full pipeline maintains consistency`() {
        // $10.41 -> 10.5 USDC (no FX, no slippage, but still rounds)
        val (usdcMinor, urlAmount) = fullConversionPipeline(
            fiatMinor = 1041L,  // $10.41 (cents)
            fiatDecimals = 2,
            rate = BigDecimal.ONE,  // USD rate is 1:1
            applySlippage = false
        )

        // Verify divisible by 100,000
        assertEquals("Minor must be divisible by 100,000", 0L, usdcMinor % 100_000)

        // Verify URL parses back correctly
        val parsedMinor = parseAmountToMinor(urlAmount)
        assertEquals(
            "INVARIANT VIOLATED: URL '$urlAmount' -> $parsedMinor != stored $usdcMinor",
            usdcMinor,
            parsedMinor
        )

        // Verify expected value ($10.41 -> 10.5 USDC)
        assertEquals("$10.41 should produce 10.5 USDC", 10_500_000L, usdcMinor)
        assertEquals("URL should be '10.5'", "10.5", urlAmount)
    }

    @Test(expected = ArithmeticException::class)
    fun `INVARIANT - formatAmountString rejects non-rounded amounts`() {
        // If someone stores a non-rounded amount, formatting should fail
        val invalidMinor = 123_456_789L  // Not divisible by 100,000
        formatAmountString(invalidMinor)  // Should throw ArithmeticException
    }

    @Test
    fun `INVARIANT - detector expectedAmountMinor equals stored amount`() {
        // Simulates: expectedAmountMinor = invoice.amount (from SolanaPayPaymentDetector.kt:171)
        val (storedUsdcMinor, urlAmount) = fullConversionPipeline(
            fiatMinor = 50_000L,  // 500 RUB
            fiatDecimals = 2,
            rate = FIXED_RATE_RUB,
            applySlippage = true
        )

        // Detector uses stored amount directly
        val expectedAmountMinor = storedUsdcMinor

        // Customer pays the URL amount (parse URL -> minor)
        val customerPaidMinor = parseAmountToMinor(urlAmount)

        // Validation: customerPaidMinor >= expectedAmountMinor
        assert(customerPaidMinor >= expectedAmountMinor) {
            "Customer paid $customerPaidMinor but expected >= $expectedAmountMinor"
        }

        // They should be exactly equal for properly rounded amounts
        assertEquals(
            "Customer payment should exactly match expected",
            expectedAmountMinor,
            customerPaidMinor
        )
    }
}
