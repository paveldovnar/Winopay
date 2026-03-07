package com.winopay.tron.rpc

import android.util.Log
import com.winopay.tron.TronConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Multi-RPC failover manager for TRON payment detection.
 *
 * PRODUCTION-GRADE RELIABILITY:
 * 1. TronGrid as primary provider
 * 2. Public TRON nodes as fallback
 * 3. 3-second timeout per provider (fast failover)
 * 4. Remembers last successful provider for session
 *
 * PROVIDER LISTS:
 * Mainnet:
 *   1. TronGrid (https://api.trongrid.io)
 *   2. TRON public node (https://api.tronstack.io)
 *
 * Nile Testnet:
 *   1. TronGrid Nile (https://nile.trongrid.io)
 *
 * Shasta Testnet:
 *   1. TronGrid Shasta (https://api.shasta.trongrid.io)
 */
object TronProviderManager {

    private const val TAG = "TronProviderManager"
    private const val TIMEOUT_MS = 3000

    /** Max pages to fetch when paginating (safety cap) */
    const val MAX_PAGES = 3

    /** Max transactions per page */
    const val PAGE_SIZE = 50

    /** Backoff delays for 429 retry (ms) */
    private val BACKOFF_DELAYS_MS = listOf(500L, 1000L, 2000L)

    // Provider URLs by network
    private val MAINNET_PROVIDERS = listOf(
        ProviderConfig("TronGrid", "https://api.trongrid.io"),
        ProviderConfig("TronStack", "https://api.tronstack.io")
    )

    private val NILE_PROVIDERS = listOf(
        ProviderConfig("TronGrid-Nile", "https://nile.trongrid.io")
    )

    private val SHASTA_PROVIDERS = listOf(
        ProviderConfig("TronGrid-Shasta", "https://api.shasta.trongrid.io")
    )

    /** Provider configuration */
    private data class ProviderConfig(val name: String, val url: String)

    /** Built providers by network */
    private val providersByNetwork = mutableMapOf<String, List<TronRpcProvider>>()

    /** Last successful provider per network */
    @Volatile
    private var lastSuccessfulProvider = mutableMapOf<String, TronRpcProvider>()

    /** API key for TronGrid (optional, for higher rate limits) */
    private var tronGridApiKey: String? = null

    /**
     * Set TronGrid API key for higher rate limits.
     * Call this from Application.onCreate() if you have a key.
     */
    fun setApiKey(apiKey: String?) {
        tronGridApiKey = apiKey
        // Clear cached providers to rebuild with new key
        providersByNetwork.clear()
    }

    /**
     * Get providers for a network.
     */
    fun getProviders(networkId: String): List<TronRpcProvider> {
        return providersByNetwork.getOrPut(networkId) {
            buildProviderList(networkId)
        }
    }

    /**
     * Build provider list for a network.
     */
    private fun buildProviderList(networkId: String): List<TronRpcProvider> {
        val configs = when (networkId) {
            TronConstants.NETWORK_MAINNET -> MAINNET_PROVIDERS
            TronConstants.NETWORK_NILE -> NILE_PROVIDERS
            TronConstants.NETWORK_SHASTA -> SHASTA_PROVIDERS
            else -> {
                Log.w(TAG, "Unknown TRON network: $networkId, using mainnet providers")
                MAINNET_PROVIDERS
            }
        }

        val providers = configs.map { config ->
            StandardTronRpcProvider(
                name = config.name,
                baseUrl = config.url,
                apiKey = if (config.name.startsWith("TronGrid")) tronGridApiKey else null,
                timeoutMs = TIMEOUT_MS
            )
        }

        Log.i(TAG, "TRON|PROVIDERS|network=$networkId|chain=${providers.joinToString(" → ") { it.name }}")
        return providers
    }

    /**
     * Get TRC20 transactions with multi-provider failover and 429 retry.
     *
     * @param address Wallet address
     * @param contractAddress Token contract to filter (null for all)
     * @param networkId Network ID (tron-mainnet, tron-nile, etc.)
     * @param limit Max transactions per page
     * @param minTimestamp Only return transactions after this timestamp (ms)
     * @param fingerprint Pagination cursor (null for first page)
     * @return Paginated list of transactions
     */
    suspend fun getTrc20Transactions(
        address: String,
        contractAddress: String?,
        networkId: String,
        limit: Int = 20,
        minTimestamp: Long? = null,
        fingerprint: String? = null
    ): TronRpcResult<Trc20TransactionsPage> = withContext(Dispatchers.IO) {
        Log.d(TAG, "TRON|TRC20_TX|address=${address.take(12)}...|contract=${contractAddress?.take(12) ?: "all"}|network=$networkId|page=${fingerprint?.take(8) ?: "first"}")

        val orderedProviders = getOrderedProviders(networkId)
        val failedProviders = mutableListOf<String>()

        for ((index, provider) in orderedProviders.withIndex()) {
            val providerNum = index + 1
            Log.d(TAG, "  [$providerNum/${orderedProviders.size}] ${provider.name} → trying...")

            // Try with 429 backoff retry
            val result = fetchWithBackoff(provider, address, contractAddress, limit, minTimestamp, fingerprint)

            when (result) {
                is TronRpcResult.Success -> {
                    Log.d(TAG, "  [$providerNum/${orderedProviders.size}] ${provider.name} → OK (${result.data.transactions.size} txs, nextPage=${result.data.fingerprint != null})")
                    lastSuccessfulProvider[networkId] = provider
                    return@withContext result
                }
                is TronRpcResult.Failure -> {
                    val reason = "${result.errorType}: ${result.message}"
                    Log.w(TAG, "  [$providerNum/${orderedProviders.size}] ${provider.name} → FAIL ($reason)")
                    failedProviders.add("${provider.name}: $reason")
                }
            }
        }

        Log.e(TAG, "TRON|TRC20_TX|ALL_FAILED|providers=${orderedProviders.size}|${failedProviders.joinToString("|")}")

        TronRpcResult.Failure(
            errorType = TronRpcErrorType.NETWORK_ERROR,
            message = "All ${orderedProviders.size} providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Fetch with exponential backoff on 429.
     */
    private suspend fun fetchWithBackoff(
        provider: TronRpcProvider,
        address: String,
        contractAddress: String?,
        limit: Int,
        minTimestamp: Long?,
        fingerprint: String?
    ): TronRpcResult<Trc20TransactionsPage> {
        var lastResult: TronRpcResult<Trc20TransactionsPage>? = null

        for ((attempt, backoffMs) in BACKOFF_DELAYS_MS.withIndex()) {
            val result = provider.getTrc20Transactions(
                address = address,
                contractAddress = contractAddress,
                limit = limit,
                minTimestamp = minTimestamp,
                fingerprint = fingerprint
            )

            if (result is TronRpcResult.Failure && result.errorType == TronRpcErrorType.RATE_LIMITED) {
                Log.w(TAG, "    429 rate limited, backoff ${backoffMs}ms (attempt ${attempt + 1}/${BACKOFF_DELAYS_MS.size})")
                delay(backoffMs)
                lastResult = result
                continue
            }

            return result
        }

        // All retries exhausted
        return lastResult ?: TronRpcResult.Failure(
            errorType = TronRpcErrorType.RATE_LIMITED,
            message = "Rate limited after ${BACKOFF_DELAYS_MS.size} retries",
            providerName = provider.name
        )
    }

    /**
     * Paginated fetch of TRC20 transactions with stop conditions.
     *
     * Fetches pages until ONE of these conditions is met:
     * 1. Found a candidate transaction >= createdAt (returns all fetched txs)
     * 2. Reached transactions older than createdAt (stop - payment can't be earlier)
     * 3. Hit safety cap (MAX_PAGES pages fetched)
     * 4. No more pages (fingerprint is null)
     *
     * @param address Wallet address
     * @param contractAddress Token contract to filter
     * @param networkId Network ID
     * @param createdAt Invoice creation timestamp (ms) - stop scanning when reaching older txs
     * @param candidateMatcher Function to check if a transaction is a valid candidate
     * @return All transactions fetched across pages
     */
    suspend fun getTrc20TransactionsPaginated(
        address: String,
        contractAddress: String?,
        networkId: String,
        createdAt: Long,
        candidateMatcher: ((Trc20Transaction) -> Boolean)? = null
    ): TronRpcResult<List<Trc20Transaction>> = withContext(Dispatchers.IO) {
        Log.i(TAG, "TRON|PAGINATE|START|address=${address.take(12)}...|createdAt=$createdAt|network=$networkId")

        val allTransactions = mutableListOf<Trc20Transaction>()
        var fingerprint: String? = null
        var pageNum = 0
        var foundCandidate = false
        var reachedOldTxs = false

        while (pageNum < MAX_PAGES) {
            pageNum++

            val result = getTrc20Transactions(
                address = address,
                contractAddress = contractAddress,
                networkId = networkId,
                limit = PAGE_SIZE,
                minTimestamp = null, // Don't filter at API level - we need to detect "reached older" condition
                fingerprint = fingerprint
            )

            when (result) {
                is TronRpcResult.Failure -> {
                    Log.e(TAG, "TRON|PAGINATE|FAIL|page=$pageNum|${result.errorType}|${result.message}")
                    // Return what we have so far, or the error if nothing
                    return@withContext if (allTransactions.isEmpty()) {
                        result
                    } else {
                        Log.w(TAG, "TRON|PAGINATE|PARTIAL|pages=$pageNum|txs=${allTransactions.size}")
                        TronRpcResult.Success(allTransactions, result.providerName)
                    }
                }
                is TronRpcResult.Success -> {
                    val page = result.data
                    val txs = page.transactions

                    if (txs.isEmpty()) {
                        Log.d(TAG, "TRON|PAGINATE|EMPTY|page=$pageNum")
                        break
                    }

                    // Check transactions in this page
                    for (tx in txs) {
                        // Stop condition 2: transaction older than invoice creation
                        if (tx.blockTimestamp < createdAt) {
                            Log.d(TAG, "TRON|PAGINATE|STOP|OLDER|page=$pageNum|txTime=${tx.blockTimestamp}|createdAt=$createdAt")
                            reachedOldTxs = true
                            break
                        }

                        allTransactions.add(tx)

                        // Stop condition 1: found matching candidate
                        if (candidateMatcher != null && candidateMatcher(tx)) {
                            Log.i(TAG, "TRON|PAGINATE|STOP|MATCH|page=$pageNum|txId=${tx.txId.take(12)}...")
                            foundCandidate = true
                            break
                        }
                    }

                    Log.d(TAG, "TRON|PAGINATE|PAGE|page=$pageNum|fetched=${txs.size}|total=${allTransactions.size}|hasNext=${page.fingerprint != null}")

                    // Stop if we found what we need or reached older txs
                    if (foundCandidate || reachedOldTxs) {
                        break
                    }

                    // Stop condition 4: no more pages
                    fingerprint = page.fingerprint
                    if (fingerprint == null) {
                        Log.d(TAG, "TRON|PAGINATE|STOP|END|page=$pageNum|total=${allTransactions.size}")
                        break
                    }
                }
            }
        }

        // Stop condition 3: safety cap
        if (pageNum >= MAX_PAGES && !foundCandidate && !reachedOldTxs) {
            Log.w(TAG, "TRON|PAGINATE|STOP|CAP|pages=$MAX_PAGES|total=${allTransactions.size}")
        }

        Log.i(TAG, "TRON|PAGINATE|DONE|pages=$pageNum|txs=${allTransactions.size}|found=$foundCandidate|reachedOld=$reachedOldTxs")
        TronRpcResult.Success(allTransactions, "paginated")
    }

    /**
     * Get transaction by ID with failover.
     */
    suspend fun getTransactionById(
        txId: String,
        networkId: String,
        preferredProvider: TronRpcProvider? = null
    ): TronRpcResult<TronTransaction?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "TRON|GET_TX|txId=${txId.take(12)}...|network=$networkId")

        val orderedProviders = if (preferredProvider != null) {
            listOf(preferredProvider) + getProviders(networkId).filter { it !== preferredProvider }
        } else {
            getOrderedProviders(networkId)
        }

        for ((index, provider) in orderedProviders.withIndex()) {
            val result = provider.getTransactionById(txId)

            when (result) {
                is TronRpcResult.Success -> {
                    Log.d(TAG, "  Provider ${index + 1}: ${provider.name} → SUCCESS")
                    lastSuccessfulProvider[networkId] = provider
                    return@withContext result
                }
                is TronRpcResult.Failure -> {
                    Log.w(TAG, "  Provider ${index + 1}: ${provider.name} → FAIL (${result.message})")
                }
            }
        }

        TronRpcResult.Failure(
            errorType = TronRpcErrorType.NETWORK_ERROR,
            message = "All providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Get transaction info (confirmation) with failover.
     */
    suspend fun getTransactionInfo(
        txId: String,
        networkId: String,
        preferredProvider: TronRpcProvider? = null
    ): TronRpcResult<TronTransactionInfo?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "TRON|TX_INFO|txId=${txId.take(12)}...|network=$networkId")

        val orderedProviders = if (preferredProvider != null) {
            listOf(preferredProvider) + getProviders(networkId).filter { it !== preferredProvider }
        } else {
            getOrderedProviders(networkId)
        }

        for ((index, provider) in orderedProviders.withIndex()) {
            val result = provider.getTransactionInfo(txId)

            when (result) {
                is TronRpcResult.Success -> {
                    Log.d(TAG, "  Provider ${index + 1}: ${provider.name} → SUCCESS")
                    lastSuccessfulProvider[networkId] = provider
                    return@withContext result
                }
                is TronRpcResult.Failure -> {
                    Log.w(TAG, "  Provider ${index + 1}: ${provider.name} → FAIL (${result.message})")
                }
            }
        }

        TronRpcResult.Failure(
            errorType = TronRpcErrorType.NETWORK_ERROR,
            message = "All providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Get providers ordered with last successful first.
     */
    private fun getOrderedProviders(networkId: String): List<TronRpcProvider> {
        val providers = getProviders(networkId)
        val last = lastSuccessfulProvider[networkId]
        return if (last != null && providers.contains(last)) {
            listOf(last) + providers.filter { it !== last }
        } else {
            providers
        }
    }

    /**
     * Find provider by name.
     */
    fun findProviderByName(name: String, networkId: String): TronRpcProvider? {
        return getProviders(networkId).find { it.name == name }
    }

    /**
     * Create unsigned TRC20 transfer transaction with failover.
     */
    suspend fun createTrc20Transaction(
        ownerAddressHex: String,
        contractAddressHex: String,
        functionSelector: String,
        parameter: String,
        feeLimit: Long,
        networkId: String
    ): TronRpcResult<UnsignedTronTransaction> = withContext(Dispatchers.IO) {
        Log.d(TAG, "TRON|CREATE_TX|owner=${ownerAddressHex.take(12)}...|network=$networkId")

        val orderedProviders = getOrderedProviders(networkId)

        for ((index, provider) in orderedProviders.withIndex()) {
            val result = provider.createTrc20Transaction(
                ownerAddressHex = ownerAddressHex,
                contractAddressHex = contractAddressHex,
                functionSelector = functionSelector,
                parameter = parameter,
                feeLimit = feeLimit
            )

            when (result) {
                is TronRpcResult.Success -> {
                    Log.d(TAG, "  Provider ${index + 1}: ${provider.name} → SUCCESS")
                    lastSuccessfulProvider[networkId] = provider
                    return@withContext result
                }
                is TronRpcResult.Failure -> {
                    Log.w(TAG, "  Provider ${index + 1}: ${provider.name} → FAIL (${result.message})")
                }
            }
        }

        TronRpcResult.Failure(
            errorType = TronRpcErrorType.NETWORK_ERROR,
            message = "All providers failed",
            providerName = "ALL"
        )
    }

    /**
     * Broadcast signed transaction with failover.
     */
    suspend fun broadcastTransaction(
        signedTxHex: String,
        networkId: String
    ): TronRpcResult<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "TRON|BROADCAST|network=$networkId")

        val orderedProviders = getOrderedProviders(networkId)
        val failedProviders = mutableListOf<String>()

        for ((index, provider) in orderedProviders.withIndex()) {
            Log.d(TAG, "  Provider ${index + 1}: ${provider.name}")

            val result = provider.broadcastTransaction(signedTxHex)

            when (result) {
                is TronRpcResult.Success -> {
                    Log.i(TAG, "  Provider ${index + 1}: ${provider.name} → SUCCESS|txId=${result.data.take(16)}...")
                    lastSuccessfulProvider[networkId] = provider
                    return@withContext result
                }
                is TronRpcResult.Failure -> {
                    val reason = "${result.errorType}: ${result.message}"
                    Log.w(TAG, "  Provider ${index + 1}: ${provider.name} → FAIL ($reason)")
                    failedProviders.add("${provider.name}: $reason")
                }
            }
        }

        Log.e(TAG, "TRON|BROADCAST|ALL_FAILED|${failedProviders.joinToString("|")}")

        TronRpcResult.Failure(
            errorType = TronRpcErrorType.NETWORK_ERROR,
            message = "All ${orderedProviders.size} providers failed to broadcast",
            providerName = "ALL"
        )
    }

    /**
     * Reset last successful provider (for testing).
     */
    fun resetLastSuccessful() {
        lastSuccessfulProvider.clear()
    }

    /**
     * Get provider names for a network.
     */
    fun getProviderNames(networkId: String): List<String> {
        return getProviders(networkId).map { it.name }
    }
}
