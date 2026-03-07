package com.winopay.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for MerchantProfile model.
 *
 * MULTICHAIN KEY FORMAT:
 * profile:{railId}:{networkId}:{accountId}
 *
 * Examples:
 * - Solana Devnet: profile:solana:devnet:GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW
 * - Solana Mainnet: profile:solana:mainnet-beta:GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW
 * - EVM Polygon: profile:evm:137:0x1234...
 */
class MerchantProfileStoreTest {

    // ═══════════════════════════════════════════════════════════════════
    // PROFILE KEY TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `profileKey format is correct`() {
        val key = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "devnet",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"
        )

        assertEquals(
            "profile:solana:devnet:GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW",
            key
        )
    }

    @Test
    fun `profileKey for mainnet differs from devnet`() {
        val devnetKey = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "devnet",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"
        )

        val mainnetKey = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "mainnet-beta",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"
        )

        assertTrue(devnetKey != mainnetKey)
        assertTrue(devnetKey.contains("devnet"))
        assertTrue(mainnetKey.contains("mainnet-beta"))
    }

    @Test
    fun `profileKey for different accounts on same network differs`() {
        val wallet1Key = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "devnet",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"
        )

        val wallet2Key = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "devnet",
            accountId = "DifferentWalletAddress123456789012345678901234"
        )

        assertTrue(wallet1Key != wallet2Key)
    }

    @Test
    fun `instance profileKey matches companion method`() {
        val profile = MerchantProfile(
            railId = "solana",
            networkId = "devnet",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW",
            businessName = "Test Business"
        )

        val instanceKey = profile.profileKey()
        val staticKey = MerchantProfile.profileKey(
            profile.railId,
            profile.networkId,
            profile.accountId
        )

        assertEquals(staticKey, instanceKey)
    }

    // ═══════════════════════════════════════════════════════════════════
    // MODEL TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `MerchantProfile has correct defaults`() {
        val profile = MerchantProfile(
            railId = "solana",
            networkId = "devnet",
            accountId = "TestAccount",
            businessName = "Test Business"
        )

        assertEquals("solana", profile.railId)
        assertEquals("devnet", profile.networkId)
        assertEquals("TestAccount", profile.accountId)
        assertEquals("Test Business", profile.businessName)
        assertNull(profile.logoLocalPath)
        assertTrue(profile.updatedAt > 0)
    }

    @Test
    fun `MerchantProfile with logo path`() {
        val logoPath = "/data/user/0/com.winopay/files/profile_logos/logo_test.jpg"
        val profile = MerchantProfile(
            railId = "solana",
            networkId = "mainnet-beta",
            accountId = "TestAccount",
            businessName = "Test Business",
            logoLocalPath = logoPath
        )

        assertEquals(logoPath, profile.logoLocalPath)
    }

    @Test
    fun `MerchantProfile updatedAt is set on creation`() {
        val before = System.currentTimeMillis()
        val profile = MerchantProfile(
            railId = "solana",
            networkId = "devnet",
            accountId = "TestAccount",
            businessName = "Test Business"
        )
        val after = System.currentTimeMillis()

        assertTrue(profile.updatedAt >= before)
        assertTrue(profile.updatedAt <= after)
    }

    // ═══════════════════════════════════════════════════════════════════
    // MULTICHAIN ISOLATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `EVM profile key format`() {
        val key = MerchantProfile.profileKey(
            railId = "evm",
            networkId = "137", // Polygon
            accountId = "0x1234567890abcdef1234567890abcdef12345678"
        )

        assertEquals(
            "profile:evm:137:0x1234567890abcdef1234567890abcdef12345678",
            key
        )
    }

    @Test
    fun `Different rails have different keys`() {
        val solanaKey = MerchantProfile.profileKey(
            railId = "solana",
            networkId = "mainnet-beta",
            accountId = "GsbwXfJraMomNxBcjYLcG3mxkBUiyWXAB32fGbSMQRdW"
        )

        val evmKey = MerchantProfile.profileKey(
            railId = "evm",
            networkId = "1", // Ethereum mainnet
            accountId = "0x1234567890abcdef1234567890abcdef12345678"
        )

        assertTrue(solanaKey != evmKey)
        assertTrue(solanaKey.startsWith("profile:solana:"))
        assertTrue(evmKey.startsWith("profile:evm:"))
    }

    // ═══════════════════════════════════════════════════════════════════
    // RAIL CONNECTION SERIALIZATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `RailConnection serialize without sessionId`() {
        val connection = RailConnection(
            railId = "solana",
            networkId = "devnet",
            accountId = "ABC123",
            connectedAt = 1234567890L
        )

        val serialized = RailConnection.serialize(connection)
        assertEquals("solana|devnet|ABC123|1234567890", serialized)
    }

    @Test
    fun `RailConnection serialize with sessionId`() {
        val connection = RailConnection(
            railId = "solana",
            networkId = "devnet",
            accountId = "ABC123",
            connectedAt = 1234567890L,
            sessionId = "wc-session-topic-xyz"
        )

        val serialized = RailConnection.serialize(connection)
        assertEquals("solana|devnet|ABC123|1234567890|wc-session-topic-xyz", serialized)
    }

    @Test
    fun `RailConnection deserialize without sessionId - backward compatible`() {
        val data = "solana|devnet|ABC123|1234567890"
        val connection = RailConnection.deserialize(data)

        assertNotNull(connection)
        assertEquals("solana", connection?.railId)
        assertEquals("devnet", connection?.networkId)
        assertEquals("ABC123", connection?.accountId)
        assertEquals(1234567890L, connection?.connectedAt)
        assertNull(connection?.sessionId)
    }

    @Test
    fun `RailConnection deserialize with sessionId`() {
        val data = "solana|devnet|ABC123|1234567890|wc-session-topic-xyz"
        val connection = RailConnection.deserialize(data)

        assertNotNull(connection)
        assertEquals("solana", connection?.railId)
        assertEquals("devnet", connection?.networkId)
        assertEquals("ABC123", connection?.accountId)
        assertEquals(1234567890L, connection?.connectedAt)
        assertEquals("wc-session-topic-xyz", connection?.sessionId)
    }

    @Test
    fun `RailConnection deserialize with empty sessionId is null`() {
        val data = "solana|devnet|ABC123|1234567890|"
        val connection = RailConnection.deserialize(data)

        assertNotNull(connection)
        assertNull(connection?.sessionId)
    }

    @Test
    fun `RailConnection deserialize invalid format returns null`() {
        assertNull(RailConnection.deserialize("invalid"))
        assertNull(RailConnection.deserialize("only|two"))
        assertNull(RailConnection.deserialize("one|two|three"))
    }

    @Test
    fun `RailConnection roundtrip without sessionId`() {
        val original = RailConnection(
            railId = "tron",
            networkId = "tron-mainnet",
            accountId = "TRX123",
            connectedAt = 9999999L
        )

        val serialized = RailConnection.serialize(original)
        val deserialized = RailConnection.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(original.railId, deserialized?.railId)
        assertEquals(original.networkId, deserialized?.networkId)
        assertEquals(original.accountId, deserialized?.accountId)
        assertEquals(original.connectedAt, deserialized?.connectedAt)
        assertEquals(original.sessionId, deserialized?.sessionId)
    }

    @Test
    fun `RailConnection roundtrip with sessionId`() {
        val original = RailConnection(
            railId = "evm",
            networkId = "56",
            accountId = "0xABCDEF",
            connectedAt = 1111111L,
            sessionId = "session-topic-123"
        )

        val serialized = RailConnection.serialize(original)
        val deserialized = RailConnection.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(original.railId, deserialized?.railId)
        assertEquals(original.networkId, deserialized?.networkId)
        assertEquals(original.accountId, deserialized?.accountId)
        assertEquals(original.connectedAt, deserialized?.connectedAt)
        assertEquals(original.sessionId, deserialized?.sessionId)
    }
}
