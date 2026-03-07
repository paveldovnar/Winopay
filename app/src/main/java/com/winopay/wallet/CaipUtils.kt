package com.winopay.wallet

import com.winopay.BuildConfig

/**
 * CAIP-2 and CAIP-10 utilities for WalletConnect chain and account identification.
 *
 * CAIP-2: Chain ID format "namespace:reference"
 * CAIP-10: Account ID format "namespace:reference:address"
 *
 * References:
 * - https://github.com/ChainAgnostic/CAIPs/blob/main/CAIPs/caip-2.md
 * - https://github.com/ChainAgnostic/CAIPs/blob/main/CAIPs/caip-10.md
 */
object CaipUtils {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CAIP-2 Chain ID constants
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // Solana genesis hashes (used as chain reference in CAIP-2)
    // NOTE: Per CAIP-30, genesis hash MUST be truncated to first 32 characters
    // Full mainnet hash: 5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d (43 chars)
    // Full devnet hash:  EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG (43 chars)
    private const val SOLANA_MAINNET_GENESIS = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"  // First 32 chars
    private const val SOLANA_DEVNET_GENESIS = "EtWTRABZaYq6iMfeYKouRu166VU2xqa1"   // First 32 chars

    // TRON chain IDs (hex format)
    private const val TRON_MAINNET_CHAIN_ID = "0x2b6653dc"
    private const val TRON_NILE_CHAIN_ID = "0xcd8690dc"

    // EVM chain IDs
    const val BSC_MAINNET_CHAIN_ID = "56"
    const val BSC_TESTNET_CHAIN_ID = "97"
    const val ETHEREUM_MAINNET_CHAIN_ID = "1"
    const val SEPOLIA_CHAIN_ID = "11155111"

    // Namespaces
    const val NAMESPACE_SOLANA = "solana"
    const val NAMESPACE_TRON = "tron"
    const val NAMESPACE_EIP155 = "eip155"

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CAIP-10 Account parsing
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Parse a CAIP-10 account identifier.
     *
     * Format: "namespace:chainId:address"
     * Examples:
     * - "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d:ABC123..."
     * - "tron:0x2b6653dc:TXxx..."
     * - "eip155:56:0x1234..."
     *
     * @param caip10 The CAIP-10 account identifier
     * @return Parsed CaipAccount or null if invalid
     */
    fun parseAccount(caip10: String): CaipAccount? {
        val parts = caip10.split(":")
        if (parts.size < 3) return null

        val namespace = parts[0]
        val chainId = parts[1]
        // Address may contain colons (unlikely but spec allows), so join remaining parts
        val address = parts.drop(2).joinToString(":")

        if (namespace.isBlank() || chainId.isBlank() || address.isBlank()) {
            return null
        }

        return CaipAccount(
            namespace = namespace,
            chainId = chainId,
            address = address
        )
    }

