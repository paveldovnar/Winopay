package com.winopay.data.local

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for InvoiceEntity polling address selection.
 *
 * CRITICAL: These tests verify that:
 * - SOL invoices poll the wallet address
 * - SPL invoices poll the ATA (token account)
 *
 * If these tests fail, payment detection will be broken for SPL tokens.
 */
class InvoiceEntityPollingTest {

    companion object {
        const val TEST_WALLET = "7EYnhQoR9YM3N7UoaKRoA44Uy8JeaZV3qyouov87awMs"
        const val TEST_ATA = "4wBqpZM9k3yqkKAaFq5XCFqTqL4QDfXcxPLnAKPkA1gg"  // Example ATA
        const val TEST_USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
        const val TEST_REFERENCE = "2xGbT5J9n2qDvZ7qRu6UqsPmjKKPgEJaVnJnZvV7vYzM"
    }

    // ━━━━━━━━━━ SOL INVOICE TESTS ━━━━━━━━━━

    @Test
    fun `SOL invoice should poll wallet address`() {
        val invoice = createSolInvoice()

        val pollingAddress = invoice.getPollingAddress()

        assertEquals(
            "SOL invoice should poll wallet directly",
            TEST_WALLET,
            pollingAddress
        )
    }

    @Test
    fun `SOL invoice should NOT require ATA polling`() {
        val invoice = createSolInvoice()

        assertFalse(
            "SOL invoice should not require ATA polling",
            invoice.requiresAtaPolling()
        )
    }

    @Test
    fun `SOL invoice can have null recipientTokenAccount`() {
        val invoice = createSolInvoice()

        assertNull(
            "SOL invoice should have null recipientTokenAccount",
            invoice.recipientTokenAccount
        )
    }

    // ━━━━━━━━━━ SPL (USDC) INVOICE TESTS ━━━━━━━━━━

    @Test
    fun `USDC invoice should poll ATA address`() {
        val invoice = createUsdcInvoice()

        val pollingAddress = invoice.getPollingAddress()

        assertEquals(
            "USDC invoice should poll ATA, not wallet",
            TEST_ATA,
            pollingAddress
        )
    }

    @Test
    fun `USDC invoice should require ATA polling`() {
        val invoice = createUsdcInvoice()

        assertTrue(
            "USDC invoice should require ATA polling",
            invoice.requiresAtaPolling()
        )
    }

    @Test
    fun `USDC invoice must have recipientTokenAccount`() {
        val invoice = createUsdcInvoice()

        assertNotNull(
            "USDC invoice must have recipientTokenAccount",
            invoice.recipientTokenAccount
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `USDC invoice without ATA should throw`() {
        // Create USDC invoice with null ATA (invalid state)
        val invalidInvoice = InvoiceEntity.create(
            id = "test-invalid",
            reference = TEST_REFERENCE,
            recipientAddress = TEST_WALLET,
            recipientTokenAccount = null,  // Missing ATA!
            amount = 1_000_000,
            currency = PaymentCurrency.USDC,
            splTokenMint = TEST_USDC_MINT
        )

        // This should throw because USDC requires ATA
        invalidInvoice.getPollingAddress()
    }

    // ━━━━━━━━━━ ADDRESS DISTINCTION TESTS ━━━━━━━━━━

    @Test
    fun `polling address should differ between SOL and USDC`() {
        val solInvoice = createSolInvoice()
        val usdcInvoice = createUsdcInvoice()

        val solPolling = solInvoice.getPollingAddress()
        val usdcPolling = usdcInvoice.getPollingAddress()

        assertNotEquals(
            "SOL and USDC should poll different addresses",
            solPolling,
            usdcPolling
        )
    }

    @Test
    fun `USDC polling address should not be wallet`() {
        val invoice = createUsdcInvoice()

        val pollingAddress = invoice.getPollingAddress()

        assertNotEquals(
            "USDC polling should NOT be the wallet (must be ATA)",
            invoice.recipientAddress,
            pollingAddress
        )
    }

    // ━━━━━━━━━━ HELPER FUNCTIONS ━━━━━━━━━━

    private fun createSolInvoice(): InvoiceEntity {
        return InvoiceEntity.create(
            id = "test-sol-001",
            reference = TEST_REFERENCE,
            recipientAddress = TEST_WALLET,
            recipientTokenAccount = null,  // SOL doesn't use ATA
            amount = 1_000_000_000,  // 1 SOL in lamports
            currency = PaymentCurrency.SOL,
            splTokenMint = null
        )
    }

    private fun createUsdcInvoice(): InvoiceEntity {
        return InvoiceEntity.create(
            id = "test-usdc-001",
            reference = TEST_REFERENCE,
            recipientAddress = TEST_WALLET,
            recipientTokenAccount = TEST_ATA,  // USDC requires ATA
            amount = 1_000_000,  // 1 USDC in minor units (6 decimals)
            currency = PaymentCurrency.USDC,
            splTokenMint = TEST_USDC_MINT
        )
    }
}
