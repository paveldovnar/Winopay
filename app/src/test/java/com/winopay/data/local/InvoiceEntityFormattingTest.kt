package com.winopay.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for InvoiceEntity FX formatting methods.
 *
 * Tests local currency display with historical FX rates for transaction history.
 */
class InvoiceEntityFormattingTest {

    /**
     * Test formatting FIAT amount with stored FX data.
     * This simulates a confirmed transaction that was paid when merchant's currency was RUB.
     */
    @Test
    fun `formatFiatAmount returns correct local currency amount`() {
        // Given: Invoice with RUB fiat data
        // USDC amount: 21.50 USD (21500000 minor units)
        // FX rate: 76.50 RUB per USD
        // Expected FIAT: 21.50 * 76.50 = 1644.75 RUB = 164475 kopeks
        val invoice = InvoiceEntity(
            id = "test-1",
            reference = "ref1",
            recipientAddress = "addr1",
            recipientTokenAccount = "ata1",
            amount = 21_500_000L, // 21.50 USDC (6 decimals)
            currency = PaymentCurrency.USDC,
            splTokenMint = "usdc-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig1",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis(),
            // FX data from payment time
            fiatAmountMinor = 164475L, // 1644.75 RUB (2 decimals)
            fiatCurrency = "RUB",
            fiatDecimals = 2,
            rateUsed = "76.50",
            rateProvider = "Frankfurter"
        )

        // When
        val formatted = invoice.formatFiatAmount()

        // Then: Should show 1644.75 RUB
        assertEquals("1644.75", formatted)
    }

    /**
     * Test formatting stablecoin amount (USD equivalent).
     * USDC/USDT are treated as 1:1 with USD.
     */
    @Test
    fun `formatStablecoinAmount returns correct USD amount`() {
        // Given: Invoice with USDC payment
        val invoice = InvoiceEntity(
            id = "test-2",
            reference = "ref2",
            recipientAddress = "addr2",
            recipientTokenAccount = "ata2",
            amount = 21_500_000L, // 21.50 USDC (6 decimals)
            currency = PaymentCurrency.USDC,
            splTokenMint = "usdc-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig2",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis()
        )

        // When
        val formatted = invoice.formatStablecoinAmount()

        // Then: Should show 21.50 USD
        assertEquals("21.50", formatted)
    }

    /**
     * Test that invoice without FX data returns null for FIAT formatting.
     * This triggers the USD fallback in the UI.
     */
    @Test
    fun `formatFiatAmount returns null when no FX data`() {
        // Given: Invoice without FX data (old transaction before FX feature)
        val invoice = InvoiceEntity(
            id = "test-3",
            reference = "ref3",
            recipientAddress = "addr3",
            recipientTokenAccount = "ata3",
            amount = 10_000_000L, // 10.00 USDC
            currency = PaymentCurrency.USDC,
            splTokenMint = "usdc-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig3",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis()
            // No FX fields
        )

        // When
        val formatted = invoice.formatFiatAmount()

        // Then: Should return null (triggers USD fallback)
        assertNull(formatted)
    }

    /**
     * Test rate display formatting.
     */
    @Test
    fun `formatRateDisplay returns correctly formatted rate`() {
        // Given: Invoice with Thai Baht FX data
        val invoice = InvoiceEntity(
            id = "test-4",
            reference = "ref4",
            recipientAddress = "addr4",
            recipientTokenAccount = "ata4",
            amount = 10_000_000L, // 10.00 USDC
            currency = PaymentCurrency.USDC,
            splTokenMint = "usdc-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig4",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis(),
            // FX data
            fiatAmountMinor = 35500L, // 355.00 THB
            fiatCurrency = "THB",
            fiatDecimals = 2,
            rateUsed = "35.50"
        )

        // When
        val formatted = invoice.formatRateDisplay()

        // Then: Should show "1 USD = 35.50 THB"
        assertEquals("1 USD = 35.50 THB", formatted)
    }

    /**
     * Test BigDecimal precision for FX calculations.
     */
    @Test
    fun `getFiatDisplayAmount uses BigDecimal for precision`() {
        // Given: Invoice with precise FX data
        val invoice = InvoiceEntity(
            id = "test-5",
            reference = "ref5",
            recipientAddress = "addr5",
            recipientTokenAccount = "ata5",
            amount = 12_345_678L, // 12.345678 USDC
            currency = PaymentCurrency.USDC,
            splTokenMint = "usdc-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig5",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis(),
            // FX data: 12.345678 * 76.65 = 946.30 RUB
            fiatAmountMinor = 94630L, // 946.30 RUB
            fiatCurrency = "RUB",
            fiatDecimals = 2,
            rateUsed = "76.65"
        )

        // When
        val amount = invoice.getFiatDisplayAmount()

        // Then: Should be precise BigDecimal 946.30
        // Compare using compareTo to ignore trailing zeros
        assertEquals(0, BigDecimal("946.30").compareTo(amount))
    }

    /**
     * Test USDT formatting (same as USDC, 6 decimals).
     */
    @Test
    fun `formatStablecoinAmount works for USDT`() {
        // Given: Invoice with USDT payment
        val invoice = InvoiceEntity(
            id = "test-6",
            reference = "ref6",
            recipientAddress = "addr6",
            recipientTokenAccount = "ata6",
            amount = 99_990_000L, // 99.99 USDT (6 decimals)
            currency = PaymentCurrency.USDT,
            splTokenMint = "usdt-mint",
            railId = "solana",
            networkId = "mainnet",
            status = InvoiceStatus.CONFIRMED,
            foundSignature = "sig6",
            memo = null,
            label = null,
            message = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = System.currentTimeMillis()
        )

        // When
        val formatted = invoice.formatStablecoinAmount()

        // Then: Should show 99.99 USD (USDT = USD 1:1)
        assertEquals("99.99", formatted)
    }
}
