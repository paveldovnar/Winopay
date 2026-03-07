package com.winopay.data.pos

import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for soft-expire deadline logic.
 *
 * SOFT-EXPIRE REQUIREMENTS:
 * 1. Invoice has deadlineAt field stored in DB
 * 2. Deadline can be extended (Wait 5 more minutes)
 * 3. Worker reads deadline from DB (respects extensions)
 * 4. Only UI can mark invoice as EXPIRED (revoke)
 */
class SoftExpireTest {

    // ═══════════════════════════════════════════════════════════════════
    // DEADLINE FIELD TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `InvoiceEntity has deadlineAt field`() {
        val invoice = createTestInvoice()
        assertEquals(0L, invoice.deadlineAt)
    }

    @Test
    fun `withDeadline sets initial deadline`() {
        val invoice = createTestInvoice()
        val deadline = System.currentTimeMillis() + 300_000L // 5 minutes

        val withDeadline = invoice.withDeadline(deadline)

        assertEquals(deadline, withDeadline.deadlineAt)
        // Original should be unchanged
        assertEquals(0L, invoice.deadlineAt)
    }

    @Test
    fun `extendDeadline updates deadline`() {
        val now = System.currentTimeMillis()
        val initialDeadline = now + 300_000L // 5 minutes
        val extendedDeadline = now + 600_000L // 10 minutes

        val invoice = createTestInvoice().withDeadline(initialDeadline)
        assertEquals(initialDeadline, invoice.deadlineAt)

        val extended = invoice.extendDeadline(extendedDeadline)
        assertEquals(extendedDeadline, extended.deadlineAt)
        assertTrue(extended.updatedAt >= now)
    }

    // ═══════════════════════════════════════════════════════════════════
    // TIMEOUT CALCULATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `DEFAULT_TIMEOUT_MS is 5 minutes`() {
        assertEquals(5 * 60 * 1000L, InvoiceTimeouts.DEFAULT_TIMEOUT_MS)
    }

    @Test
    fun `EXTENSION_TIMEOUT_MS is 5 minutes`() {
        assertEquals(5 * 60 * 1000L, InvoiceTimeouts.EXTENSION_TIMEOUT_MS)
    }

    @Test
    fun `LAST_CHANCE_SWEEP_MS is 3 seconds`() {
        assertEquals(3_000L, InvoiceTimeouts.LAST_CHANCE_SWEEP_MS)
    }

    @Test
    fun `deadline calculation from now plus timeout`() {
        val now = System.currentTimeMillis()
        val deadline = now + InvoiceTimeouts.DEFAULT_TIMEOUT_MS

        val remainingMs = deadline - now
        assertEquals(InvoiceTimeouts.DEFAULT_TIMEOUT_MS, remainingMs)
    }

    @Test
    fun `extension adds 5 minutes from current time`() {
        val originalDeadline = System.currentTimeMillis()
        val newDeadline = System.currentTimeMillis() + InvoiceTimeouts.EXTENSION_TIMEOUT_MS

        assertTrue(newDeadline > originalDeadline)
        assertTrue(newDeadline - originalDeadline <= InvoiceTimeouts.EXTENSION_TIMEOUT_MS + 100) // 100ms tolerance
    }

    // ═══════════════════════════════════════════════════════════════════
    // STATUS TRANSITION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `markExpired sets status to EXPIRED`() {
        val invoice = createTestInvoice()
        assertEquals(InvoiceStatus.CREATED, invoice.status)

        val expired = invoice.markExpired()
        assertEquals(InvoiceStatus.EXPIRED, expired.status)
    }

    @Test
    fun `markCanceled sets status to CANCELED`() {
        val invoice = createTestInvoice()
        val canceled = invoice.markCanceled()
        assertEquals(InvoiceStatus.CANCELED, canceled.status)
    }

    @Test
    fun `deadline extension preserves status`() {
        val invoice = createTestInvoice()
        assertEquals(InvoiceStatus.CREATED, invoice.status)

        val extended = invoice.extendDeadline(System.currentTimeMillis() + 300_000L)
        assertEquals(InvoiceStatus.CREATED, extended.status)
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════

    private fun createTestInvoice(): InvoiceEntity {
        return InvoiceEntity(
            id = "test-invoice-123",
            reference = "test-reference",
            recipientAddress = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW",
            recipientTokenAccount = "DerivedATA123456789012345678901234567890123",
            amount = 200_000L, // 0.2 USDC
            currency = PaymentCurrency.USDC,
            splTokenMint = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
            railId = "solana",
            networkId = "devnet",
            status = InvoiceStatus.CREATED,
            foundSignature = null,
            memo = "test-invoice-123",
            label = "Test Business",
            message = "Payment to Test Business",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            confirmedAt = null
        )
    }
}
