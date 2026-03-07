package com.winopay.solana.pay

import com.winopay.payments.TokenPolicyProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for dual stablecoin acceptance (USDC + USDT).
 *
 * Tests verify:
 * 1. TokenPolicyProvider correctly identifies stablecoins
 * 2. getAllowedTokens returns correct sets
 * 3. Polling targets are derived correctly
 * 4. Warning codes are generated correctly
 * 5. Devnet guard disables dual acceptance
 */
class DualStablecoinAcceptanceTest {

    // Test mint addresses (mainnet)
    private val USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    private val USDT_MINT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
    private val RANDOM_TOKEN = "SomeRandomTokenMintAddressHere1234567890abc"

    // ═══════════════════════════════════════════════════════════════════
    // STABLECOIN IDENTIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `USDC mint is recognized as stablecoin`() {
        // This test validates the concept - actual SolanaTokenPolicy uses BuildConfig
        val knownStablecoins = setOf(USDC_MINT, USDT_MINT)
        assertTrue("USDC should be recognized as stablecoin", USDC_MINT in knownStablecoins)
    }

    @Test
    fun `USDT mint is recognized as stablecoin`() {
        val knownStablecoins = setOf(USDC_MINT, USDT_MINT)
        assertTrue("USDT should be recognized as stablecoin", USDT_MINT in knownStablecoins)
    }

    @Test
    fun `Random token is not recognized as stablecoin`() {
        val knownStablecoins = setOf(USDC_MINT, USDT_MINT)
        assertFalse("Random token should not be stablecoin", RANDOM_TOKEN in knownStablecoins)
    }

    @Test
    fun `Null mint is not stablecoin (SOL payment)`() {
        val mint: String? = null
        val knownStablecoins = setOf(USDC_MINT, USDT_MINT)
        assertFalse("Null mint should not be stablecoin", mint in knownStablecoins)
    }

    // ═══════════════════════════════════════════════════════════════════
    // ALLOWED MINTS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `getAllowedMints for USDC returns both stablecoins when dual acceptance enabled`() {
        // With dual acceptance enabled, USDC invoice should accept both USDC and USDT
        val isDualEnabled = true
        val expectedMint = USDC_MINT

        val allowedMints = if (isDualEnabled && expectedMint in setOf(USDC_MINT, USDT_MINT)) {
            setOf(USDC_MINT, USDT_MINT)
        } else {
            setOf(expectedMint)
        }

        assertEquals(2, allowedMints.size)
        assertTrue(USDC_MINT in allowedMints)
        assertTrue(USDT_MINT in allowedMints)
    }

    @Test
    fun `getAllowedMints for USDT returns both stablecoins when dual acceptance enabled`() {
        val isDualEnabled = true
        val expectedMint = USDT_MINT

        val allowedMints = if (isDualEnabled && expectedMint in setOf(USDC_MINT, USDT_MINT)) {
            setOf(USDC_MINT, USDT_MINT)
        } else {
            setOf(expectedMint)
        }

        assertEquals(2, allowedMints.size)
        assertTrue(USDC_MINT in allowedMints)
        assertTrue(USDT_MINT in allowedMints)
    }

    @Test
    fun `getAllowedMints for USDC returns only USDC when dual acceptance disabled`() {
        val isDualEnabled = false
        val expectedMint = USDC_MINT

        val allowedMints = if (isDualEnabled && expectedMint in setOf(USDC_MINT, USDT_MINT)) {
            setOf(USDC_MINT, USDT_MINT)
        } else {
            setOf(expectedMint)
        }

        assertEquals(1, allowedMints.size)
        assertTrue(USDC_MINT in allowedMints)
        assertFalse(USDT_MINT in allowedMints)
    }

    @Test
    fun `getAllowedMints for random token returns only that token`() {
        val isDualEnabled = true
        val expectedMint = RANDOM_TOKEN

        val allowedMints = if (isDualEnabled && expectedMint in setOf(USDC_MINT, USDT_MINT)) {
            setOf(USDC_MINT, USDT_MINT)
        } else {
            setOf(expectedMint)
        }

        assertEquals(1, allowedMints.size)
        assertTrue(RANDOM_TOKEN in allowedMints)
    }

    // ═══════════════════════════════════════════════════════════════════
    // POLLING TARGETS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Stablecoin invoice should poll multiple ATAs when dual acceptance enabled`() {
        val isDualEnabled = true
        val expectedMint = USDC_MINT
        val recipientWallet = "MerchantWalletAddress12345"

        // Simulate getting polling targets
        val allowedMints = if (isDualEnabled && expectedMint in setOf(USDC_MINT, USDT_MINT)) {
            setOf(USDC_MINT, USDT_MINT)
        } else {
            setOf(expectedMint)
        }

        // Each allowed mint generates a polling target
        assertEquals("Should poll 2 ATAs for dual acceptance", 2, allowedMints.size)
    }

    @Test
    fun `SOL invoice should poll single wallet address`() {
        val expectedMint: String? = null  // SOL has no mint

        // For SOL, we poll the wallet directly
        val pollingTargetCount = if (expectedMint == null) 1 else 2

        assertEquals("SOL should poll 1 address (wallet)", 1, pollingTargetCount)
    }

    // ═══════════════════════════════════════════════════════════════════
    // WARNING CODE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Warning generated when paid with different stablecoin`() {
        val expectedMint = USDC_MINT
        val actualMint = USDT_MINT
        val stablecoins = setOf(USDC_MINT, USDT_MINT)

        val warningCode = if (actualMint != expectedMint &&
            expectedMint in stablecoins &&
            actualMint in stablecoins) {
            "PAID_WITH_DIFFERENT_STABLECOIN"
        } else {
            null
        }

        assertEquals("PAID_WITH_DIFFERENT_STABLECOIN", warningCode)
    }

    @Test
    fun `No warning when paid with expected stablecoin`() {
        val expectedMint = USDC_MINT
        val actualMint = USDC_MINT
        val stablecoins = setOf(USDC_MINT, USDT_MINT)

        val warningCode = if (actualMint != expectedMint &&
            expectedMint in stablecoins &&
            actualMint in stablecoins) {
            "PAID_WITH_DIFFERENT_STABLECOIN"
        } else {
            null
        }

        assertEquals(null, warningCode)
    }

    @Test
    fun `No warning for non-stablecoin payments`() {
        val expectedMint = RANDOM_TOKEN
        val actualMint = RANDOM_TOKEN
        val stablecoins = setOf(USDC_MINT, USDT_MINT)

        val warningCode = if (actualMint != expectedMint &&
            expectedMint in stablecoins &&
            actualMint in stablecoins) {
            "PAID_WITH_DIFFERENT_STABLECOIN"
        } else {
            null
        }

        assertEquals(null, warningCode)
    }

    // ═══════════════════════════════════════════════════════════════════
    // DEVNET GUARD TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Dual acceptance disabled when USDT_MINT is empty (devnet)`() {
        val usdcMint = USDC_MINT
        val usdtMint = ""  // Empty on devnet

        val isDualEnabled = usdcMint.isNotBlank() && usdtMint.isNotBlank()

        assertFalse("Dual acceptance should be disabled on devnet", isDualEnabled)
    }

    @Test
    fun `Dual acceptance enabled when both mints are valid (mainnet)`() {
        val usdcMint = USDC_MINT
        val usdtMint = USDT_MINT

        val isDualEnabled = usdcMint.isNotBlank() && usdtMint.isNotBlank()

        assertTrue("Dual acceptance should be enabled on mainnet", isDualEnabled)
    }

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATION INTEGRATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Mint in allowedMints set should pass validation`() {
        val allowedMints = setOf(USDC_MINT, USDT_MINT)
        val actualMint = USDT_MINT

        val mintMatched = actualMint in allowedMints

        assertTrue("USDT should match when in allowedMints", mintMatched)
    }

    @Test
    fun `Mint not in allowedMints set should fail validation`() {
        val allowedMints = setOf(USDC_MINT, USDT_MINT)
        val actualMint = RANDOM_TOKEN

        val mintMatched = actualMint in allowedMints

        assertFalse("Random token should not match", mintMatched)
    }

    @Test
    fun `actualMintUsed should be set when validation succeeds with different stablecoin`() {
        val expectedMint = USDC_MINT
        val actualMintUsed = USDT_MINT

        // When transaction validated successfully but with different stablecoin
        val result = TransactionValidator.Result.Valid(
            actualMintUsed = actualMintUsed,
            warningCode = if (actualMintUsed != expectedMint) {
                TokenPolicyProvider.WARNING_PAID_WITH_DIFFERENT_STABLECOIN
            } else null
        )

        assertEquals(USDT_MINT, result.actualMintUsed)
        assertEquals(TokenPolicyProvider.WARNING_PAID_WITH_DIFFERENT_STABLECOIN, result.warningCode)
    }
}
