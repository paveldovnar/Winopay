package com.winopay.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressNormalizerTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Solana address normalization
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `normalize solana keeps valid base58 address unchanged`() {
        val address = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        val result = AddressNormalizer.normalize("solana", address)
        assertEquals(address, result)
    }

    @Test
    fun `normalize solana trims whitespace`() {
        val address = "  7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU  "
        val result = AddressNormalizer.normalize("solana", address)
        assertEquals("7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU", result)
    }

    @Test
    fun `isValid solana accepts valid addresses`() {
        assertTrue(AddressNormalizer.isValid("solana", "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"))
        assertTrue(AddressNormalizer.isValid("solana", "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")) // USDC mint
    }

    @Test
    fun `isValid solana rejects invalid addresses`() {
        assertFalse(AddressNormalizer.isValid("solana", "abc")) // too short
        assertFalse(AddressNormalizer.isValid("solana", "0x1234567890123456789012345678901234567890")) // EVM format
        assertFalse(AddressNormalizer.isValid("solana", "TXyz123456789012345678901234567890123")) // TRON format
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TRON address normalization
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `normalize tron keeps valid address unchanged`() {
        // Real TRON testnet address format (base58check, starts with T, 34 chars)
        val address = "TJYvsuAk8dLmBZyLqijEV8wPTiKQp2Y2YN"
        val result = AddressNormalizer.normalize("tron", address)
        assertEquals(address, result)
    }

    @Test
    fun `isValid tron accepts valid T-prefix addresses`() {
        // Real TRON mainnet USDT contract address
        assertTrue(AddressNormalizer.isValid("tron", "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"))
        // TRON foundation address
        assertTrue(AddressNormalizer.isValid("tron", "TJYvsuAk8dLmBZyLqijEV8wPTiKQp2Y2YN"))
    }

    @Test
    fun `isValid tron rejects invalid addresses`() {
        assertFalse(AddressNormalizer.isValid("tron", "abc")) // too short
        assertFalse(AddressNormalizer.isValid("tron", "Axyz123456789012345678901234567890123")) // wrong prefix
        assertFalse(AddressNormalizer.isValid("tron", "0x1234567890123456789012345678901234567890")) // EVM format
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // EVM address normalization
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `normalize evm converts to lowercase`() {
        val address = "0xABCDEF1234567890ABCDEF1234567890ABCDEF12"
        val result = AddressNormalizer.normalize("evm", address)
        assertEquals("0xabcdef1234567890abcdef1234567890abcdef12", result)
    }

    @Test
    fun `normalize evm preserves lowercase`() {
        val address = "0xabcdef1234567890abcdef1234567890abcdef12"
        val result = AddressNormalizer.normalize("evm", address)
        assertEquals(address, result)
    }

    @Test
    fun `normalize evm handles 0X prefix`() {
        val address = "0XABCDEF1234567890ABCDEF1234567890ABCDEF12"
        val result = AddressNormalizer.normalize("evm", address)
        assertEquals("0xabcdef1234567890abcdef1234567890abcdef12", result)
    }

    @Test
    fun `isValid evm accepts valid addresses`() {
        assertTrue(AddressNormalizer.isValid("evm", "0xabcdef1234567890abcdef1234567890abcdef12"))
        assertTrue(AddressNormalizer.isValid("evm", "0xABCDEF1234567890ABCDEF1234567890ABCDEF12"))
    }

    @Test
    fun `isValid evm rejects invalid addresses`() {
        assertFalse(AddressNormalizer.isValid("evm", "abc")) // too short
        assertFalse(AddressNormalizer.isValid("evm", "0xabcdef")) // too short
        assertFalse(AddressNormalizer.isValid("evm", "0xGGGGGG1234567890ABCDEF1234567890ABCDEF12")) // invalid hex
        assertFalse(AddressNormalizer.isValid("evm", "0x")) // too short
        assertFalse(AddressNormalizer.isValid("evm", "0xZZZZZZ1234567890abcdef1234567890abcdef12")) // invalid chars
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Address shortening
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `shorten with default params`() {
        val address = "0xabcdef1234567890abcdef1234567890abcdef12"
        val result = AddressNormalizer.shorten(address)
        assertEquals("0xabcd...ef12", result)
    }

    @Test
    fun `shorten with custom lengths`() {
        val address = "0xabcdef1234567890abcdef1234567890abcdef12"
        val result = AddressNormalizer.shorten(address, prefixLen = 10, suffixLen = 6)
        assertEquals("0xabcdef12...cdef12", result)
    }

    @Test
    fun `shorten returns full address if too short`() {
        val address = "0xabc"
        val result = AddressNormalizer.shorten(address)
        assertEquals("0xabc", result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Unknown rails
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `normalize unknown rail returns address as-is`() {
        val address = "someUnknownAddress123"
        val result = AddressNormalizer.normalize("bitcoin", address)
        assertEquals(address, result)
    }

    @Test
    fun `isValid unknown rail returns false`() {
        assertFalse(AddressNormalizer.isValid("bitcoin", "someAddress"))
    }
}
