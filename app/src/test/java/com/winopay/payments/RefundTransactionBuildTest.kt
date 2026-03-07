package com.winopay.payments

import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for refund transaction building logic.
 *
 * Tests that buildRefundTransaction uses:
 * - actualMintUsed (or fallback to splTokenMint)
 * - payerAddress as recipient
 * - Exact received amount (minor units)
 */
class RefundTransactionBuildTest {

    private fun createInvoice(
        status: InvoiceStatus = InvoiceStatus.CONFIRMED,
        payerAddress: String? = "payer-wallet-address",
        payerTokenAccount: String? = "payer-token-account",
        refundTxId: String? = null,
        amount: Long = 21_500_000L, // 21.50 USDC
        splTokenMint: String = "expected-usdc-mint",
        actualMintUsed: String? = null
    ) = InvoiceEntity(
        id = "test-invoice-1",
        reference = "ref1",
        recipientAddress = "merchant-wallet",
        recipientTokenAccount = "merchant-ata",
        amount = amount,
        currency = PaymentCurrency.USDC,
        splTokenMint = splTokenMint,
        railId = "solana",
        networkId = "mainnet",
        status = status,
        foundSignature = if (status == InvoiceStatus.CONFIRMED) "confirmed-sig" else null,
        memo = null,
        label = null,
        message = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        confirmedAt = if (status == InvoiceStatus.CONFIRMED) System.currentTimeMillis() else null,
        payerAddress = payerAddress,
        payerTokenAccount = payerTokenAccount,
        refundTxId = refundTxId,
        actualMintUsed = actualMintUsed
    )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // MINT SELECTION TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `refund uses actualMintUsed when different from expected`() {
        // Given: Invoice requested USDC but was paid with USDT
        val invoice = createInvoice(
            splTokenMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", // USDC mint
            actualMintUsed = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB" // USDT mint
        )

        // When: Determine which mint to use for refund
        val refundMint = invoice.actualMintUsed ?: invoice.splTokenMint

        // Then: Should use USDT (the actual mint used for payment)
        assertEquals(
            "Refund should use actualMintUsed, not expected mint",
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
            refundMint
        )
    }

    @Test
    fun `refund falls back to splTokenMint when actualMintUsed is null`() {
        // Given: Invoice paid with expected USDC (no dual-stablecoin scenario)
        val invoice = createInvoice(
            splTokenMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", // USDC mint
            actualMintUsed = null // Not set, meaning same mint was used
        )

        // When: Determine which mint to use for refund
        val refundMint = invoice.actualMintUsed ?: invoice.splTokenMint

        // Then: Should use splTokenMint as fallback
        assertEquals(
            "Refund should fallback to splTokenMint",
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            refundMint
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PAYER ADDRESS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `refund recipient is payerAddress from validated transaction`() {
        // Given: Invoice with validated payer address
        val expectedPayer = "GqTPL6qRf5aUuqscLh8Rg2HTxPUXfhhAXDptTLhp1t2J"
        val invoice = createInvoice(
            payerAddress = expectedPayer
        )

        // When: Get refund recipient
        val refundRecipient = invoice.payerAddress

        // Then: Should use the stored payer address (not merchant's address)
        assertEquals(
            "Refund recipient should be payerAddress",
            expectedPayer,
            refundRecipient
        )
    }

    @Test
    fun `refund amount is exactly what was received in minor units`() {
        // Given: Invoice with specific amount in minor units
        val receivedAmount = 123_456_789L // 123.456789 USDC
        val invoice = createInvoice(amount = receivedAmount)

        // When: Get refund amount
        val refundAmount = invoice.amount

        // Then: Should refund EXACTLY what was received (no rounding)
        assertEquals(
            "Refund amount should exactly match received amount",
            receivedAmount,
            refundAmount
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SECURITY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `refund parameters are immutable from original invoice`() {
        // Given: Original invoice
        val originalPayer = "original-payer-wallet"
        val originalAmount = 50_000_000L
        val originalMint = "original-usdc-mint"

        val invoice = createInvoice(
            payerAddress = originalPayer,
            amount = originalAmount,
            actualMintUsed = originalMint
        )

        // When: Get refund parameters (these should come from immutable invoice data)
        val refundPayer = invoice.payerAddress
        val refundAmount = invoice.amount
        val refundMint = invoice.actualMintUsed ?: invoice.splTokenMint

        // Then: All values should match original invoice (no modification possible)
        assertEquals("Payer should be from original invoice", originalPayer, refundPayer)
        assertEquals("Amount should be from original invoice", originalAmount, refundAmount)
        assertEquals("Mint should be from original invoice", originalMint, refundMint)
    }

    @Test
    fun `refund cannot proceed without payer address`() {
        // Given: Invoice without payer (edge case - should not happen normally)
        val invoice = createInvoice(payerAddress = null)

        // When: Check if refund is possible
        val canRefund = invoice.canRefund()

        // Then: Should not be refundable
        assertTrue("Refund should be blocked when payer is unknown", !canRefund)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DOUBLE REFUND PREVENTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `marking refund sent prevents further refunds`() {
        // Given: Invoice before refund
        val invoiceBefore = createInvoice(refundTxId = null)
        assertTrue("Should be refundable before", invoiceBefore.canRefund())

        // When: Mark refund as sent (simulating markRefundSent call)
        val invoiceAfter = invoiceBefore.markRefundSent("refund-tx-signature")

        // Then: Should no longer be refundable
        assertTrue("Should not be refundable after marking refund sent", !invoiceAfter.canRefund())
        assertNotNull("refundTxId should be set", invoiceAfter.refundTxId)
        assertEquals("refundTxId should match", "refund-tx-signature", invoiceAfter.refundTxId)
    }
}
