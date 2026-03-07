package com.winopay.tron

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TronConstants.
 */
class TronConstantsTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // NETWORK SUPPORT
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `supportsNetwork returns true for tron-mainnet`() {
        assertTrue(TronConstants.supportsNetwork("tron-mainnet"))
    }

    @Test
    fun `supportsNetwork returns true for tron-nile`() {
        assertTrue(TronConstants.supportsNetwork("tron-nile"))
    }

    @Test
    fun `supportsNetwork returns true for tron-shasta`() {
        assertTrue(TronConstants.supportsNetwork("tron-shasta"))
    }

    @Test
    fun `supportsNetwork returns false for solana networks`() {
        assertFalse(TronConstants.supportsNetwork("devnet"))
        assertFalse(TronConstants.supportsNetwork("mainnet"))
        assertFalse(TronConstants.supportsNetwork("mainnet-beta"))
    }

    @Test
    fun `supportsNetwork is case-insensitive`() {
        assertTrue(TronConstants.supportsNetwork("TRON-MAINNET"))
        assertTrue(TronConstants.supportsNetwork("Tron-Mainnet"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CONTRACT ADDRESS GETTERS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getUsdtContract returns correct mainnet address`() {
        assertEquals(
            "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
            TronConstants.getUsdtContract(TronConstants.NETWORK_MAINNET)
        )
    }

    @Test
    fun `getUsdcContract returns correct mainnet address`() {
        assertEquals(
            "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8",
            TronConstants.getUsdcContract(TronConstants.NETWORK_MAINNET)
        )
    }

    @Test
    fun `getUsdtContract returns nile testnet address`() {
        assertEquals(
            "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj",
            TronConstants.getUsdtContract(TronConstants.NETWORK_NILE)
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STABLECOIN DETECTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isKnownStablecoin returns true for USDT`() {
        assertTrue(TronConstants.isKnownStablecoin(
            TronConstants.MAINNET_USDT_CONTRACT,
            TronConstants.NETWORK_MAINNET
        ))
    }

    @Test
    fun `isKnownStablecoin returns true for USDC`() {
        assertTrue(TronConstants.isKnownStablecoin(
            TronConstants.MAINNET_USDC_CONTRACT,
            TronConstants.NETWORK_MAINNET
        ))
    }

    @Test
    fun `isKnownStablecoin returns false for unknown contract`() {
        assertFalse(TronConstants.isKnownStablecoin(
            "TXXXunknownContract12345678901234",
            TronConstants.NETWORK_MAINNET
        ))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TOKEN SYMBOLS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getTokenSymbol returns USDT for USDT contract`() {
        assertEquals("USDT", TronConstants.getTokenSymbol(TronConstants.MAINNET_USDT_CONTRACT))
    }

    @Test
    fun `getTokenSymbol returns USDC for USDC contract`() {
        assertEquals("USDC", TronConstants.getTokenSymbol(TronConstants.MAINNET_USDC_CONTRACT))
    }

    @Test
    fun `getTokenSymbol returns TRX for null`() {
        assertEquals("TRX", TronConstants.getTokenSymbol(null))
    }

    @Test
    fun `getTokenSymbol returns TRC20 for unknown`() {
        assertEquals("TRC20", TronConstants.getTokenSymbol("TXXXunknown"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CONSTANTS VALUES
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `TRC20 transfer method ID is correct`() {
        assertEquals("a9059cbb", TronConstants.TRC20_TRANSFER_METHOD_ID)
    }

    @Test
    fun `token decimals are 6`() {
        assertEquals(6, TronConstants.USDT_DECIMALS)
        assertEquals(6, TronConstants.USDC_DECIMALS)
    }

    @Test
    fun `SUPPORTED_NETWORKS contains all networks`() {
        assertEquals(3, TronConstants.SUPPORTED_NETWORKS.size)
        assertTrue(TronConstants.NETWORK_MAINNET in TronConstants.SUPPORTED_NETWORKS)
        assertTrue(TronConstants.NETWORK_NILE in TronConstants.SUPPORTED_NETWORKS)
        assertTrue(TronConstants.NETWORK_SHASTA in TronConstants.SUPPORTED_NETWORKS)
    }
}
