package com.winopay.payments

import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for refund eligibility logic.
 *
 * Tests the strict eligibility rules:
 * - invoice.status == CONFIRMED
 * - invoice.refundTxId == null
 * - payerAddress != null
 * - amount > 0
 */
class RefundEligibilityTest {

    private fun createInvoice(
        status: InvoiceStatus = InvoiceStatus.CONFIRMED,
        payerAddress: String? = "payer-wallet-address",
        payerTokenAccount: String? = "payer-token-account",
        refundTxId: String? = null,
        amount: Long = 21_500_000L, // 21.50 USDC
        actualMintUsed: String? = null
    ) = InvoiceEntity(
        id = "test-invoice-1",
        reference = "ref1",
        recipientAddress = "merchant-wallet",
        recipientTokenAccount = "merchant-ata",
        amount = amount,
        currency = PaymentCurrency.USDC,
        splTokenMint = "usdc-mint-address",
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
    // ELIGIBILITY: POSITIVE CASES
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `canRefund returns true for confirmed invoice with payer address`() {
        // Given: A confirmed invoice with payer info and no existing refund
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = "payer-wallet-address",
            refundTxId = null
        )

        // When/Then: Should be eligible for refund
        assertTrue("Confirmed invoice with payer should be refundable", invoice.canRefund())
    }

    @Test
    fun `canRefund returns true when actualMintUsed differs from expected`() {
        // Given: Invoice paid with USDT instead of USDC (dual stablecoin acceptance)
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = "payer-wallet",
            actualMintUsed = "usdt-mint-address" // Different from splTokenMint
        )

        // When/Then: Should still be refundable
        assertTrue("Invoice with different mint should be refundable", invoice.canRefund())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ELIGIBILITY: NEGATIVE CASES (STATUS)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `canRefund returns false for PENDING invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.PENDING)
        assertFalse("PENDING invoice should not be refundable", invoice.canRefund())
    }

    @Test
    fun `canRefund returns false for CREATED invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.CREATED)
        assertFalse("CREATED invoice should not be refundable", invoice.canRefund())
    }

    @Test
    fun `canRefund returns false for FAILED invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.FAILED)
        assertFalse("FAILED invoice should not be refundable", invoice.canRefund())
    }

    @Test
    fun `canRefund returns false for EXPIRED invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.EXPIRED)
        assertFalse("EXPIRED invoice should not be refundable", invoice.canRefund())
    }

    @Test
    fun `canRefund returns false for CANCELED invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.CANCELED)
        assertFalse("CANCELED invoice should not be refundable", invoice.canRefund())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ELIGIBILITY: NEGATIVE CASES (PAYER INFO)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `canRefund returns false when payerAddress is null`() {
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = null // Missing payer info
        )
        assertFalse("Invoice without payer address should not be refundable", invoice.canRefund())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ELIGIBILITY: NEGATIVE CASES (ALREADY REFUNDED)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `canRefund returns false when refundTxId already exists`() {
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = "payer-wallet",
            refundTxId = "existing-refund-tx-signature" // Already refunded!
        )
        assertFalse("Already refunded invoice should not be refundable again", invoice.canRefund())
    }

    @Test
    fun `canRefund is idempotent - checking twice returns same result`() {
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = "payer-wallet"
        )

        // When: Check eligibility multiple times
        val result1 = invoice.canRefund()
        val result2 = invoice.canRefund()
        val result3 = invoice.canRefund()

        // Then: All results should be identical
        assertTrue("First check should return true", result1)
        assertTrue("Second check should return true", result2)
        assertTrue("Third check should return true", result3)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ELIGIBILITY: EDGE CASES
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `canRefund returns true even with empty payerTokenAccount`() {
        // payerTokenAccount can be null/empty if we can derive it from payerAddress
        val invoice = createInvoice(
            status = InvoiceStatus.CONFIRMED,
            payerAddress = "payer-wallet",
            payerTokenAccount = null // No stored token account
        )
        assertTrue("Invoice should be refundable even without payerTokenAccount", invoice.canRefund())
    }
}
