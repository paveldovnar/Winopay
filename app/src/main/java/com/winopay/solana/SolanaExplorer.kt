package com.winopay.solana

import com.winopay.BuildConfig

/**
 * Utility for building Solana Explorer URLs.
 * Cluster-aware based on BuildConfig.SOLANA_CLUSTER.
 */
object SolanaExplorer {

    private const val BASE_URL = "https://explorer.solana.com"

    /**
     * Build transaction URL for Solana Explorer.
     *
     * @param signature Transaction signature
     * @return Full explorer URL with appropriate cluster parameter
     */
    fun buildTxUrl(signature: String): String {
        return when (BuildConfig.SOLANA_CLUSTER) {
            "devnet" -> "$BASE_URL/tx/$signature?cluster=devnet"
            "testnet" -> "$BASE_URL/tx/$signature?cluster=testnet"
            else -> "$BASE_URL/tx/$signature" // mainnet-beta, no cluster param needed
        }
    }

    /**
     * Build address URL for Solana Explorer.
     *
     * @param address Account address (base58)
     * @return Full explorer URL with appropriate cluster parameter
     */
    fun buildAddressUrl(address: String): String {
        return when (BuildConfig.SOLANA_CLUSTER) {
            "devnet" -> "$BASE_URL/address/$address?cluster=devnet"
            "testnet" -> "$BASE_URL/address/$address?cluster=testnet"
            else -> "$BASE_URL/address/$address"
        }
    }
}
