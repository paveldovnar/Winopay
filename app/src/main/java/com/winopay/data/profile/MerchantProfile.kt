package com.winopay.data.profile

/**
 * Merchant profile data model.
 *
 * MULTICHAIN-READY:
 * Profile is keyed by {railId, networkId, accountId} to support:
 * - Different wallets on same chain
 * - Same wallet on different networks (devnet/mainnet)
 * - Different blockchain rails (Solana, EVM, Tron)
 *
 * PERSISTENCE:
 * - Stored in DataStore (backed up via Android Auto Backup)
 * - Logo copied to internal storage (NOT raw content:// URI)
 *
 * @param railId Payment rail identifier (e.g., "solana", "evm", "tron")
 * @param networkId Network/cluster (e.g., "devnet", "mainnet-beta", "1", "137")
 * @param accountId Wallet address or CAIP-10 identifier
 * @param businessName Merchant business name
 * @param logoLocalPath Path to logo in internal storage (NOT content:// URI)
 * @param updatedAt Timestamp of last update
 */
data class MerchantProfile(
    val railId: String,
    val networkId: String,
    val accountId: String,
    val businessName: String,
    val logoLocalPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Generate unique profile key for storage.
         * Format: "profile:{railId}:{networkId}:{accountId}"
         */
        fun profileKey(railId: String, networkId: String, accountId: String): String {
            return "profile:$railId:$networkId:$accountId"
        }
    }

    /**
     * Get profile key for this instance.
     */
    fun profileKey(): String = profileKey(railId, networkId, accountId)
}
