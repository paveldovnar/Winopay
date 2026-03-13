package com.winopay.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Unit tests for currency pipeline fixes (2026-03-13).
 *
 * Covers three critical fixes:
 * 1. Merchant protection guard removal (was comparing USD with local currency)
 * 2. Double precision fix in fiat→minor conversion
 * 3. Input validation (fiatAmountMinor > 0)
 *
 * All tests mirror the production pipeline without Android dependencies.
 */
class CurrencyPipelineFixTest {

    companion object {
        private const val CALC_SCALE = 8
        private const val STABLECOIN_DISPLAY_SCALE = 2
        private val SLIPPAGE_MULTIPLIER = BigDecimal("1.025")
    }

    /**
     * Round UP (CEILING) to 2 decimal places.
     * Mirrors CurrencyConverter.roundUpToTwoDecimals().
     */
    private fun roundUpToTwoDecimals(amount: BigDecimal): BigDecimal {
        return amount.setScale(STABLECOIN_DISPLAY_SCALE, RoundingMode.CEILING)
    }

    /**
     * Full non-USD conversion pipeline — AFTER the fix (no merchant protection guard).
     * Mirrors CurrencyConverter.convertToUsdcMinorWithMetadata() for non-USD path.
     */
    private fun convertFiatToUsdcMinor(
        fiatAmountMinor: Long,
        fiatDecimals: Int,
        rate: BigDecimal
    ): Long {
        require(fiatAmountMinor > 0) { "Amount must be positive, got: $fiatAmountMinor" }

        // Step 1-2: fiatMinor → fiatMajor
        val fiatMinorBD = BigDecimal.valueOf(fiatAmountMinor)
        val divisor = BigDecimal.TEN.pow(fiatDecimals)
        val fiatMajor = fiatMinorBD.divide(divisor, CALC_SCALE, RoundingMode.UNNECESSARY)

        // Step 3: usdAmount = fiatMajor / rate
        val usdAmount = fiatMajor.divide(rate, CALC_SCALE, RoundingMode.HALF_UP)

        // Step 4: Apply 2.5% slippage
        val usdWithSlippage = usdAmount.multiply(SLIPPAGE_MULTIPLIER)

        // Step 5: Round UP to 2 decimals (CEILING)
        val usdcRounded = roundUpToTwoDecimals(usdWithSlippage)

        // FIX: No merchant protection guard — usdcRounded IS the final value
        val finalUsdc = usdcRounded

        // Step 6: To minor units (× 1_000_000)
        return finalUsdc.multiply(BigDecimal(1_000_000)).longValueExact()
    }

    /**
     * Simulate the fixed PosManager fiat→minor conversion using BigDecimal.
     * Mirrors the production fix: BigDecimal(inputAmount.toString()).movePointRight(2)
     */
    private fun fiatInputToMinor(inputAmount: Double, fiatDecimals: Int = 2): Long {
        return BigDecimal(inputAmount.toString())
            .movePointRight(fiatDecimals)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIX #1: MERCHANT PROTECTION GUARD REMOVAL
    // The old guard compared usdcRounded (USD) with fiatMajor (local currency).
    // For weak currencies (THB, RUB, etc.) this caused massive overcharges.
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `THB 1000 converts to approx 28_88 USDC not 1000 USDC`() {
        // Rate: 1 USD = 35.5 THB
        // 1000 THB = 100000 minor units
        // USD = 1000 / 35.5 = 28.16901408...
        // With slippage: 28.16901408 * 1.025 = 28.87323943...
        // CEILING(2 decimals) = 28.88
        // USDC minor = 28.88 * 1_000_000 = 28_880_000
        val rate = BigDecimal("35.5")
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 100_000L, // 1000 THB in satang
            fiatDecimals = 2,
            rate = rate
        )

        assertEquals("1000 THB should produce 28.88 USDC", 28_880_000L, result)

        // Critical regression check: must NOT be 1000 USDC (the old bug)
        assertTrue(
            "1000 THB must NOT produce 1000 USDC (old guard bug)",
            result < 100_000_000L // 100 USDC — any sane result for 1000 THB is well below this
        )
    }

