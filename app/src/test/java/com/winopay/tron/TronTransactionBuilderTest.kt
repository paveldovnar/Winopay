package com.winopay.tron

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TronTransactionBuilder.
 *
 * Tests parameter encoding for TRC20 transfers.
 */
class TronTransactionBuilderTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PARAMETER ENCODING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `encodeTransferParameter produces 128 char hex string`() {
        val recipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val amount = 1_000_000L // 1 USDT

        val result = TronTransactionBuilder.encodeTransferParameter(recipientHex, amount)

        assertEquals("Parameter should be 128 hex chars", 128, result.length)
    }

    @Test
    fun `encodeTransferParameter pads address correctly`() {
        // 20-byte address: a614f803b6fd780986a42c78ec9c7f77e6ded13c
        val recipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val amount = 1_000_000L

        val result = TronTransactionBuilder.encodeTransferParameter(recipientHex, amount)

        // First 64 chars should contain the address (last 40 chars are actual address)
        val addressPart = result.substring(0, 64)
        assertTrue(
            "Address should be zero-padded on left",
            addressPart.startsWith("0000000000000000000000")
        )
        assertTrue(
            "Address should end with actual address",
            addressPart.endsWith("a614f803b6fd780986a42c78ec9c7f77e6ded13c")
        )
    }

    @Test
    fun `encodeTransferParameter encodes amount correctly`() {
        val recipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val amount = 5_000_000L // 5 USDT (in 6 decimals)

        val result = TronTransactionBuilder.encodeTransferParameter(recipientHex, amount)

        // Last 64 chars should be the amount
        val amountPart = result.substring(64, 128)

        // 5_000_000 in hex = 4c4b40, padded to 64 chars
        assertTrue("Amount should be zero-padded", amountPart.startsWith("00000"))
        assertTrue("Amount should end with hex value", amountPart.endsWith("4c4b40"))
    }

    @Test
    fun `encodeTransferParameter handles large amounts`() {
        val recipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val amount = 1_000_000_000_000L // 1 million USDT

        val result = TronTransactionBuilder.encodeTransferParameter(recipientHex, amount)

        assertEquals(128, result.length)

        // Verify decode roundtrip
        val decoded = TronTransactionBuilder.decodeTransferParameter(result)
        assertNotNull("Should decode successfully", decoded)
        assertEquals("Amount should match", amount, decoded!!.second)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PARAMETER DECODING
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `decodeTransferParameter roundtrip works correctly`() {
        val originalRecipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val originalAmount = 10_500_000L // 10.5 USDT

        val encoded = TronTransactionBuilder.encodeTransferParameter(originalRecipientHex, originalAmount)
        val decoded = TronTransactionBuilder.decodeTransferParameter(encoded)

        assertNotNull("Should decode successfully", decoded)
        assertEquals("Amount should match", originalAmount, decoded!!.second)
        // Address has 41 prefix added back
        assertTrue("Address should contain original address", decoded.first.contains("a614f803b6fd780986a42c78ec9c7f77e6ded13c"))
    }

    @Test
    fun `decodeTransferParameter returns null for invalid length`() {
        val invalid = "0011223344" // Too short
        val result = TronTransactionBuilder.decodeTransferParameter(invalid)
        assertEquals(null, result)
    }

    @Test
    fun `decodeTransferParameter adds 41 prefix back`() {
        val recipientHex = "41a614f803b6fd780986a42c78ec9c7f77e6ded13c"
        val amount = 1_000_000L

        val encoded = TronTransactionBuilder.encodeTransferParameter(recipientHex, amount)
        val decoded = TronTransactionBuilder.decodeTransferParameter(encoded)

        assertNotNull(decoded)
        assertTrue("Decoded address should start with 41", decoded!!.first.startsWith("41"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // BUILD TRANSACTION
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `buildTrc20Transfer succeeds with valid addresses`() {
        val owner = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t" // USDT contract (valid TRON address)
        val recipient = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8" // USDC contract (valid TRON address)
        val contract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val amount = 5_000_000L

        val result = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = owner,
            recipientAddress = recipient,
            tokenContract = contract,
            amountMinor = amount
        )

        assertTrue("Should succeed", result is TronTransactionBuilder.BuildResult.Success)
        val success = result as TronTransactionBuilder.BuildResult.Success
        assertEquals("transfer(address,uint256)", success.functionSelector)
        assertEquals(128, success.parameter.length)
        assertEquals(TronTransactionBuilder.DEFAULT_FEE_LIMIT_SUN, success.feeLimit)
    }

    @Test
    fun `buildTrc20Transfer fails with invalid owner address`() {
        val owner = "invalid"
        val recipient = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
        val contract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val amount = 5_000_000L

        val result = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = owner,
            recipientAddress = recipient,
            tokenContract = contract,
            amountMinor = amount
        )

        assertTrue("Should fail", result is TronTransactionBuilder.BuildResult.Error)
        val error = result as TronTransactionBuilder.BuildResult.Error
        assertTrue("Error should mention owner", error.message.contains("owner"))
    }

    @Test
    fun `buildTrc20Transfer fails with invalid recipient address`() {
        val owner = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val recipient = "not_valid"
        val contract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val amount = 5_000_000L

        val result = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = owner,
            recipientAddress = recipient,
            tokenContract = contract,
            amountMinor = amount
        )

        assertTrue("Should fail", result is TronTransactionBuilder.BuildResult.Error)
        val error = result as TronTransactionBuilder.BuildResult.Error
        assertTrue("Error should mention recipient", error.message.contains("recipient"))
    }

    @Test
    fun `buildTrc20Transfer fails with zero amount`() {
        val owner = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val recipient = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
        val contract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val amount = 0L

        val result = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = owner,
            recipientAddress = recipient,
            tokenContract = contract,
            amountMinor = amount
        )

        assertTrue("Should fail for zero amount", result is TronTransactionBuilder.BuildResult.Error)
    }

    @Test
    fun `buildTrc20Transfer fails with negative amount`() {
        val owner = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val recipient = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
        val contract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        val amount = -100L

        val result = TronTransactionBuilder.buildTrc20Transfer(
            ownerAddress = owner,
            recipientAddress = recipient,
            tokenContract = contract,
            amountMinor = amount
        )

        assertTrue("Should fail for negative amount", result is TronTransactionBuilder.BuildResult.Error)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ESTIMATES
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `estimateBandwidth returns reasonable value`() {
        val bandwidth = TronTransactionBuilder.estimateBandwidth()
        assertTrue("Bandwidth should be positive", bandwidth > 0)
        assertTrue("Bandwidth should be reasonable", bandwidth < 1000)
    }

    @Test
    fun `estimateEnergy returns reasonable value`() {
        val energy = TronTransactionBuilder.estimateEnergy()
        assertTrue("Energy should be positive", energy > 0)
        assertTrue("Energy should be in expected range", energy >= 30_000 && energy <= 100_000)
    }

    @Test
    fun `calculateFeeLimit returns value covering energy cost`() {
        val feeLimit = TronTransactionBuilder.calculateFeeLimit()
        val energy = TronTransactionBuilder.estimateEnergy()

        // Fee should cover at least the estimated energy at base price
        assertTrue("Fee limit should cover energy", feeLimit >= energy * 420)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CONSTANTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `TRC20_TRANSFER_SELECTOR is correct`() {
        assertEquals(
            "transfer(address,uint256)",
            TronTransactionBuilder.TRC20_TRANSFER_SELECTOR
        )
    }

    @Test
    fun `DEFAULT_FEE_LIMIT_SUN is 30 TRX`() {
        assertEquals(30_000_000L, TronTransactionBuilder.DEFAULT_FEE_LIMIT_SUN)
    }
}
