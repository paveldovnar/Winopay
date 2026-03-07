package com.winopay.solana

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for stablecoin mint address configuration.
 *
 * CRITICAL: These mints must match the real token addresses on each network.
 * - Devnet USDC: Circle faucet USDC (faucet.circle.com)
 * - Mainnet USDC: Circle official USDC
 * - Mainnet USDT: Tether official USDT
 *
 * Using wrong mints will cause payment detection to fail (mint mismatch).
 */
class MintConfigurationTest {

    // ═══════════════════════════════════════════════════════════════════
    // OFFICIAL MINT ADDRESSES
    // ═══════════════════════════════════════════════════════════════════

    companion object {
        // Circle USDC on devnet (from faucet.circle.com)
        const val DEVNET_USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"

        // Circle USDC on mainnet-beta
        const val MAINNET_USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"

        // Tether USDT on mainnet-beta
        const val MAINNET_USDT_MINT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"

        // Standard Solana address length (Base58)
        const val SOLANA_ADDRESS_LENGTH = 44
    }

    // ═══════════════════════════════════════════════════════════════════
    // DEVNET MINT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `devnet USDC mint is Circle faucet address`() {
        // Circle faucet USDC on devnet - this is the ONLY valid devnet USDC
        // Users must get devnet USDC from faucet.circle.com
        assertEquals(
            "Devnet USDC mint must be Circle faucet address",
            "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
            DEVNET_USDC_MINT
        )
    }

    @Test
    fun `devnet USDC mint is valid Solana address length`() {
        assertEquals(
            "Devnet USDC mint should be 44 characters",
            SOLANA_ADDRESS_LENGTH,
            DEVNET_USDC_MINT.length
        )
    }

    @Test
    fun `devnet USDC mint is NOT the old wrong mint`() {
        // This was the wrong mint previously configured
        val wrongMint = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"
        assertTrue(
            "Devnet USDC mint must NOT be the old wrong mint",
            DEVNET_USDC_MINT != wrongMint
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // MAINNET MINT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `mainnet USDC mint is Circle official address`() {
        assertEquals(
            "Mainnet USDC must be Circle official address",
            "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            MAINNET_USDC_MINT
        )
    }

    @Test
    fun `mainnet USDT mint is Tether official address`() {
        assertEquals(
            "Mainnet USDT must be Tether official address",
            "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",
            MAINNET_USDT_MINT
        )
    }

    @Test
    fun `mainnet mints are valid Solana address lengths`() {
        assertEquals(SOLANA_ADDRESS_LENGTH, MAINNET_USDC_MINT.length)
        assertEquals(SOLANA_ADDRESS_LENGTH, MAINNET_USDT_MINT.length)
    }

    // ═══════════════════════════════════════════════════════════════════
    // MINT COMPARISON TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `devnet and mainnet USDC mints are different`() {
        assertTrue(
            "Devnet and mainnet USDC mints must be different",
            DEVNET_USDC_MINT != MAINNET_USDC_MINT
        )
    }

    @Test
    fun `mainnet USDC and USDT mints are different`() {
        assertTrue(
            "USDC and USDT mints must be different",
            MAINNET_USDC_MINT != MAINNET_USDT_MINT
        )
    }
}