    /**
     * Build a CAIP-2 chain ID from namespace and reference.
     *
     * @param namespace The namespace (e.g., "solana", "eip155", "tron")
     * @param reference The chain reference (e.g., genesis hash, chain ID)
     * @return CAIP-2 chain ID (e.g., "solana:devnet-genesis-hash")
     */
    fun buildChainId(namespace: String, reference: String): String {
        return "$namespace:$reference"
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CAIP-2 Validation
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Validate a CAIP-2 chain ID format.
     *
     * CAIP-2 format: namespace:reference
     * - namespace: 3-8 lowercase alphanumeric characters
     * - reference: 1-64 alphanumeric or special characters
     *
     * @param chainId The chain ID to validate
     * @return true if valid CAIP-2 format
     */
    fun isValidChainId(chainId: String): Boolean {
        val parts = chainId.split(":")
        if (parts.size != 2) return false

        val namespace = parts[0]
        val reference = parts[1]

        // Namespace: 3-8 lowercase alphanumeric
        val namespaceRegex = "^[a-z0-9]{3,8}$".toRegex()
        if (!namespace.matches(namespaceRegex)) return false

        // Reference: 1-64 alphanumeric, or special chars for some chains
        // Relaxed: allow alphanumeric, dash, underscore, hex prefix (0x)
        val referenceRegex = "^[a-zA-Z0-9_-]{1,64}$".toRegex()
        if (!reference.matches(referenceRegex)) return false

        return true
    }

    /**
     * Validate chain IDs for known namespaces with specific rules.
     *
     * @param chainId The chain ID to validate
     * @return ValidationResult with status and error message if invalid
     */
    fun validateChainId(chainId: String): ValidationResult {
        val parts = chainId.split(":")
        if (parts.size != 2) {
            return ValidationResult(false, "Invalid format: expected 'namespace:reference', got '$chainId'")
        }

        val namespace = parts[0]
        val reference = parts[1]

        // Check namespace format
        val namespaceRegex = "^[a-z0-9]{3,8}$".toRegex()
        if (!namespace.matches(namespaceRegex)) {
            return ValidationResult(false, "Invalid namespace '$namespace': must be 3-8 lowercase alphanumeric chars")
        }

        // Check reference based on namespace
        return when (namespace) {
            NAMESPACE_SOLANA -> {
                // Solana uses base58 genesis hash truncated to exactly 32 chars per CAIP-30
                if (reference.length != 32) {
                    ValidationResult(false, "Invalid Solana reference '$reference': expected 32-char truncated genesis hash, got ${reference.length} chars")
                } else if (!reference.matches("^[1-9A-HJ-NP-Za-km-z]+$".toRegex())) {
                    ValidationResult(false, "Invalid Solana reference '$reference': not valid base58")
                } else {
                    ValidationResult(true)
                }
            }
            NAMESPACE_EIP155 -> {
                // EIP155 uses numeric chain ID
                if (!reference.matches("^[0-9]+$".toRegex())) {
                    ValidationResult(false, "Invalid EIP155 reference '$reference': must be numeric chain ID")
                } else {
                    ValidationResult(true)
                }
            }
            NAMESPACE_TRON -> {
                // TRON uses hex chain ID (0x prefixed)
                if (!reference.matches("^0x[0-9a-fA-F]+$".toRegex())) {
                    ValidationResult(false, "Invalid TRON reference '$reference': must be hex chain ID (0x...)")
                } else {
                    ValidationResult(true)
                }
            }
            else -> {
                // Unknown namespace - just check reference length
                if (reference.isEmpty() || reference.length > 64) {
                    ValidationResult(false, "Invalid reference '$reference': must be 1-64 chars")
                } else {
                    ValidationResult(true)
                }
            }
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null
    )

    /**
     * Single source of truth for getting CAIP-2 chain ID.
     * Alias for toChainId() - use this everywhere to ensure consistency.
     *
     * @param railId Our rail identifier (e.g., "solana", "tron", "evm")
     * @param networkId Our network identifier (e.g., "devnet", "tron-mainnet", "56")
     * @return CAIP-2 chain ID or null if unmapped
     */
    fun chainIdForRail(railId: String, networkId: String): String? = toChainId(railId, networkId)

    /**
     * Get all known valid CAIP-2 chain IDs for debugging.
     * These are the exact strings that should be sent to WalletConnect.
     */
    fun getAllKnownChainIds(): Map<String, String> = mapOf(
        "solana:mainnet-beta" to "solana:$SOLANA_MAINNET_GENESIS",
        "solana:devnet" to "solana:$SOLANA_DEVNET_GENESIS",
        "tron:tron-mainnet" to "tron:$TRON_MAINNET_CHAIN_ID",
        "tron:tron-nile" to "tron:$TRON_NILE_CHAIN_ID",
        "evm:56" to "eip155:$BSC_MAINNET_CHAIN_ID",
        "evm:97" to "eip155:$BSC_TESTNET_CHAIN_ID",
        "evm:1" to "eip155:$ETHEREUM_MAINNET_CHAIN_ID",
        "evm:11155111" to "eip155:$SEPOLIA_CHAIN_ID"
    )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // WinoPay <-> CAIP-2 mapping
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Convert WinoPay railId/networkId to CAIP-2 chain ID.
     *
     * CAIP-2 compliant mappings (reference ≤ 32 chars):
     * - solana/devnet → solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1
     * - solana/mainnet-beta → solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp
     * - tron/tron-nile → tron:0xcd8690dc
     * - tron/tron-mainnet → tron:0x2b6653dc
     * - evm/97 → eip155:97
     * - evm/56 → eip155:56
     * - evm/1 → eip155:1
     *
     * @param railId Our rail identifier (e.g., "solana", "tron", "evm")
     * @param networkId Our network identifier (e.g., "devnet", "tron-mainnet", "56")
     * @return CAIP-2 chain ID or null if unmapped
     */
    fun toChainId(railId: String, networkId: String): String? {
        return when (railId) {
            "solana" -> {
                when (networkId) {
                    "devnet" -> buildChainId(NAMESPACE_SOLANA, SOLANA_DEVNET_GENESIS)
                    "mainnet-beta" -> buildChainId(NAMESPACE_SOLANA, SOLANA_MAINNET_GENESIS)
                    else -> null
                }
            }
            "tron" -> {
                when (networkId) {
                    "tron-nile" -> buildChainId(NAMESPACE_TRON, TRON_NILE_CHAIN_ID)
                    "tron-mainnet" -> buildChainId(NAMESPACE_TRON, TRON_MAINNET_CHAIN_ID)
                    else -> null
                }
            }
            "evm" -> {
                // For EVM, networkId IS the chain ID
                when (networkId) {
                    BSC_MAINNET_CHAIN_ID, BSC_TESTNET_CHAIN_ID,
                    ETHEREUM_MAINNET_CHAIN_ID, SEPOLIA_CHAIN_ID -> {
                        buildChainId(NAMESPACE_EIP155, networkId)
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Convert CAIP-2 chain ID to WinoPay railId/networkId.
     *
     * @param chainId CAIP-2 chain ID (e.g., "eip155:56")
     * @return Pair of (railId, networkId) or null if unmapped
     */
    fun fromChainId(chainId: String): Pair<String, String>? {
        val parts = chainId.split(":")
        if (parts.size != 2) return null

        val namespace = parts[0]
        val reference = parts[1]

        return when (namespace) {
            NAMESPACE_SOLANA -> {
                when (reference) {
                    SOLANA_DEVNET_GENESIS -> "solana" to "devnet"
                    SOLANA_MAINNET_GENESIS -> "solana" to "mainnet-beta"
                    else -> null
                }
            }
            NAMESPACE_TRON -> {
                when (reference) {
                    TRON_NILE_CHAIN_ID -> "tron" to "tron-nile"
                    TRON_MAINNET_CHAIN_ID -> "tron" to "tron-mainnet"
                    else -> null
                }
            }
            NAMESPACE_EIP155 -> {
                // For EVM, the reference IS the networkId
                when (reference) {
                    BSC_MAINNET_CHAIN_ID, BSC_TESTNET_CHAIN_ID,
                    ETHEREUM_MAINNET_CHAIN_ID, SEPOLIA_CHAIN_ID -> {
                        "evm" to reference
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Helper functions for building session namespaces
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Get supported Solana chain IDs based on build flavor.
     */
    fun getSolanaChainIds(): List<String> {
        return when (BuildConfig.SOLANA_CLUSTER) {
            "devnet" -> listOf(buildChainId(NAMESPACE_SOLANA, SOLANA_DEVNET_GENESIS))
            "mainnet-beta" -> listOf(buildChainId(NAMESPACE_SOLANA, SOLANA_MAINNET_GENESIS))
            else -> emptyList()
        }
    }

    /**
     * Get supported TRON chain IDs based on build flavor.
     */
    fun getTronChainIds(): List<String> {
        return when (BuildConfig.TRON_NETWORK) {
            "tron-nile" -> listOf(buildChainId(NAMESPACE_TRON, TRON_NILE_CHAIN_ID))
            "tron-mainnet" -> listOf(buildChainId(NAMESPACE_TRON, TRON_MAINNET_CHAIN_ID))
            else -> emptyList()
        }
    }

    /**
     * Get supported EVM chain IDs based on build flavor.
     * Returns BSC + Ethereum for mainnet, BSC Testnet + Sepolia for devnet.
     */
    fun getEvmChainIds(): List<String> {
        return when (BuildConfig.SOLANA_CLUSTER) {
            "devnet" -> listOf(
                buildChainId(NAMESPACE_EIP155, BSC_TESTNET_CHAIN_ID),
                buildChainId(NAMESPACE_EIP155, SEPOLIA_CHAIN_ID)
            )
            "mainnet-beta" -> listOf(
                buildChainId(NAMESPACE_EIP155, BSC_MAINNET_CHAIN_ID),
                buildChainId(NAMESPACE_EIP155, ETHEREUM_MAINNET_CHAIN_ID)
            )
            else -> emptyList()
        }
    }

    /**
     * Get the primary EVM network ID for the current build.
     * BSC for mainnet, BSC Testnet for devnet.
     */
    fun getPrimaryEvmNetworkId(): String {
        return when (BuildConfig.SOLANA_CLUSTER) {
            "devnet" -> BSC_TESTNET_CHAIN_ID
            "mainnet-beta" -> BSC_MAINNET_CHAIN_ID
            else -> BSC_MAINNET_CHAIN_ID
        }
    }

    /**
     * Get display name for an EVM chain ID.
     */
    fun getEvmNetworkDisplayName(chainId: String): String {
        return when (chainId) {
            BSC_MAINNET_CHAIN_ID -> "BNB Smart Chain"
            BSC_TESTNET_CHAIN_ID -> "BSC Testnet"
            ETHEREUM_MAINNET_CHAIN_ID -> "Ethereum"
            SEPOLIA_CHAIN_ID -> "Sepolia Testnet"
            else -> "EVM Chain $chainId"
        }
    }
}

/**
 * Parsed CAIP-10 account.
 *
 * @param namespace The blockchain namespace (e.g., "solana", "eip155", "tron")
 * @param chainId The chain reference within the namespace
 * @param address The wallet address on this chain
 */
data class CaipAccount(
    val namespace: String,
    val chainId: String,
    val address: String
) {
    /**
     * Get the full CAIP-2 chain ID.
     */
    val fullChainId: String
        get() = "$namespace:$chainId"

    /**
     * Convert back to CAIP-10 format.
     */
    fun toCaip10(): String = "$namespace:$chainId:$address"
}
