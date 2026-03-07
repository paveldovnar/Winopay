package com.winopay.tron

import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.TronPaymentRail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TRON connect functionality.
 *
 * Tests address validation and rail lookup.
 * NOTE: MerchantProfileStore tests require Android context (instrumented tests).
 */
class TronConnectTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ADDRESS VALIDATION FOR CONNECT
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `valid TRON address passes validation`() {
        val validAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        assertTrue(
            "Valid TRON address should pass validation",
            TronAddressUtils.isValidAddress(validAddress)
        )
    }

    @Test
    fun `invalid TRON address fails validation`() {
        assertFalse("Empty string should fail", TronAddressUtils.isValidAddress(""))
        assertFalse("Null should fail", TronAddressUtils.isValidAddress(null))
        assertFalse("Too short should fail", TronAddressUtils.isValidAddress("TR7NHqje"))
        assertFalse(
            "Non-T prefix should fail",
            TronAddressUtils.isValidAddress("AR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")
        )
    }

    @Test
    fun `Solana address fails TRON validation`() {
        // Typical Solana address (base58, 44 chars, doesn't start with T)
        val solanaAddress = "7EcDhSYGxXyscszYEp35KHN8vvw3svAuLKTzXwCFLtV"
        assertFalse(
            "Solana address should fail TRON validation",
            TronAddressUtils.isValidAddress(solanaAddress)
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RAIL LOOKUP
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getRailByRailId returns TronPaymentRail for tron`() {
        val rail = PaymentRailFactory.getRailByRailId("tron")
        assertNotNull("Should return a rail for tron", rail)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
        assertEquals("tron", rail.railId)
    }

    @Test
    fun `getRailByRailId returns TronPaymentRail for tron with mainnet networkId`() {
        val rail = PaymentRailFactory.getRailByRailId("tron", TronConstants.NETWORK_MAINNET)
        assertNotNull("Should return a rail for tron-mainnet", rail)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
    }

    @Test
    fun `getRailByRailId returns TronPaymentRail for tron with nile networkId`() {
        val rail = PaymentRailFactory.getRailByRailId("tron", TronConstants.NETWORK_NILE)
        assertNotNull("Should return a rail for tron-nile", rail)
        assertTrue("Should be TronPaymentRail", rail is TronPaymentRail)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RAIL ID CONSTANTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `TronPaymentRail RAIL_ID is tron`() {
        assertEquals("tron", TronPaymentRail.RAIL_ID)
    }

    @Test
    fun `TronConstants NETWORK_MAINNET is tron-mainnet`() {
        assertEquals("tron-mainnet", TronConstants.NETWORK_MAINNET)
    }

    @Test
    fun `TronConstants NETWORK_NILE is tron-nile`() {
        assertEquals("tron-nile", TronConstants.NETWORK_NILE)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FORMAT FOR DISPLAY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `formatForDisplay truncates correctly`() {
        val address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val formatted = TronAddressUtils.formatForDisplay(address)
        // 6 prefix + "..." + 4 suffix = "TR7NHq...Lj6t"
        assertTrue(
            "Should be truncated with ellipsis",
            formatted.contains("...")
        )
        assertTrue("Should start with TR7NHq", formatted.startsWith("TR7NHq"))
        assertTrue("Should end with Lj6t", formatted.endsWith("Lj6t"))
    }

    @Test
    fun `formatForDisplay returns short address unchanged`() {
        val shortAddress = "TABC"
        val formatted = TronAddressUtils.formatForDisplay(shortAddress)
        assertEquals("Short address should be unchanged", shortAddress, formatted)
    }
}
