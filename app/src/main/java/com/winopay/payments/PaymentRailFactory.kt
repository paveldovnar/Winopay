package com.winopay.payments

import android.util.Log
import com.winopay.tron.TronConstants

/**
 * Factory for creating PaymentRail instances based on rail ID or network ID.
 *
 * SUPPORTED RAILS:
 * - solana: Solana (devnet, mainnet, mainnet-beta, testnet)
 * - tron: TRON (tron-mainnet, tron-nile, tron-shasta)
 * - evm: Ethereum/Polygon/BSC (STUB - not yet implemented)
 *
 * USAGE:
 * - getRailByRailId("solana") -> SolanaPaymentRail
 * - getRailByRailId("tron") -> TronPaymentRail
 * - getRailByRailId("evm") -> EvmPaymentRail (stub)
 * - getRail("devnet") -> SolanaPaymentRail (legacy network-based lookup)
 * - getRail("tron-mainnet") -> TronPaymentRail
 *
 * Future networks will be added here without modifying business logic.
 */
object PaymentRailFactory {

    private const val TAG = "PaymentRailFactory"

    // Singleton instances (rails are stateless wrappers, safe to reuse)
    private val solanaRail by lazy { SolanaPaymentRail() }

    // TRON rails by network (different networks have different contract addresses)
    private val tronRails = mutableMapOf<String, TronPaymentRail>()

    /**
     * Get PaymentRail by rail ID (preferred method).
     *
     * @param railId Rail identifier (e.g., "solana", "evm", "tron")
     * @param networkId Optional network ID for rails that need it (e.g., TRON)
     * @return PaymentRail implementation
     * @throws IllegalArgumentException if rail is not supported
     */
    fun getRailByRailId(railId: String, networkId: String? = null): PaymentRail {
        Log.d(TAG, "Getting rail for railId: $railId, networkId: $networkId")

        return when (railId) {
            SolanaPaymentRail.RAIL_ID -> {
                Log.d(TAG, "Using SolanaPaymentRail")
                solanaRail
            }
            TronPaymentRail.RAIL_ID -> {
                // TRON needs network ID for contract addresses
                val network = networkId ?: TronConstants.NETWORK_MAINNET
                Log.d(TAG, "Using TronPaymentRail for network: $network")
                getTronRail(network)
            }
            EvmPaymentRail.RAIL_ID -> {
                Log.w(TAG, "EVM rail requested but not yet implemented")
                throw UnsupportedOperationException("EVM rail not yet implemented")
            }
            else -> {
                Log.e(TAG, "Unsupported rail: $railId")
                throw IllegalArgumentException("Unsupported rail: $railId")
            }
        }
    }

    /**
     * Get or create TronPaymentRail for a network.
     */
    private fun getTronRail(networkId: String): TronPaymentRail {
        return tronRails.getOrPut(networkId) {
            TronPaymentRail(networkId)
        }
    }

    /**
     * Get PaymentRail for the given network ID (legacy method).
     *
     * @param networkId Network identifier (e.g., "devnet", "mainnet", "mainnet-beta", "tron-mainnet", "1", "137")
     * @return PaymentRail implementation for the network
     * @throws IllegalArgumentException if network is not supported
     */
    fun getRail(networkId: String): PaymentRail {
        Log.d(TAG, "Getting rail for networkId: $networkId")

        return when {
            SolanaPaymentRail.supportsNetwork(networkId) -> {
                Log.d(TAG, "Using SolanaPaymentRail for network: $networkId")
                solanaRail
            }
            TronPaymentRail.supportsNetwork(networkId) -> {
                Log.d(TAG, "Using TronPaymentRail for network: $networkId")
                getTronRail(networkId)
            }
            EvmPaymentRail.supportsNetwork(networkId) -> {
                Log.w(TAG, "EVM network $networkId requested but not yet implemented")
                throw UnsupportedOperationException("EVM rail not yet implemented for network: $networkId")
            }
            else -> {
                Log.e(TAG, "Unsupported network: $networkId")
                throw IllegalArgumentException("Unsupported network: $networkId")
            }
        }
    }

    /**
     * Get PaymentRail for the given network ID, or null if not supported.
     *
     * @param networkId Network identifier
     * @return PaymentRail or null if network is not supported
     */
    fun getRailOrNull(networkId: String): PaymentRail? {
        return try {
            getRail(networkId)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Check if a rail ID is supported.
     *
     * @param railId Rail identifier
     * @return true if rail is supported (even if stub)
     */
    fun isRailSupported(railId: String): Boolean {
        return railId == SolanaPaymentRail.RAIL_ID ||
               railId == TronPaymentRail.RAIL_ID ||
               railId == EvmPaymentRail.RAIL_ID
    }

    /**
     * Check if a rail is implemented (not just a stub).
     *
     * @param railId Rail identifier
     * @return true if rail is fully implemented
     */
    fun isRailImplemented(railId: String): Boolean {
        return railId == SolanaPaymentRail.RAIL_ID || railId == TronPaymentRail.RAIL_ID
        // EVM is stub-only, not yet implemented
    }

    /**
     * Check if a network is supported.
     *
     * @param networkId Network identifier
     * @return true if network is supported
     */
    fun isNetworkSupported(networkId: String): Boolean {
        return SolanaPaymentRail.supportsNetwork(networkId) ||
               TronPaymentRail.supportsNetwork(networkId) ||
               EvmPaymentRail.supportsNetwork(networkId)
    }

    /**
     * Get the default PaymentRail (Solana).
     * Used when network is not specified.
     */
    fun getDefaultRail(): PaymentRail {
        return solanaRail
    }

    /**
     * Get all supported network IDs (all rails).
     */
    fun getSupportedNetworks(): Set<String> {
        return SolanaPaymentRail.SUPPORTED_NETWORKS +
               TronConstants.SUPPORTED_NETWORKS +
               EvmPaymentRail.SUPPORTED_NETWORKS
    }

    /**
     * Get all supported rail IDs.
     */
    fun getSupportedRails(): Set<String> {
        return setOf(
            SolanaPaymentRail.RAIL_ID,
            TronPaymentRail.RAIL_ID,
            EvmPaymentRail.RAIL_ID
        )
    }

    /**
     * Get all implemented (non-stub) rail IDs.
     */
    fun getImplementedRails(): Set<String> {
        return setOf(SolanaPaymentRail.RAIL_ID, TronPaymentRail.RAIL_ID)
    }
}
