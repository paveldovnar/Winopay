package com.winopay.tron

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TronAddressUtils.
 */
class TronAddressUtilsTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ADDRESS VALIDATION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isValidAddress returns true for valid USDT contract`() {
        val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        assertTrue("USDT contract should be valid", TronAddressUtils.isValidAddress(usdtContract))
    }

    @Test
    fun `isValidAddress returns true for valid USDC contract`() {
        val usdcContract = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
        assertTrue("USDC contract should be valid", TronAddressUtils.isValidAddress(usdcContract))
    }

    @Test
    fun `isValidAddress returns false for null address`() {
        assertFalse("Null should be invalid", TronAddressUtils.isValidAddress(null))
    }

    @Test
    fun `isValidAddress returns false for empty string`() {
        assertFalse("Empty string should be invalid", TronAddressUtils.isValidAddress(""))
    }

    @Test
    fun `isValidAddress returns false for too short address`() {
        assertFalse("Short address should be invalid", TronAddressUtils.isValidAddress("TR7NHqjeKQx"))
    }

    @Test
    fun `isValidAddress returns false for address not starting with T`() {
        assertFalse(
            "Address not starting with T should be invalid",
            TronAddressUtils.isValidAddress("AR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")
        )
    }

    @Test
    fun `isValidAddress returns false for invalid base58 characters`() {
        // '0', 'O', 'I', 'l' are not valid base58 characters
        assertFalse(
            "Address with invalid base58 char should be invalid",
            TronAddressUtils.isValidAddress("T00NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ADDRESS CONVERSION (Limited tests - full conversion needs checksum validation)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `toHexAddress returns null for invalid address`() {
        assertNull("Invalid address should return null hex", TronAddressUtils.toHexAddress("invalid"))
    }

    @Test
    fun `toBase58Address returns null for invalid hex`() {
        assertNull("Invalid hex should return null base58", TronAddressUtils.toBase58Address("invalid"))
    }

    @Test
    fun `toBase58Address returns null for hex without 41 prefix`() {
        assertNull(
            "Hex without 41 prefix should return null",
            TronAddressUtils.toBase58Address("00a614f803b6fd780986a42c78ec9c7f77e6ded13c")
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FORMAT FOR DISPLAY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `formatForDisplay truncates long address`() {
        val address = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val formatted = TronAddressUtils.formatForDisplay(address)
        // Default: 6 prefix + 4 suffix
        assertEquals("TR7NHq...Lj6t", formatted)
    }

    @Test
    fun `formatForDisplay returns short address unchanged`() {
        val shortAddress = "TR7N"
        val formatted = TronAddressUtils.formatForDisplay(shortAddress)
        assertEquals(shortAddress, formatted)
    }
}