    @Test
    fun `RUB 5000 converts correctly not to 5000 USDC`() {
        // Rate: 1 USD = 92.0 RUB
        // 5000 RUB = 500000 minor units
        // USD = 5000 / 92 = 54.34782609...
        // With slippage: 54.34782609 * 1.025 = 55.70652174...
        // CEILING(2 decimals) = 55.71
        // USDC minor = 55_710_000
        val rate = BigDecimal("92.0")
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 500_000L, // 5000 RUB in kopeks
            fiatDecimals = 2,
            rate = rate
        )

        assertEquals("5000 RUB should produce 55.71 USDC", 55_710_000L, result)

        // Must NOT be anywhere near 5000 USDC
        assertTrue(
            "5000 RUB must NOT produce 5000 USDC",
            result < 500_000_000L
        )
    }

    @Test
    fun `INR 2000 converts correctly not to 2000 USDC`() {
        // Rate: 1 USD = 83.0 INR
        // 2000 INR = 200000 minor units (paise)
        // USD = 2000 / 83 = 24.09638554...
        // With slippage: 24.09638554 * 1.025 = 24.69879518...
        // CEILING(2 decimals) = 24.70
        // USDC minor = 24_700_000
        val rate = BigDecimal("83.0")
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 200_000L, // 2000 INR in paise
            fiatDecimals = 2,
            rate = rate
        )

        assertEquals("2000 INR should produce 24.70 USDC", 24_700_000L, result)
    }

    @Test
    fun `EUR 100 converts correctly - strong currency unaffected by fix`() {
        // Rate: 1 USD = 0.92 EUR (EUR is stronger than USD)
        // 100 EUR = 10000 minor units
        // USD = 100 / 0.92 = 108.69565217...
        // With slippage: 108.69565217 * 1.025 = 111.41304348...
        // CEILING(2 decimals) = 111.42
        // USDC minor = 111_420_000
        val rate = BigDecimal("0.92")
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 10_000L, // 100 EUR in cents
            fiatDecimals = 2,
            rate = rate
        )

        assertEquals("100 EUR should produce 111.42 USDC", 111_420_000L, result)
    }

    @Test
    fun `GBP 100 converts correctly - strong currency unaffected by fix`() {
        // Rate: 1 USD = 0.79 GBP
        // 100 GBP = 10000 minor units
        // USD = 100 / 0.79 = 126.58227848...
        // With slippage: 126.58227848 * 1.025 = 129.74683544...
        // CEILING(2 decimals) = 129.75
        // USDC minor = 129_750_000
        val rate = BigDecimal("0.79")
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 10_000L,
            fiatDecimals = 2,
            rate = rate
        )

        assertEquals("100 GBP should produce 129.75 USDC", 129_750_000L, result)
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIX #2: DOUBLE PRECISION FIX (PosManager fiat→minor)
    // Old: (inputAmount * 100).toLong() — loses cents on ~15% of inputs
    // New: BigDecimal(inputAmount.toString()).movePointRight(2).HALF_UP
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `19_99 converts to 1999 minor units not 1998`() {
        // This is the canonical Double precision bug:
        // 19.99 * 100 = 1998.9999...98 in IEEE-754
        // .toLong() truncates to 1998 (WRONG)
        // BigDecimal fix produces 1999 (CORRECT)
        val result = fiatInputToMinor(19.99)
        assertEquals("19.99 must produce 1999 minor units", 1999L, result)
    }

    @Test
    fun `33_33 converts to 3333 minor units not 3332`() {
        // 33.33 * 100 = 3332.9999...96 in IEEE-754
        val result = fiatInputToMinor(33.33)
        assertEquals("33.33 must produce 3333 minor units", 3333L, result)
    }

    @Test
    fun `10_07 converts to 1007 minor units not 1006`() {
        // 10.07 * 100 = 1006.9999...99 in IEEE-754
        val result = fiatInputToMinor(10.07)
        assertEquals("10.07 must produce 1007 minor units", 1007L, result)
    }

    @Test
    fun `1_10 converts to 110 minor units`() {
        // 1.10 * 100 = 110.0 (exact in IEEE-754)
        val result = fiatInputToMinor(1.10)
        assertEquals("1.10 must produce 110 minor units", 110L, result)
    }

    @Test
    fun `0_01 converts to 1 minor unit`() {
        val result = fiatInputToMinor(0.01)
        assertEquals("0.01 must produce 1 minor unit", 1L, result)
    }

    @Test
    fun `99_99 converts to 9999 minor units`() {
        val result = fiatInputToMinor(99.99)
        assertEquals("99.99 must produce 9999 minor units", 9999L, result)
    }

    @Test
    fun `1000_00 converts to 100000 minor units`() {
        val result = fiatInputToMinor(1000.0)
        assertEquals("1000.00 must produce 100000 minor units", 100_000L, result)
    }

    @Test
    fun `old Double multiplication produces wrong result for 19_99`() {
        // Demonstrate the bug we fixed: Double arithmetic truncation
        val oldResult = (19.99 * 100).toLong()
        assertEquals("Old method produces 1998 (the bug)", 1998L, oldResult)

        // New method produces correct result
        val newResult = fiatInputToMinor(19.99)
        assertEquals("New method produces 1999 (correct)", 1999L, newResult)
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIX #3: INPUT VALIDATION (fiatAmountMinor > 0)
    // ═══════════════════════════════════════════════════════════════════

    @Test(expected = IllegalArgumentException::class)
    fun `convertFiatToUsdcMinor rejects zero amount`() {
        convertFiatToUsdcMinor(
            fiatAmountMinor = 0L,
            fiatDecimals = 2,
            rate = BigDecimal("35.5")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `convertFiatToUsdcMinor rejects negative amount`() {
        convertFiatToUsdcMinor(
            fiatAmountMinor = -100L,
            fiatDecimals = 2,
            rate = BigDecimal("35.5")
        )
    }

    @Test
    fun `convertFiatToUsdcMinor accepts 1 minor unit`() {
        // Smallest valid amount: 1 satang/kopek/cent
        val result = convertFiatToUsdcMinor(
            fiatAmountMinor = 1L,
            fiatDecimals = 2,
            rate = BigDecimal("35.5")
        )
        assertTrue("1 minor unit should produce a positive USDC amount", result > 0)
    }

    // ═══════════════════════════════════════════════════════════════════
    // END-TO-END: Full pipeline with fixes applied
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `full pipeline - THB 1000 from user input to USDC minor`() {
        // Step 1: User types 1000.0 on numpad (Double)
        val inputAmount = 1000.0

        // Step 2: Convert to minor units (the fix)
        val fiatAmountMinor = fiatInputToMinor(inputAmount)
        assertEquals("1000 THB → 100000 satang", 100_000L, fiatAmountMinor)

        // Step 3: Convert to USDC (no guard)
        val rate = BigDecimal("35.5")
        val usdcMinor = convertFiatToUsdcMinor(fiatAmountMinor, 2, rate)
        assertEquals("100000 satang → 28.88 USDC", 28_880_000L, usdcMinor)

        // Verify display amount
        val displayUsdc = usdcMinor.toDouble() / 1_000_000.0
        assertEquals("Display amount should be ~28.88", 28.88, displayUsdc, 0.001)
    }

    @Test
    fun `full pipeline - USD 19_99 from user input to USDC minor`() {
        // Step 1: User types 19.99 on numpad (Double)
        val inputAmount = 19.99

        // Step 2: Convert to minor units (the fix — no more 1998 bug)
        val fiatAmountMinor = fiatInputToMinor(inputAmount)
        assertEquals("19.99 USD → 1999 cents", 1999L, fiatAmountMinor)

        // Step 3: USD path — 1:1 with USDC
        // 1999 cents = $19.99 = 19.99 USDC = 19_990_000 minor
        val usdcMinor = fiatAmountMinor * 10_000L // USD fast path
        assertEquals("1999 cents → 19_990_000 USDC minor", 19_990_000L, usdcMinor)
    }
}
