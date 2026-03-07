package com.winopay.payments

import com.winopay.tron.TronConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PaymentRailFactory TRON support.
 */
class TronRailFactoryTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RAIL ID LOOKUP
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getRailByRailId returns TronPaymentRail for tron`() {
        val rail = PaymentRailFactory.getRailByRailId("tron")
        assertNotNull("Should return a rail", rail)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
        assertEquals("tron", rail.railId)
    }

    @Test
    fun `getRailByRailId with networkId uses correct network`() {
        val railMainnet = PaymentRailFactory.getRailByRailId("tron", TronConstants.NETWORK_MAINNET)
        val railNile = PaymentRailFactory.getRailByRailId("tron", TronConstants.NETWORK_NILE)

        assertTrue("Mainnet rail should be TronPaymentRail", railMainnet is TronPaymentRail)
        assertTrue("Nile rail should be TronPaymentRail", railNile is TronPaymentRail)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // NETWORK ID LOOKUP
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getRail returns TronPaymentRail for tron-mainnet`() {
        val rail = PaymentRailFactory.getRail(TronConstants.NETWORK_MAINNET)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
    }

    @Test
    fun `getRail returns TronPaymentRail for tron-nile`() {
        val rail = PaymentRailFactory.getRail(TronConstants.NETWORK_NILE)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
    }

    @Test
    fun `getRail returns TronPaymentRail for tron-shasta`() {
        val rail = PaymentRailFactory.getRail(TronConstants.NETWORK_SHASTA)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SUPPORT CHECKS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isRailSupported returns true for tron`() {
        assertTrue("tron rail should be supported", PaymentRailFactory.isRailSupported("tron"))
    }

    @Test
    fun `isRailImplemented returns true for tron`() {
        assertTrue("tron rail should be implemented", PaymentRailFactory.isRailImplemented("tron"))
    }

    @Test
    fun `isNetworkSupported returns true for TRON networks`() {
        assertTrue(PaymentRailFactory.isNetworkSupported(TronConstants.NETWORK_MAINNET))
        assertTrue(PaymentRailFactory.isNetworkSupported(TronConstants.NETWORK_NILE))
        assertTrue(PaymentRailFactory.isNetworkSupported(TronConstants.NETWORK_SHASTA))
    }

    @Test
    fun `getSupportedRails includes tron`() {
        val rails = PaymentRailFactory.getSupportedRails()
        assertTrue("tron should be in supported rails", "tron" in rails)
    }

    @Test
    fun `getImplementedRails includes tron`() {
        val rails = PaymentRailFactory.getImplementedRails()
        assertTrue("tron should be in implemented rails", "tron" in rails)
    }

    @Test
    fun `getSupportedNetworks includes TRON networks`() {
        val networks = PaymentRailFactory.getSupportedNetworks()
        assertTrue(TronConstants.NETWORK_MAINNET in networks)
        assertTrue(TronConstants.NETWORK_NILE in networks)
        assertTrue(TronConstants.NETWORK_SHASTA in networks)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RAIL CACHING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getRail returns same instance for same network`() {
        val rail1 = PaymentRailFactory.getRail(TronConstants.NETWORK_MAINNET)
        val rail2 = PaymentRailFactory.getRail(TronConstants.NETWORK_MAINNET)
        assertTrue("Should return same instance", rail1 === rail2)
    }
}
