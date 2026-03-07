package com.winopay.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PaymentRailFactory and SolanaPaymentRail.
 *
 * NOTE: Full integration tests require Android environment.
 * These tests focus on network support logic which is pure Kotlin.
 */
class PaymentRailFactoryTest {

    // ═══════════════════════════════════════════════════════════════════
    // SOLANA NETWORK SUPPORT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `SolanaPaymentRail supports devnet`() {
        assertTrue(SolanaPaymentRail.supportsNetwork("devnet"))
    }

    @Test
    fun `SolanaPaymentRail supports mainnet`() {
        assertTrue(SolanaPaymentRail.supportsNetwork("mainnet"))
    }

    @Test
    fun `SolanaPaymentRail supports mainnet-beta`() {
        assertTrue(SolanaPaymentRail.supportsNetwork("mainnet-beta"))
    }

    @Test
    fun `SolanaPaymentRail supports testnet`() {
        assertTrue(SolanaPaymentRail.supportsNetwork("testnet"))
    }

    @Test
    fun `SolanaPaymentRail does not support ethereum`() {
        assertFalse(SolanaPaymentRail.supportsNetwork("ethereum"))
    }

    @Test
    fun `SolanaPaymentRail does not support bsc`() {
        assertFalse(SolanaPaymentRail.supportsNetwork("bsc"))
    }

    @Test
    fun `SolanaPaymentRail does not support polygon`() {
        assertFalse(SolanaPaymentRail.supportsNetwork("polygon"))
    }

    @Test
    fun `SolanaPaymentRail does not support empty string`() {
        assertFalse(SolanaPaymentRail.supportsNetwork(""))
    }

    // ═══════════════════════════════════════════════════════════════════
    // FACTORY NETWORK SUPPORT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `isNetworkSupported returns true for Solana networks`() {
        assertTrue(PaymentRailFactory.isNetworkSupported("devnet"))
        assertTrue(PaymentRailFactory.isNetworkSupported("mainnet"))
        assertTrue(PaymentRailFactory.isNetworkSupported("mainnet-beta"))
        assertTrue(PaymentRailFactory.isNetworkSupported("testnet"))
    }

    @Test
    fun `isNetworkSupported returns false for unsupported networks`() {
        assertFalse(PaymentRailFactory.isNetworkSupported("ethereum"))
        assertFalse(PaymentRailFactory.isNetworkSupported("bsc"))
        assertFalse(PaymentRailFactory.isNetworkSupported("polygon"))
        assertFalse(PaymentRailFactory.isNetworkSupported(""))
    }

    // ═══════════════════════════════════════════════════════════════════
    // SUPPORTED NETWORKS SET TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `getSupportedNetworks contains all Solana networks`() {
        val networks = PaymentRailFactory.getSupportedNetworks()

        assertTrue("devnet" in networks)
        assertTrue("mainnet" in networks)
        assertTrue("mainnet-beta" in networks)
        assertTrue("testnet" in networks)
    }

    @Test
    fun `getSupportedNetworks includes all Solana, TRON, and EVM networks`() {
        val networks = PaymentRailFactory.getSupportedNetworks()

        // Solana networks (4)
        assertTrue("devnet" in networks)
        assertTrue("mainnet" in networks)
        assertTrue("mainnet-beta" in networks)
        assertTrue("testnet" in networks)

        // TRON networks (3)
        assertTrue("tron-mainnet" in networks)
        assertTrue("tron-nile" in networks)
        assertTrue("tron-shasta" in networks)

        // EVM networks (6)
        assertTrue("1" in networks)      // Ethereum Mainnet
        assertTrue("137" in networks)    // Polygon Mainnet
        assertTrue("56" in networks)     // BSC Mainnet

        // Total: 4 Solana + 3 TRON + 6 EVM = 13
        assertEquals(13, networks.size)
    }

    @Test
    fun `Solana SUPPORTED_NETWORKS is subset of factory list`() {
        val allNetworks = PaymentRailFactory.getSupportedNetworks()
        assertTrue(allNetworks.containsAll(SolanaPaymentRail.SUPPORTED_NETWORKS))
    }

    @Test
    fun `EVM SUPPORTED_NETWORKS is subset of factory list`() {
        val allNetworks = PaymentRailFactory.getSupportedNetworks()
        assertTrue(allNetworks.containsAll(EvmPaymentRail.SUPPORTED_NETWORKS))
    }

    // ═══════════════════════════════════════════════════════════════════
    // RAIL ID TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `SolanaPaymentRail RAIL_ID is solana`() {
        assertEquals("solana", SolanaPaymentRail.RAIL_ID)
    }

    @Test
    fun `EvmPaymentRail RAIL_ID is evm`() {
        assertEquals("evm", EvmPaymentRail.RAIL_ID)
    }

    // ═══════════════════════════════════════════════════════════════════
    // MULTI-RAIL SUPPORT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `isRailSupported returns true for solana`() {
        assertTrue(PaymentRailFactory.isRailSupported("solana"))
    }

    @Test
    fun `isRailSupported returns true for evm`() {
        assertTrue(PaymentRailFactory.isRailSupported("evm"))
    }

    @Test
    fun `isRailSupported returns false for unknown rail`() {
        assertFalse(PaymentRailFactory.isRailSupported("unknown"))
        assertFalse(PaymentRailFactory.isRailSupported("bitcoin"))
    }

    @Test
    fun `isRailImplemented returns true for solana`() {
        assertTrue(PaymentRailFactory.isRailImplemented("solana"))
    }

    @Test
    fun `isRailImplemented returns false for evm (stub only)`() {
        assertFalse(PaymentRailFactory.isRailImplemented("evm"))
    }

    @Test
    fun `getSupportedRails contains solana, tron and evm`() {
        val rails = PaymentRailFactory.getSupportedRails()
        assertTrue("solana" in rails)
        assertTrue("tron" in rails)
        assertTrue("evm" in rails)
        assertEquals(3, rails.size)
    }

    @Test
    fun `getImplementedRails contains solana and tron`() {
        val rails = PaymentRailFactory.getImplementedRails()
        assertTrue("solana" in rails)
        assertTrue("tron" in rails)
        assertFalse("evm" in rails)
        assertEquals(2, rails.size)
    }

    @Test
    fun `isNetworkSupported returns true for EVM chainIds`() {
        assertTrue(PaymentRailFactory.isNetworkSupported("1"))     // Ethereum
        assertTrue(PaymentRailFactory.isNetworkSupported("137"))   // Polygon
        assertTrue(PaymentRailFactory.isNetworkSupported("56"))    // BSC
    }
}
