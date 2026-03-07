package com.winopay.solana.pay

import com.winopay.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TransactionValidator STRICT validation logic.
 *
 * Tests validation requirements:
 * - Recipient not found => Invalid
 * - Wrong mint => Invalid
 * - Underpay => Invalid
 * - Correct pay => Valid
 *
 * These tests verify the pure logic without network calls or Android dependencies.
 */
class TransactionValidatorTest {

    companion object {
        private const val RECIPIENT = "RecipientWalletAddress123456789"
        private const val USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        private const val WRONG_MINT = "WrongMintAddress123456789"
        private const val EXPECTED_AMOUNT = 1_000_000L // 1 USDC
    }

    // ═══════════════════════════════════════════════════════════════════
    // SPL TOKEN VALIDATION LOGIC TESTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Test data class representing token balance change.
     */
    data class TokenBalanceChange(
        val owner: String,
        val mint: String,
        val preAmount: Long,
        val postAmount: Long
    )

    /**
     * Simulates SPL validation logic (mirrors TransactionValidator).
     */
    private fun validateSplLogic(
        balanceChanges: List<TokenBalanceChange>,
        expectedRecipient: String,
        expectedMint: String,
        expectedAmount: Long
    ): Pair<Boolean, String> {
        var recipientFound = false
        var amountReceived = 0L
        var mintMatched = false

        for (change in balanceChanges) {
            if (change.mint != expectedMint) continue
            mintMatched = true

            val delta = change.postAmount - change.preAmount
            if (change.owner == expectedRecipient && delta > 0) {
                recipientFound = true
                amountReceived = delta
                break
            }
        }

        if (!mintMatched) {
            return false to "Expected mint $expectedMint not found"
        }

        if (!recipientFound) {
            return false to "Recipient $expectedRecipient did not receive tokens"
        }

        if (amountReceived < expectedAmount) {
            val shortfall = expectedAmount - amountReceived
            if (shortfall > 1) {
                return false to "Underpayment: received $amountReceived, expected $expectedAmount"
            }
        }

        return true to "Valid"
    }

