package com.winopay.solana.rpc

import android.util.Log
import com.winopay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Multi-RPC failover manager for Solana payment detection.
 *
 * PRODUCTION-GRADE RELIABILITY:
 * 1. Primary provider from BuildConfig
 * 2. Cluster-specific fallback providers
 * 3. 3-second timeout per provider (fast failover)
 * 4. Remembers last successful provider for session
 * 5. Uses same provider for getSignatures + getTransaction (consistency)
 *
 * PROVIDER LISTS:
 * Devnet:
 *   1. BuildConfig.SOLANA_RPC_URL (primary)
 *   2. https://api.devnet.solana.com
 *   3. https://rpc.ankr.com/solana_devnet
 *
 * Mainnet:
 *   1. BuildConfig.SOLANA_RPC_URL (primary)
 *   2. https://api.mainnet-beta.solana.com
 *   3. https://rpc.ankr.com/solana
 *   4. https://solana-mainnet.g.alchemy.com/v2/demo (public demo)
 */
object RpcProviderManager {

    private const val TAG = "RpcProviderManager"
    private const val TIMEOUT_MS = 3000  // 3 seconds per provider

    /**
     * DEBUG-ONLY: Simulate primary RPC failure to test failover.
     * Set to true to force Primary provider to return TIMEOUT failure.
     * This flag is ONLY evaluated when BuildConfig.DEBUG is true.
     */
    private const val SIMULATE_PRIMARY_FAIL = false

    // Cluster from BuildConfig
    private val cluster: String = BuildConfig.SOLANA_CLUSTER

    // Primary RPC URL from BuildConfig
    private val primaryUrl: String = BuildConfig.SOLANA_RPC_URL

    // Backup RPC URL from BuildConfig (for failover)
    private val backupUrl: String = BuildConfig.SOLANA_RPC_URL_BACKUP

    /**
     * Ordered list of providers for current cluster.
     * First provider is primary (from BuildConfig), rest are fallbacks.
     */
    val providers: List<SolanaRpcProvider> by lazy {
        buildProviderList()
    }

    /**
     * Last successful provider (remembered for session).
     * Helps avoid unnecessary failover when primary is blocked.
     */
    @Volatile
    private var lastSuccessfulProvider: SolanaRpcProvider? = null

    /**
     * Build cluster-specific provider list.
     */
    private fun buildProviderList(): List<SolanaRpcProvider> {
        val list = mutableListOf<SolanaRpcProvider>()

        // 1. Primary from BuildConfig (or env override)
        list.add(StandardRpcProvider("Primary", primaryUrl, TIMEOUT_MS))

        // 2. Backup from BuildConfig (if different from primary)
        if (backupUrl.isNotBlank() && backupUrl != primaryUrl) {
            list.add(StandardRpcProvider("Backup", backupUrl, TIMEOUT_MS))
        }

        // 3. Cluster-specific additional fallbacks
        when {
            cluster.contains("devnet", ignoreCase = true) -> {
                // Devnet fallbacks
                if (!primaryUrl.contains("rpc.ankr.com") && !backupUrl.contains("rpc.ankr.com")) {
                    list.add(StandardRpcProvider("Ankr Devnet", "https://rpc.ankr.com/solana_devnet", TIMEOUT_MS))
                }
            }
            cluster.contains("mainnet", ignoreCase = true) -> {
                // Mainnet fallbacks
                if (!primaryUrl.contains("rpc.ankr.com") && !backupUrl.contains("rpc.ankr.com")) {
                    list.add(StandardRpcProvider("Ankr Mainnet", "https://rpc.ankr.com/solana", TIMEOUT_MS))
                }
                if (!primaryUrl.contains("helius") && !backupUrl.contains("helius")) {
                    list.add(StandardRpcProvider("Helius Public", "https://mainnet.helius-rpc.com/?api-key=1d8740dc-e5f4-421c-b823-e1bad1889eff", TIMEOUT_MS))
                }
            }
            else -> {
                Log.w(TAG, "Unknown cluster: $cluster, using primary/backup only")
            }
        }

        Log.i(TAG, "━━━━━ RPC PROVIDERS INITIALIZED ━━━━━")
        Log.i(TAG, "  cluster: $cluster")
        Log.i(TAG, "  primary: $primaryUrl")
        Log.i(TAG, "  backup:  ${if (backupUrl.isNotBlank() && backupUrl != primaryUrl) backupUrl else "(same as primary)"}")
        Log.i(TAG, "  chain:   ${list.map { it.name }.joinToString(" → ")}")
        if (BuildConfig.DEBUG && SIMULATE_PRIMARY_FAIL) {
            Log.w(TAG, "  ⚠️ DEBUG: SIMULATE_PRIMARY_FAIL=true (Primary will fail!)")
        }
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return list
    }

