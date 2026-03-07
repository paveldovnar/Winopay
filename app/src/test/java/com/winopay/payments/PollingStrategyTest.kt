package com.winopay.payments

import com.winopay.BuildConfig
import com.winopay.solana.SolanaAddressUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for multi-strategy polling target derivation.
 *
 * Tests the PollingStrategy enum and ATA derivation logic without
 * requiring network calls or database access.
 *
 * MULTI-STRATEGY DETECTION:
 * 1. Strategy A (DERIVED_ATA): Poll derived ATAs
 * 2. Strategy B (DISCOVERED_ACCOUNT): Poll discovered token accounts
 * 3. Strategy C (WALLET_SCAN): Scan wallet for SPL transfers (future)
 */
class PollingStrategyTest {

    // Test wallet addresses (real devnet addresses)
    private val testWallet = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"

    // ═══════════════════════════════════════════════════════════════════
    // POLLING STRATEGY ENUM TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `PollingStrategy enum has all expected values`() {
        val strategies = PollingStrategy.values()
        assertEquals("Should have 3 strategies", 3, strategies.size)
        assertTrue("Should have DERIVED_ATA", strategies.contains(PollingStrategy.DERIVED_ATA))
        assertTrue("Should have DISCOVERED_ACCOUNT", strategies.contains(PollingStrategy.DISCOVERED_ACCOUNT))
        assertTrue("Should have WALLET_SCAN", strategies.contains(PollingStrategy.WALLET_SCAN))
    }

    // ═══════════════════════════════════════════════════════════════════
    // POLLING TARGET DATA CLASS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `PollingTarget with default strategy is DERIVED_ATA`() {
        val target = PollingTarget(
            address = testWallet,
            tokenMint = null,
            label = "wallet"
        )
        assertEquals("Default strategy should be DERIVED_ATA", PollingStrategy.DERIVED_ATA, target.strategy)
    }

    @Test
    fun `PollingTarget can have different strategies`() {
        val derivedTarget = PollingTarget(
            address = testWallet,
            tokenMint = BuildConfig.USDC_MINT,
            label = "USDC_ATA",
            strategy = PollingStrategy.DERIVED_ATA
        )
        assertEquals(PollingStrategy.DERIVED_ATA, derivedTarget.strategy)

        val discoveredTarget = PollingTarget(
            address = "DiscoveredAccountAddress123456789012345678901234",
            tokenMint = BuildConfig.USDC_MINT,
            label = "USDC_DISCOVERED",
            strategy = PollingStrategy.DISCOVERED_ACCOUNT
        )
        assertEquals(PollingStrategy.DISCOVERED_ACCOUNT, discoveredTarget.strategy)
    }

    // ═══════════════════════════════════════════════════════════════════
    // ATA DERIVATION CONSISTENCY TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `ATA derivation is deterministic`() {
        val wallet = testWallet
        val mint = BuildConfig.USDC_MINT

        // Derive multiple times
        val ata1 = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, mint)
        val ata2 = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, mint)

        assertEquals("ATA derivation should be deterministic", ata1, ata2)
    }

    @Test
    fun `ATA derivation produces valid address`() {
        val wallet = testWallet
        val mint = BuildConfig.USDC_MINT

        val ata = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, mint)

        // Base58 Solana address is 32-44 characters
        assertTrue("ATA should be valid length", ata.length in 32..44)
        assertTrue("ATA should be valid Base58", SolanaAddressUtils.isValidSolanaAddress(ata))
    }

    @Test
    fun `ATA differs from wallet address`() {
        val wallet = testWallet
        val mint = BuildConfig.USDC_MINT

        val ata = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, mint)

        assertNotEquals("ATA should differ from wallet", wallet, ata)
    }

    @Test
    fun `Different mints produce different ATAs`() {
        val wallet = testWallet

        val usdcAta = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, BuildConfig.USDC_MINT)

        // Use a different mint for comparison
        val otherMint = "So11111111111111111111111111111111111111112" // Wrapped SOL mint
        val otherAta = SolanaAddressUtils.deriveAssociatedTokenAddress(wallet, otherMint)

        assertNotEquals("Different mints should produce different ATAs", usdcAta, otherAta)
    }

    @Test
    fun `Different wallets produce different ATAs for same mint`() {
        val mint = BuildConfig.USDC_MINT

        val wallet1Ata = SolanaAddressUtils.deriveAssociatedTokenAddress(testWallet, mint)
        val wallet2Ata = SolanaAddressUtils.deriveAssociatedTokenAddress(
            "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU", // Different wallet
            mint
        )

        assertNotEquals("Different wallets should produce different ATAs", wallet1Ata, wallet2Ata)
    }

    // NOTE: SolanaTokenPolicy tests are in DualStablecoinAcceptanceTest.kt
    // They require Android's Log class which is not available in unit tests
}