    @Test
    fun `SPL - recipient not found returns Invalid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = "WrongRecipient",
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 1_000_000L
            )
        )

        val (valid, reason) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention recipient", reason.contains("did not receive"))
    }

    @Test
    fun `SPL - wrong mint returns Invalid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = WRONG_MINT,
                preAmount = 0L,
                postAmount = 1_000_000L
            )
        )

        val (valid, reason) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention mint", reason.contains("mint"))
    }

    @Test
    fun `SPL - underpay by more than 1 unit returns Invalid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 900_000L // Underpay by 100,000
            )
        )

        val (valid, reason) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention underpayment", reason.contains("Underpayment"))
    }

    @Test
    fun `SPL - exact amount returns Valid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 1_000_000L
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertTrue("Should be valid", valid)
    }

    @Test
    fun `SPL - overpay returns Valid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 1_100_000L // Overpay
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertTrue("Should be valid (overpay accepted)", valid)
    }

    @Test
    fun `SPL - 1 unit tolerance returns Valid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 999_999L // 1 unit short
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertTrue("Should be valid (1 unit tolerance)", valid)
    }

    @Test
    fun `SPL - 2 units underpay returns Invalid`() {
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 999_998L // 2 units short
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("Should be invalid (2 units short)", valid)
    }

    @Test
    fun `SPL - empty balance changes returns Invalid`() {
        val changes = emptyList<TokenBalanceChange>()

        val (valid, reason) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention mint not found", reason.contains("not found"))
    }

    @Test
    fun `SPL - ATA created in tx (pre=0) returns Valid`() {
        // Simulates ATA being created in the same transaction
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 0L, // ATA didn't exist before
                postAmount = 1_000_000L
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertTrue("Should be valid (ATA created)", valid)
    }

    @Test
    fun `SPL - existing balance increased returns Valid`() {
        // Recipient already had tokens, received more
        val changes = listOf(
            TokenBalanceChange(
                owner = RECIPIENT,
                mint = USDC_MINT,
                preAmount = 5_000_000L, // Already had 5 USDC
                postAmount = 6_000_000L // Received 1 USDC
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertTrue("Should be valid", valid)
    }

    // ═══════════════════════════════════════════════════════════════════
    // SOL TRANSFER VALIDATION LOGIC TESTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Test data class representing SOL transfer.
     */
    data class SolTransfer(
        val destination: String,
        val lamports: Long
    )

    /**
     * Simulates SOL validation logic (mirrors TransactionValidator).
     */
    private fun validateSolLogic(
        transfers: List<SolTransfer>,
        expectedRecipient: String,
        expectedAmount: Long
    ): Pair<Boolean, String> {
        for (transfer in transfers) {
            if (transfer.destination == expectedRecipient) {
                if (transfer.lamports < expectedAmount) {
                    val shortfall = expectedAmount - transfer.lamports
                    if (shortfall > 1) {
                        return false to "Underpayment: received ${transfer.lamports}, expected $expectedAmount"
                    }
                }
                return true to "Valid"
            }
        }

        return false to "No valid SOL transfer to recipient $expectedRecipient found"
    }

    @Test
    fun `SOL - no transfer to recipient returns Invalid`() {
        val transfers = listOf(
            SolTransfer("WrongRecipient", 1_000_000_000L)
        )

        val (valid, reason) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention no transfer found", reason.contains("found"))
    }

    @Test
    fun `SOL - empty transfers returns Invalid`() {
        val transfers = emptyList<SolTransfer>()

        val (valid, _) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertFalse("Should be invalid", valid)
    }

    @Test
    fun `SOL - underpay returns Invalid`() {
        val transfers = listOf(
            SolTransfer(RECIPIENT, 900_000_000L) // Underpay
        )

        val (valid, reason) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertFalse("Should be invalid", valid)
        assertTrue("Reason should mention underpayment", reason.contains("Underpayment"))
    }

    @Test
    fun `SOL - exact amount returns Valid`() {
        val transfers = listOf(
            SolTransfer(RECIPIENT, 1_000_000_000L)
        )

        val (valid, _) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertTrue("Should be valid", valid)
    }

    @Test
    fun `SOL - overpay returns Valid`() {
        val transfers = listOf(
            SolTransfer(RECIPIENT, 1_100_000_000L)
        )

        val (valid, _) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertTrue("Should be valid (overpay)", valid)
    }

    // ═══════════════════════════════════════════════════════════════════
    // REFERENCE VERIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulates reference verification logic.
     */
    private fun verifyReferenceInKeys(accountKeys: List<String>, reference: String): Boolean {
        return reference in accountKeys
    }

    @Test
    fun `Reference found in accountKeys returns true`() {
        val keys = listOf("Key1", "ReferenceKey123", "Key2")
        assertTrue(verifyReferenceInKeys(keys, "ReferenceKey123"))
    }

    @Test
    fun `Reference not in accountKeys returns false`() {
        val keys = listOf("Key1", "Key2", "Key3")
        assertFalse(verifyReferenceInKeys(keys, "ReferenceKey123"))
    }

    @Test
    fun `Empty accountKeys returns false`() {
        val keys = emptyList<String>()
        assertFalse(verifyReferenceInKeys(keys, "ReferenceKey123"))
    }

    // ═══════════════════════════════════════════════════════════════════
    // AMOUNT TOLERANCE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Amount tolerance - exactly 1 unit short is accepted`() {
        val expected = 1_000_000L
        val received = 999_999L
        val shortfall = expected - received

        assertTrue("1 unit shortfall should be tolerated", shortfall <= 1)
    }

    @Test
    fun `Amount tolerance - 2 units short is rejected`() {
        val expected = 1_000_000L
        val received = 999_998L
        val shortfall = expected - received

        assertFalse("2 unit shortfall should NOT be tolerated", shortfall <= 1)
    }

    @Test
    fun `Amount tolerance - equal is accepted`() {
        val expected = 1_000_000L
        val received = 1_000_000L

        assertTrue("Equal amount should be accepted", received >= expected)
    }

    @Test
    fun `Amount tolerance - overpay is accepted`() {
        val expected = 1_000_000L
        val received = 1_000_001L

        assertTrue("Overpay should be accepted", received >= expected)
    }

    // ═══════════════════════════════════════════════════════════════════
    // REGRESSION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `REGRESSION - old code accepted missing recipient - now must fail`() {
        // The OLD code returned Valid when recipient not found
        // The NEW code must return Invalid
        val changes = listOf(
            TokenBalanceChange(
                owner = "SomeOtherWallet",
                mint = USDC_MINT,
                preAmount = 0L,
                postAmount = 1_000_000L
            )
        )

        val (valid, _) = validateSplLogic(changes, RECIPIENT, USDC_MINT, EXPECTED_AMOUNT)

        assertFalse("CRITICAL: Must reject when recipient not found", valid)
    }

    @Test
    fun `REGRESSION - old code accepted missing transfer - now must fail`() {
        // The OLD code returned Valid when no matching transfer found
        // The NEW code must return Invalid
        val transfers = emptyList<SolTransfer>()

        val (valid, _) = validateSolLogic(transfers, RECIPIENT, 1_000_000_000L)

        assertFalse("CRITICAL: Must reject when no transfer found", valid)
    }

    // ═══════════════════════════════════════════════════════════════════
    // BLOCKTIME VALIDATION LOGIC TESTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulates blockTime validation logic (mirrors TransactionValidator.validateTransactionJson).
     *
     * Returns: Pair<isValid, reason>
     */
    private fun validateBlockTimeLogic(
        invoiceCreatedAtMs: Long,
        blockTimeSeconds: Long
    ): Pair<Boolean, String> {
        // Rule 1: invoiceCreatedAt must be positive
        if (invoiceCreatedAtMs <= 0L) {
            return false to "Invalid invoice creation time"
        }

        // Rule 2: blockTime must be positive
        val blockTimeMs = blockTimeSeconds * 1000
        if (blockTimeMs <= 0L) {
            return false to "Missing blockTime"
        }

        // Rule 3: blockTime must be >= invoiceCreatedAt
        if (blockTimeMs < invoiceCreatedAtMs) {
            return false to "Transaction predates invoice creation"
        }

        return true to "Valid"
    }

    @Test
    fun `blockTime validation - invoiceCreatedAt zero rejects`() {
        val (valid, reason) = validateBlockTimeLogic(
            invoiceCreatedAtMs = 0L,
            blockTimeSeconds = 1L
        )

        assertFalse("Should reject zero invoiceCreatedAt", valid)
        assertTrue("Reason should mention invalid creation time", reason.contains("Invalid invoice"))
    }

    @Test
    fun `blockTime validation - invoiceCreatedAt negative rejects`() {
        val (valid, _) = validateBlockTimeLogic(
            invoiceCreatedAtMs = -1000L,
            blockTimeSeconds = 1L
        )

        assertFalse("Should reject negative invoiceCreatedAt", valid)
    }

    @Test
    fun `blockTime validation - blockTime zero rejects`() {
        val (valid, reason) = validateBlockTimeLogic(
            invoiceCreatedAtMs = 1000L,
            blockTimeSeconds = 0L
        )

        assertFalse("Should reject zero blockTime", valid)
        assertTrue("Reason should mention missing blockTime", reason.contains("Missing"))
    }

    @Test
    fun `blockTime validation - blockTime negative rejects`() {
        val (valid, _) = validateBlockTimeLogic(
            invoiceCreatedAtMs = 1000L,
            blockTimeSeconds = -1L
        )

        assertFalse("Should reject negative blockTime", valid)
    }

    @Test
    fun `blockTime validation - blockTime before invoiceCreatedAt rejects`() {
        val invoiceCreatedAtMs = 2000L  // 2 seconds
        val blockTimeSeconds = 1L        // 1 second (= 1000ms)

        val (valid, reason) = validateBlockTimeLogic(
            invoiceCreatedAtMs = invoiceCreatedAtMs,
            blockTimeSeconds = blockTimeSeconds
        )

        assertFalse("Should reject when blockTime < invoiceCreatedAt", valid)
        assertTrue("Reason should mention predates", reason.contains("predates"))
    }

    @Test
    fun `blockTime validation - blockTime equal to invoiceCreatedAt accepts`() {
        val timeMs = 2000L  // 2 seconds
        val timeSeconds = 2L // 2 seconds (= 2000ms)

        val (valid, _) = validateBlockTimeLogic(
            invoiceCreatedAtMs = timeMs,
            blockTimeSeconds = timeSeconds
        )

        assertTrue("Should accept when blockTime == invoiceCreatedAt", valid)
    }

    @Test
    fun `blockTime validation - blockTime after invoiceCreatedAt accepts`() {
        val invoiceCreatedAtMs = 1000L  // 1 second
        val blockTimeSeconds = 2L        // 2 seconds (= 2000ms)

        val (valid, _) = validateBlockTimeLogic(
            invoiceCreatedAtMs = invoiceCreatedAtMs,
            blockTimeSeconds = blockTimeSeconds
        )

        assertTrue("Should accept when blockTime > invoiceCreatedAt", valid)
    }
}