    /**
     * Get signatures for address with multi-provider failover.
     *
     * @return Success result with data AND the provider that succeeded,
     *         or final Failure if all providers fail.
     */
    suspend fun getSignaturesForAddress(
        address: String,
        limit: Int = 10
    ): RpcCallResult<List<SignatureInfo>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "━━━━━ RPC FAILOVER: getSignaturesForAddress ━━━━━")
        Log.d(TAG, "  cluster: $cluster")
        Log.d(TAG, "  address: $address")
        Log.d(TAG, "  limit: $limit")

        // Try last successful provider first (if available)
        val orderedProviders = getOrderedProviders()
        val failedProviders = mutableListOf<String>()

        for ((index, provider) in orderedProviders.withIndex()) {
            val providerNum = index + 1
            Log.d(TAG, "  Provider $providerNum: ${provider.name}")

            // DEBUG-ONLY: Simulate primary failure for testing failover
            if (shouldSimulatePrimaryFailure(provider.name)) {
                Log.w(TAG, "  Provider $providerNum: ${provider.name} → SIMULATED FAIL (debug)")
                failedProviders.add("${provider.name}: SIMULATED_TIMEOUT (debug)")
                continue
            }

            val result = provider.getSignaturesForAddress(address, limit)

            when (result) {
                is RpcCallResult.Success -> {
                    Log.d(TAG, "  Provider $providerNum: ${provider.name} → SUCCESS")
                    Log.d(TAG, "    signatures: ${result.data.size}")
                    Log.d(TAG, "  selectedProvider: ${provider.name}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    // Remember successful provider
                    lastSuccessfulProvider = provider
                    return@withContext result
                }
                is RpcCallResult.Failure -> {
                    val reason = "${result.errorType}: ${result.message}"
                    Log.w(TAG, "  Provider $providerNum: ${provider.name} → FAIL ($reason)")
                    failedProviders.add("${provider.name}: $reason")
                }
            }
        }

        // All providers failed
        Log.e(TAG, "  ALL PROVIDERS FAILED")
        failedProviders.forEachIndexed { i, reason ->
            Log.e(TAG, "    ${i + 1}. $reason")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        RpcCallResult.Failure(
            errorType = RpcErrorType.NETWORK_ERROR,
            message = "All ${providers.size} RPC providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Get transaction with multi-provider failover.
     *
     * @param preferredProvider If provided, try this provider first (for consistency
     *                          when validating a signature found by this provider).
     */
    suspend fun getTransaction(
        signature: String,
        preferredProvider: SolanaRpcProvider? = null
    ): RpcCallResult<JSONObject?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "━━━━━ RPC FAILOVER: getTransaction ━━━━━")
        Log.d(TAG, "  signature: ${signature.take(20)}...")
        if (preferredProvider != null) {
            Log.d(TAG, "  preferredProvider: ${preferredProvider.name}")
        }

        // Build provider order: preferred first, then others
        val orderedProviders = if (preferredProvider != null) {
            listOf(preferredProvider) + providers.filter { it !== preferredProvider }
        } else {
            getOrderedProviders()
        }

        val failedProviders = mutableListOf<String>()

        for ((index, provider) in orderedProviders.withIndex()) {
            val providerNum = index + 1
            Log.d(TAG, "  Provider $providerNum: ${provider.name}")

            // DEBUG-ONLY: Simulate primary failure for testing failover
            if (shouldSimulatePrimaryFailure(provider.name)) {
                Log.w(TAG, "  Provider $providerNum: ${provider.name} → SIMULATED FAIL (debug)")
                failedProviders.add("${provider.name}: SIMULATED_TIMEOUT (debug)")
                continue
            }

            val result = provider.getTransaction(signature)

            when (result) {
                is RpcCallResult.Success -> {
                    Log.d(TAG, "  Provider $providerNum: ${provider.name} → SUCCESS")
                    Log.d(TAG, "  selectedProvider: ${provider.name}")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    lastSuccessfulProvider = provider
                    return@withContext result
                }
                is RpcCallResult.Failure -> {
                    val reason = "${result.errorType}: ${result.message}"
                    Log.w(TAG, "  Provider $providerNum: ${provider.name} → FAIL ($reason)")
                    failedProviders.add("${provider.name}: $reason")
                }
            }
        }

        Log.e(TAG, "  ALL PROVIDERS FAILED")
        failedProviders.forEachIndexed { i, reason ->
            Log.e(TAG, "    ${i + 1}. $reason")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        RpcCallResult.Failure(
            errorType = RpcErrorType.NETWORK_ERROR,
            message = "All ${providers.size} RPC providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Check confirmation status with failover.
     */
    suspend fun getSignatureStatus(
        signature: String,
        preferredProvider: SolanaRpcProvider? = null
    ): RpcCallResult<ConfirmationStatus> = withContext(Dispatchers.IO) {
        val orderedProviders = if (preferredProvider != null) {
            listOf(preferredProvider) + providers.filter { it !== preferredProvider }
        } else {
            getOrderedProviders()
        }

        for ((index, provider) in orderedProviders.withIndex()) {
            // DEBUG-ONLY: Simulate primary failure for testing failover
            if (shouldSimulatePrimaryFailure(provider.name)) {
                Log.w(TAG, "  getSignatureStatus Provider ${index + 1}: ${provider.name} → SIMULATED FAIL (debug)")
                continue
            }

            val result = provider.getSignatureStatus(signature)
            if (result is RpcCallResult.Success) {
                lastSuccessfulProvider = provider
                return@withContext result
            }
        }

        RpcCallResult.Failure(
            errorType = RpcErrorType.NETWORK_ERROR,
            message = "All providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Discover token accounts by owner with multi-provider failover.
     *
     * FALLBACK DETECTION STRATEGY:
     * When ATA derivation fails to find payments, this discovers ALL token accounts
     * for the owner (optionally filtered by mint) to find non-standard accounts.
     *
     * @param owner Owner wallet address
     * @param mint Token mint address (null for all SPL tokens)
     * @return List of discovered token accounts
     */
    suspend fun getTokenAccountsByOwner(
        owner: String,
        mint: String?
    ): RpcCallResult<List<DiscoveredTokenAccount>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "━━━━━ RPC FAILOVER: getTokenAccountsByOwner ━━━━━")
        Log.d(TAG, "  owner: ${owner.take(12)}...")
        Log.d(TAG, "  mint: ${mint?.take(12) ?: "(all SPL tokens)"}...")

        val orderedProviders = getOrderedProviders()
        val failedProviders = mutableListOf<String>()

        for ((index, provider) in orderedProviders.withIndex()) {
            val providerNum = index + 1
            Log.d(TAG, "  Provider $providerNum: ${provider.name}")

            if (shouldSimulatePrimaryFailure(provider.name)) {
                Log.w(TAG, "  Provider $providerNum: ${provider.name} → SIMULATED FAIL (debug)")
                failedProviders.add("${provider.name}: SIMULATED_TIMEOUT (debug)")
                continue
            }

            val result = provider.getTokenAccountsByOwner(owner, mint)

            when (result) {
                is RpcCallResult.Success -> {
                    Log.d(TAG, "  Provider $providerNum: ${provider.name} → SUCCESS")
                    Log.d(TAG, "    accounts found: ${result.data.size}")
                    result.data.forEach { account ->
                        Log.d(TAG, "      ${account.address.take(12)}... mint=${account.mint.take(12)}... balance=${account.balance}")
                    }
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    lastSuccessfulProvider = provider
                    return@withContext result
                }
                is RpcCallResult.Failure -> {
                    val reason = "${result.errorType}: ${result.message}"
                    Log.w(TAG, "  Provider $providerNum: ${provider.name} → FAIL ($reason)")
                    failedProviders.add("${provider.name}: $reason")
                }
            }
        }

        Log.e(TAG, "  ALL PROVIDERS FAILED")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        RpcCallResult.Failure(
            errorType = RpcErrorType.NETWORK_ERROR,
            message = "All ${providers.size} RPC providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Get providers ordered with last successful first.
     */
    private fun getOrderedProviders(): List<SolanaRpcProvider> {
        val last = lastSuccessfulProvider
        return if (last != null && providers.contains(last)) {
            listOf(last) + providers.filter { it !== last }
        } else {
            providers
        }
    }

    /**
     * Find provider by name (for consistency in validation).
     */
    fun findProviderByName(name: String): SolanaRpcProvider? {
        return providers.find { it.name == name }
    }

    /**
     * Reset last successful provider (for testing).
     */
    fun resetLastSuccessful() {
        lastSuccessfulProvider = null
    }

    /**
     * Get current cluster name.
     */
    fun getCluster(): String = cluster

    /**
     * Get provider names for logging.
     */
    fun getProviderNames(): List<String> = providers.map { it.name }

    /**
     * DEBUG-ONLY: Check if primary failure simulation is active.
     * Returns false in release builds (compiler will optimize this away).
     */
    private fun shouldSimulatePrimaryFailure(providerName: String): Boolean {
        return BuildConfig.DEBUG && SIMULATE_PRIMARY_FAIL && providerName == "Primary"
    }
}
