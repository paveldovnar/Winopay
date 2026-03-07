package com.winopay.data.profile

/**
 * Represents a connected payment rail.
 *
 * MULTICHAIN:
 * Each rail connection stores the wallet address and network info.
 * Multiple rails can be connected simultaneously (Solana + TRON + EVM).
 *
 * SESSION SUPPORT:
 * When connected via WalletConnect/Reown, the sessionId is stored to enable
 * future signing operations (refunds, etc.) without reconnecting.
 *
 * @param railId Payment rail identifier (e.g., "solana", "tron", "evm")
 * @param networkId Network/cluster (e.g., "devnet", "mainnet-beta", "tron-nile", "56")
 * @param accountId Wallet address
 * @param connectedAt Timestamp when this rail was connected
 * @param sessionId WalletConnect/Reown session topic (optional, for signing support)
 */
data class RailConnection(
    val railId: String,
    val networkId: String,
    val accountId: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val sessionId: String? = null
) {
    companion object {
        /**
         * Serialize RailConnection to storage string.
         * Format: "railId|networkId|accountId|connectedAt|sessionId"
         * (sessionId is optional for backward compatibility)
         */
        fun serialize(connection: RailConnection): String {
            val base = "${connection.railId}|${connection.networkId}|${connection.accountId}|${connection.connectedAt}"
            return if (connection.sessionId != null) {
                "$base|${connection.sessionId}"
            } else {
                base
            }
        }

        /**
         * Deserialize RailConnection from storage string.
         * Backward compatible: handles both 4-field (old) and 5-field (new) format.
         */
        fun deserialize(data: String): RailConnection? {
            val parts = data.split("|")
            if (parts.size < 4) return null
            return try {
                RailConnection(
                    railId = parts[0],
                    networkId = parts[1],
                    accountId = parts[2],
                    connectedAt = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                    sessionId = parts.getOrNull(4)?.takeIf { it.isNotBlank() }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Global business profile (independent of rails).
 *
 * SINGLE SOURCE OF TRUTH:
 * Business name and logo are global - they don't change when switching rails.
 * This ensures consistent merchant identity across all payment methods.
 *
 * @param businessName Merchant business name
 * @param logoLocalPath Path to logo in internal storage
 * @param updatedAt Timestamp of last update
 */
data class BusinessProfile(
    val businessName: String,
    val logoLocalPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
