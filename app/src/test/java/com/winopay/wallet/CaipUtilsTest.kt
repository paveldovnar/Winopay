package com.winopay.wallet

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CaipUtils CAIP-2/CAIP-10 parsing and mapping.
 */
class CaipUtilsTest {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // parseAccount tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `parseAccount extracts solana mainnet address correctly`() {
        // Per CAIP-30: Solana genesis hash truncated to 32 chars
        val caip10 = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp:ABC123xyz789"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("solana", result?.namespace)
        assertEquals("5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp", result?.chainId)
        assertEquals("ABC123xyz789", result?.address)
    }

    @Test
    fun `parseAccount extracts solana devnet address correctly`() {
        // Per CAIP-30: Solana genesis hash truncated to 32 chars
        val caip10 = "solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1:DevnetWalletAddr"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("solana", result?.namespace)
        assertEquals("EtWTRABZaYq6iMfeYKouRu166VU2xqa1", result?.chainId)
        assertEquals("DevnetWalletAddr", result?.address)
    }

    @Test
    fun `parseAccount extracts tron mainnet address correctly`() {
        val caip10 = "tron:0x2b6653dc:TXxyz123456789"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("tron", result?.namespace)
        assertEquals("0x2b6653dc", result?.chainId)
        assertEquals("TXxyz123456789", result?.address)
    }

    @Test
    fun `parseAccount extracts tron nile address correctly`() {
        val caip10 = "tron:0xcd8690dc:TNileTestAddr"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("tron", result?.namespace)
        assertEquals("0xcd8690dc", result?.chainId)
        assertEquals("TNileTestAddr", result?.address)
    }

    @Test
    fun `parseAccount extracts evm BSC address correctly`() {
        val caip10 = "eip155:56:0x1234567890abcdef1234567890abcdef12345678"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("eip155", result?.namespace)
        assertEquals("56", result?.chainId)
        assertEquals("0x1234567890abcdef1234567890abcdef12345678", result?.address)
    }

    @Test
    fun `parseAccount extracts evm Ethereum address correctly`() {
        val caip10 = "eip155:1:0xabcdef1234567890abcdef1234567890abcdef12"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("eip155", result?.namespace)
        assertEquals("1", result?.chainId)
        assertEquals("0xabcdef1234567890abcdef1234567890abcdef12", result?.address)
    }

    @Test
    fun `parseAccount returns null for invalid format - no colons`() {
        val result = CaipUtils.parseAccount("invalid-string")
        assertNull(result)
    }

    @Test
    fun `parseAccount returns null for invalid format - only one colon`() {
        val result = CaipUtils.parseAccount("solana:address")
        assertNull(result)
    }

    @Test
    fun `parseAccount returns null for empty parts`() {
        val result = CaipUtils.parseAccount("::address")
        assertNull(result)
    }

    @Test
    fun `parseAccount handles address with colons`() {
        // Edge case: address might contain colons (unlikely but spec allows)
        val caip10 = "eip155:1:0x123:extra:parts"
        val result = CaipUtils.parseAccount(caip10)

        assertNotNull(result)
        assertEquals("eip155", result?.namespace)
        assertEquals("1", result?.chainId)
        assertEquals("0x123:extra:parts", result?.address)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // buildChainId tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `buildChainId creates correct format`() {
        assertEquals("solana:devnet", CaipUtils.buildChainId("solana", "devnet"))
        assertEquals("eip155:56", CaipUtils.buildChainId("eip155", "56"))
        assertEquals("tron:0x2b6653dc", CaipUtils.buildChainId("tron", "0x2b6653dc"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // toChainId tests (WinoPay -> CAIP-2)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `toChainId maps solana devnet correctly`() {
        val result = CaipUtils.toChainId("solana", "devnet")
        // Per CAIP-30: truncated to 32 chars
        assertEquals("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1", result)
    }

    @Test
    fun `toChainId maps solana mainnet correctly`() {
        val result = CaipUtils.toChainId("solana", "mainnet-beta")
        // Per CAIP-30: truncated to 32 chars
        assertEquals("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp", result)
    }

    @Test
    fun `toChainId maps tron nile correctly`() {
        val result = CaipUtils.toChainId("tron", "tron-nile")
        assertEquals("tron:0xcd8690dc", result)
    }

    @Test
    fun `toChainId maps tron mainnet correctly`() {
        val result = CaipUtils.toChainId("tron", "tron-mainnet")
        assertEquals("tron:0x2b6653dc", result)
    }

    @Test
    fun `toChainId maps BSC mainnet correctly`() {
        val result = CaipUtils.toChainId("evm", "56")
        assertEquals("eip155:56", result)
    }

    @Test
    fun `toChainId maps BSC testnet correctly`() {
        val result = CaipUtils.toChainId("evm", "97")
        assertEquals("eip155:97", result)
    }

    @Test
    fun `toChainId maps Ethereum mainnet correctly`() {
        val result = CaipUtils.toChainId("evm", "1")
        assertEquals("eip155:1", result)
    }

    @Test
    fun `toChainId maps Sepolia correctly`() {
        val result = CaipUtils.toChainId("evm", "11155111")
        assertEquals("eip155:11155111", result)
    }

    @Test
    fun `toChainId returns null for unknown rail`() {
        val result = CaipUtils.toChainId("unknown", "network")
        assertNull(result)
    }

    @Test
    fun `toChainId returns null for unknown network`() {
        val result = CaipUtils.toChainId("solana", "unknown-network")
        assertNull(result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // fromChainId tests (CAIP-2 -> WinoPay)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `fromChainId maps solana devnet correctly`() {
        // Per CAIP-30: truncated to 32 chars
        val result = CaipUtils.fromChainId("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1")
        assertEquals("solana" to "devnet", result)
    }

    @Test
    fun `fromChainId maps solana mainnet correctly`() {
        // Per CAIP-30: truncated to 32 chars
        val result = CaipUtils.fromChainId("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp")
        assertEquals("solana" to "mainnet-beta", result)
    }

    @Test
    fun `fromChainId maps tron nile correctly`() {
        val result = CaipUtils.fromChainId("tron:0xcd8690dc")
        assertEquals("tron" to "tron-nile", result)
    }

    @Test
    fun `fromChainId maps tron mainnet correctly`() {
        val result = CaipUtils.fromChainId("tron:0x2b6653dc")
        assertEquals("tron" to "tron-mainnet", result)
    }

    @Test
    fun `fromChainId maps BSC mainnet correctly`() {
        val result = CaipUtils.fromChainId("eip155:56")
        assertEquals("evm" to "56", result)
    }

    @Test
    fun `fromChainId maps Ethereum mainnet correctly`() {
        val result = CaipUtils.fromChainId("eip155:1")
        assertEquals("evm" to "1", result)
    }

    @Test
    fun `fromChainId returns null for unknown namespace`() {
        val result = CaipUtils.fromChainId("unknown:chainid")
        assertNull(result)
    }

    @Test
    fun `fromChainId returns null for unknown chain reference`() {
        val result = CaipUtils.fromChainId("solana:unknowngenesishash")
        assertNull(result)
    }

    @Test
    fun `fromChainId returns null for invalid format`() {
        val result = CaipUtils.fromChainId("invalid")
        assertNull(result)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CaipAccount tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `CaipAccount fullChainId returns correct format`() {
        val account = CaipAccount(
            namespace = "eip155",
            chainId = "56",
            address = "0x123"
        )
        assertEquals("eip155:56", account.fullChainId)
    }

    @Test
    fun `CaipAccount toCaip10 returns correct format`() {
        val account = CaipAccount(
            namespace = "solana",
            chainId = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp",  // Per CAIP-30: 32 chars
            address = "WalletAddr123"
        )
        assertEquals("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp:WalletAddr123", account.toCaip10())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // getEvmNetworkDisplayName tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `getEvmNetworkDisplayName returns BSC for chain 56`() {
        assertEquals("BNB Smart Chain", CaipUtils.getEvmNetworkDisplayName("56"))
    }

    @Test
    fun `getEvmNetworkDisplayName returns Ethereum for chain 1`() {
        assertEquals("Ethereum", CaipUtils.getEvmNetworkDisplayName("1"))
    }

    @Test
    fun `getEvmNetworkDisplayName returns BSC Testnet for chain 97`() {
        assertEquals("BSC Testnet", CaipUtils.getEvmNetworkDisplayName("97"))
    }

    @Test
    fun `getEvmNetworkDisplayName returns Sepolia for chain 11155111`() {
        assertEquals("Sepolia Testnet", CaipUtils.getEvmNetworkDisplayName("11155111"))
    }

    @Test
    fun `getEvmNetworkDisplayName returns generic name for unknown chain`() {
        assertEquals("EVM Chain 999", CaipUtils.getEvmNetworkDisplayName("999"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Roundtrip tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `roundtrip toChainId and fromChainId for solana devnet`() {
        val chainId = CaipUtils.toChainId("solana", "devnet")
        assertNotNull(chainId)
        val result = CaipUtils.fromChainId(chainId!!)
        assertEquals("solana" to "devnet", result)
    }

    @Test
    fun `roundtrip toChainId and fromChainId for tron mainnet`() {
        val chainId = CaipUtils.toChainId("tron", "tron-mainnet")
        assertNotNull(chainId)
        val result = CaipUtils.fromChainId(chainId!!)
        assertEquals("tron" to "tron-mainnet", result)
    }

    @Test
    fun `roundtrip toChainId and fromChainId for BSC`() {
        val chainId = CaipUtils.toChainId("evm", "56")
        assertNotNull(chainId)
        val result = CaipUtils.fromChainId(chainId!!)
        assertEquals("evm" to "56", result)
    }

    @Test
    fun `roundtrip parseAccount and toCaip10`() {
        val original = "eip155:56:0x1234567890abcdef"
        val parsed = CaipUtils.parseAccount(original)
        assertNotNull(parsed)
        assertEquals(original, parsed!!.toCaip10())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // isValidChainId tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `isValidChainId returns true for valid eip155 chain`() {
        assertTrue(CaipUtils.isValidChainId("eip155:56"))
        assertTrue(CaipUtils.isValidChainId("eip155:1"))
        assertTrue(CaipUtils.isValidChainId("eip155:97"))
    }

    @Test
    fun `isValidChainId returns true for valid solana chain`() {
        // Per CAIP-30: truncated to 32 chars
        assertTrue(CaipUtils.isValidChainId("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"))
        assertTrue(CaipUtils.isValidChainId("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1"))
    }

    @Test
    fun `isValidChainId returns false for invalid format`() {
        assertFalse(CaipUtils.isValidChainId("invalid"))
        assertFalse(CaipUtils.isValidChainId(""))
        assertFalse(CaipUtils.isValidChainId("eip155"))
        assertFalse(CaipUtils.isValidChainId(":56"))
    }

    @Test
    fun `isValidChainId returns false for invalid namespace`() {
        // Namespace must be 3-8 lowercase alphanumeric
        assertFalse(CaipUtils.isValidChainId("ab:56")) // too short
        assertFalse(CaipUtils.isValidChainId("EIP155:56")) // uppercase
        assertFalse(CaipUtils.isValidChainId("eip-155:56")) // contains dash
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // validateChainId tests
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `validateChainId returns valid for correct solana devnet`() {
        // Per CAIP-30: truncated to 32 chars
        val result = CaipUtils.validateChainId("solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1")
        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `validateChainId returns valid for correct solana mainnet`() {
        // Per CAIP-30: truncated to 32 chars
        val result = CaipUtils.validateChainId("solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp")
        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `validateChainId returns valid for correct eip155`() {
        val result = CaipUtils.validateChainId("eip155:56")
        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `validateChainId returns valid for correct tron mainnet`() {
        val result = CaipUtils.validateChainId("tron:0x2b6653dc")
        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `validateChainId returns valid for correct tron nile`() {
        val result = CaipUtils.validateChainId("tron:0xcd8690dc")
        assertTrue(result.isValid)
        assertNull(result.error)
    }

    @Test
    fun `validateChainId returns invalid for incorrect format`() {
        val result = CaipUtils.validateChainId("invalid-no-colon")
        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid format"))
    }

    @Test
    fun `validateChainId returns invalid for bad namespace`() {
        val result = CaipUtils.validateChainId("AB:56") // uppercase, too short
        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid namespace"))
    }

    @Test
    fun `validateChainId returns invalid for bad solana reference`() {
        // Solana genesis hash should be base58, 32-44 chars
        val result = CaipUtils.validateChainId("solana:short")
        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid Solana reference"))
    }

    @Test
    fun `validateChainId returns invalid for non-numeric eip155`() {
        val result = CaipUtils.validateChainId("eip155:abc")
        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid EIP155 reference"))
    }

    @Test
    fun `validateChainId returns invalid for non-hex tron`() {
        val result = CaipUtils.validateChainId("tron:notahex")
        assertFalse(result.isValid)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid TRON reference"))
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Validation for all generated chain IDs
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `all toChainId outputs are CAIP-2 valid`() {
        // Solana
        val solanaDevnet = CaipUtils.toChainId("solana", "devnet")
        assertNotNull(solanaDevnet)
        assertTrue(CaipUtils.validateChainId(solanaDevnet!!).isValid)

        val solanaMainnet = CaipUtils.toChainId("solana", "mainnet-beta")
        assertNotNull(solanaMainnet)
        assertTrue(CaipUtils.validateChainId(solanaMainnet!!).isValid)

        // TRON
        val tronNile = CaipUtils.toChainId("tron", "tron-nile")
        assertNotNull(tronNile)
        assertTrue(CaipUtils.validateChainId(tronNile!!).isValid)

        val tronMainnet = CaipUtils.toChainId("tron", "tron-mainnet")
        assertNotNull(tronMainnet)
        assertTrue(CaipUtils.validateChainId(tronMainnet!!).isValid)

        // EVM
        val bscMainnet = CaipUtils.toChainId("evm", "56")
        assertNotNull(bscMainnet)
        assertTrue(CaipUtils.validateChainId(bscMainnet!!).isValid)

        val bscTestnet = CaipUtils.toChainId("evm", "97")
        assertNotNull(bscTestnet)
        assertTrue(CaipUtils.validateChainId(bscTestnet!!).isValid)

        val ethMainnet = CaipUtils.toChainId("evm", "1")
        assertNotNull(ethMainnet)
        assertTrue(CaipUtils.validateChainId(ethMainnet!!).isValid)

        val sepolia = CaipUtils.toChainId("evm", "11155111")
        assertNotNull(sepolia)
        assertTrue(CaipUtils.validateChainId(sepolia!!).isValid)
    }
}
