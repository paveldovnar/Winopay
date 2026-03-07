package com.winopay.tron

/**
 * TRON network constants and contract addresses.
 *
 * TRC20 Token Addresses:
 * - USDT (mainnet): TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t
 * - USDC (mainnet): TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8
 *
 * Networks:
 * - Mainnet: "tron-mainnet"
 * - Nile (testnet): "tron-nile"
 * - Shasta (testnet): "tron-shasta"
 */
object TronConstants {

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // NETWORK IDS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    const val NETWORK_MAINNET = "tron-mainnet"
    const val NETWORK_NILE = "tron-nile"
    const val NETWORK_SHASTA = "tron-shasta"

    val SUPPORTED_NETWORKS = setOf(
        NETWORK_MAINNET,
        NETWORK_NILE,
        NETWORK_SHASTA
    )

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // MAINNET TOKEN CONTRACTS (TRC20)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** USDT TRC20 contract on TRON mainnet (Tether official) */
    const val MAINNET_USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"

    /** USDC TRC20 contract on TRON mainnet (Circle official) */
    const val MAINNET_USDC_CONTRACT = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // NILE TESTNET TOKEN CONTRACTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** USDT TRC20 contract on Nile testnet */
    const val NILE_USDT_CONTRACT = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj"

    /** USDC TRC20 contract on Nile testnet (if available) */
    const val NILE_USDC_CONTRACT = "" // Not always available on testnets

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TRC20 TRANSFER METHOD ID
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * TRC20 transfer method signature: transfer(address,uint256)
     * Keccak256 hash prefix (first 4 bytes): a9059cbb
     */
    const val TRC20_TRANSFER_METHOD_ID = "a9059cbb"

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TOKEN DECIMALS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /** USDT on TRON uses 6 decimals */
    const val USDT_DECIMALS = 6

    /** USDC on TRON uses 6 decimals */
    const val USDC_DECIMALS = 6

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // HELPER FUNCTIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Get USDT contract address for a given network.
     */
    fun getUsdtContract(networkId: String): String {
        return when (networkId) {
            NETWORK_MAINNET -> MAINNET_USDT_CONTRACT
            NETWORK_NILE -> NILE_USDT_CONTRACT
            else -> MAINNET_USDT_CONTRACT // Default to mainnet
        }
    }

    /**
     * Get USDC contract address for a given network.
     */
    fun getUsdcContract(networkId: String): String {
        return when (networkId) {
            NETWORK_MAINNET -> MAINNET_USDC_CONTRACT
            NETWORK_NILE -> NILE_USDC_CONTRACT
            else -> MAINNET_USDC_CONTRACT // Default to mainnet
        }
    }

    /**
     * Check if a contract address is a known stablecoin on the given network.
     */
    fun isKnownStablecoin(contract: String, networkId: String): Boolean {
        return contract == getUsdtContract(networkId) || contract == getUsdcContract(networkId)
    }

    /**
     * Get human-readable symbol for a token contract.
     */
    fun getTokenSymbol(contract: String?, networkId: String = NETWORK_MAINNET): String {
        if (contract == null) return "TRX"
        return when (contract) {
            getUsdtContract(networkId) -> "USDT"
            getUsdcContract(networkId) -> "USDC"
            else -> "TRC20"
        }
    }

    /**
     * Check if a network ID is a TRON network.
     */
    fun supportsNetwork(networkId: String): Boolean {
        return networkId.lowercase() in SUPPORTED_NETWORKS.map { it.lowercase() }
    }
}
