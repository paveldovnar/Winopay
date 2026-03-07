package com.winopay.payments

import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for PaymentRailFactory instantiation.
 *
 * CRITICAL: These tests catch circular dependency issues that cause StackOverflowError.
 *
 * Previously, SolanaPayPaymentDetector and SolanaPaymentRail had circular constructor calls:
 * - SolanaPayPaymentDetector.<init> created SolanaPaymentRail
 * - SolanaPaymentRail.<init> created SolanaPayPaymentDetector
 * - ... infinite loop -> StackOverflowError
 *
 * FIX: Detector now accepts PaymentRail via constructor; Rail uses lazy { } for detector.
 */
class PaymentRailFactoryInstantiationTest {

    // ═══════════════════════════════════════════════════════════════════
    // CIRCULAR DEPENDENCY PREVENTION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `getRail for Solana devnet does not throw StackOverflowError`() {
        // This would throw StackOverflowError if circular dependency exists
        val rail = PaymentRailFactory.getRail("devnet")
        assertNotNull(rail)
    }

    @Test
    fun `getRail for Solana mainnet does not throw StackOverflowError`() {
        val rail = PaymentRailFactory.getRail("mainnet-beta")
        assertNotNull(rail)
    }

    @Test
    fun `getRailByRailId for solana does not throw StackOverflowError`() {
        val rail = PaymentRailFactory.getRailByRailId("solana")
        assertNotNull(rail)
    }

    @Test
    fun `SolanaPaymentRail can be instantiated directly`() {
        // Direct instantiation should also work without recursion
        val rail = SolanaPaymentRail()
        assertNotNull(rail)
    }

    // ═══════════════════════════════════════════════════════════════════
    // RAIL FUNCTIONALITY TESTS (after instantiation)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `derivePollingTargets works after instantiation`() {
        val rail = PaymentRailFactory.getRail("devnet")
        val invoice = createTestInvoice()

        val targets = rail.derivePollingTargets(invoice)
        assertTrue(targets.isNotEmpty())
    }

    @Test
    fun `buildPaymentRequest works after instantiation`() {
        val rail = PaymentRailFactory.getRail("devnet")
        val invoice = createTestInvoice()

        val url = rail.buildPaymentRequest(invoice)
        assertTrue(url.startsWith("solana:"))
    }

    @Test
    fun `generateReference works after instantiation`() {
        val rail = PaymentRailFactory.getRail("devnet")

        val ref = rail.generateReference()
        assertTrue(ref.isNotBlank())
    }

    @Test
    fun `isDetecting returns false for unknown invoice`() {
        val rail = PaymentRailFactory.getRail("devnet")

        val isDetecting = rail.isDetecting("unknown-invoice-id")
        assertTrue(!isDetecting)
    }

    // ═══════════════════════════════════════════════════════════════════
    // FACTORY METHOD TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `getRailOrNull returns null for unsupported network`() {
        val rail = PaymentRailFactory.getRailOrNull("unsupported-network")
        assertTrue(rail == null)
    }

    @Test
    fun `isRailSupported returns true for solana`() {
        assertTrue(PaymentRailFactory.isRailSupported("solana"))
    }

    @Test
    fun `isRailImplemented returns true for solana`() {
        assertTrue(PaymentRailFactory.isRailImplemented("solana"))
    }

    @Test
    fun `isNetworkSupported returns true for devnet`() {
        assertTrue(PaymentRailFactory.isNetworkSupported("devnet"))
    }

    @Test
    fun `getDefaultRail returns Solana rail`() {
        val rail = PaymentRailFactory.getDefaultRail()
        assertTrue(rail.railId == "solana")
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
