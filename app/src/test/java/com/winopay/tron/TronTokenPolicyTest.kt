package com.winopay.tron

import com.winopay.payments.TronTokenPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TronTokenPolicy.
 *
 * TRON supports only USDT (no USDC, no dual stablecoin acceptance).
 */
class TronTokenPolicyTest {

    private val policy = TronTokenPolicy(TronConstants.NETWORK_MAINNET)

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STABLECOIN DETECTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isStablecoin returns true for USDT contract`() {
        assertTrue(
            "USDT should be recognized as stablecoin",
            policy.isStablecoin(TronConstants.MAINNET_USDT_CONTRACT)
        )
    }

    @Test
    fun `isStablecoin returns false for USDC contract - not supported on TRON`() {
        // TRON does not support USDC
        assertFalse(
            "USDC should NOT be recognized as stablecoin on TRON",
            policy.isStablecoin(TronConstants.MAINNET_USDC_CONTRACT)
        )
    }

    @Test
    fun `isStablecoin returns false for unknown contract`() {
        assertFalse(
            "Unknown contract should not be stablecoin",
            policy.isStablecoin("TXXXunknownContract12345678901234")
        )
    }

    @Test
    fun `isStablecoin returns false for null`() {
        assertFalse("Null should not be stablecoin", policy.isStablecoin(null))
    }

    @Test
    fun `isStablecoin returns false for empty string`() {
        assertFalse("Empty string should not be stablecoin", policy.isStablecoin(""))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STRICT MODE (NO DUAL ACCEPTANCE)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getAllowedTokens returns only USDT when USDT requested`() {
        val allowed = policy.getAllowedTokens(TronConstants.MAINNET_USDT_CONTRACT)

        assertTrue("USDT should be in allowed set", TronConstants.MAINNET_USDT_CONTRACT in allowed)
        assertFalse("USDC should NOT be in allowed set", TronConstants.MAINNET_USDC_CONTRACT in allowed)
        assertEquals("Should have exactly 1 allowed token", 1, allowed.size)
    }

    @Test
    fun `getAllowedTokens returns only requested token for any contract`() {
        val unknownToken = "TXXXunknownContract12345678901234"
        val allowed = policy.getAllowedTokens(unknownToken)

        assertEquals("Should have exactly 1 allowed token", 1, allowed.size)
        assertTrue("Requested token should be in set", unknownToken in allowed)
    }

    @Test
    fun `getAllowedTokens returns empty set for null`() {
        val allowed = policy.getAllowedTokens(null)
        assertTrue("Should return empty set for null", allowed.isEmpty())
    }

    @Test
    fun `getAllowedTokens returns empty set for blank string`() {
        val allowed = policy.getAllowedTokens("")
        assertTrue("Should return empty set for blank", allowed.isEmpty())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // WARNING CODES (ALWAYS NULL - NO DUAL ACCEPTANCE)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getWarningCode returns null - no dual acceptance on TRON`() {
        // TRON doesn't have dual acceptance, so no stablecoin mismatch warnings
        val warning = policy.getWarningCode(
            TronConstants.MAINNET_USDT_CONTRACT,
            TronConstants.MAINNET_USDC_CONTRACT
        )
        assertNull("No warning on TRON - no dual acceptance", warning)
    }

    @Test
    fun `getWarningCode returns null when tokens match`() {
        val warning = policy.getWarningCode(
            TronConstants.MAINNET_USDT_CONTRACT,
            TronConstants.MAINNET_USDT_CONTRACT
        )
        assertNull("No warning when tokens match", warning)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TOKEN SYMBOLS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getTokenSymbol returns USDT for USDT contract`() {
        assertEquals("USDT", policy.getTokenSymbol(TronConstants.MAINNET_USDT_CONTRACT))
    }

    @Test
    fun `getTokenSymbol returns TRX for null`() {
        assertEquals("TRX", policy.getTokenSymbol(null))
    }

    @Test
    fun `getTokenSymbol returns TRC20 for unknown contract`() {
        assertEquals("TRC20", policy.getTokenSymbol("TXXXunknownContract12345678901234"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DUAL ACCEPTANCE DISABLED
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isDualAcceptanceEnabled returns false - TRON only supports USDT`() {
        assertFalse("Dual acceptance should be DISABLED on TRON", policy.isDualAcceptanceEnabled)
    }
}
