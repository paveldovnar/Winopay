package com.winopay.data

import android.util.Log
import com.winopay.data.fx.ExchangeRateHostProvider
import com.winopay.data.fx.FrankfurterProvider
import com.winopay.data.fx.FxProvider
import com.winopay.data.fx.FxResponse
import com.winopay.data.fx.OpenErApiProvider
import com.winopay.data.local.DataStoreManager
import com.winopay.data.model.RatesSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for exchange rates with MULTI-PROVIDER FAILOVER.
 *
 * PROVIDER ORDER:
 * 1. Frankfurter (Primary - ECB based, most reliable)
 * 2. ExchangeRateHost (Fallback)
 * 3. OpenErApi (Backup)
 *
 * CRITICAL RULES:
 * 1. USD merchants NEVER need rates (1 USD = 1 USDC exactly)
 * 2. Non-USD merchants require valid rates or invoice creation is blocked
 * 3. Cache valid for 24 hours for degraded operation
 * 4. If all providers fail, use cache if valid
 */
class RatesRepository(private val dataStoreManager: DataStoreManager) {

    companion object {
        private const val TAG = "RatesRepository"
    }

    /**
     * Ordered list of FX providers for failover.
     * First provider is primary, subsequent are fallbacks.
     */
    private val providers: List<FxProvider> = listOf(
        FrankfurterProvider(),      // Primary (ECB)
        ExchangeRateHostProvider(), // Fallback
        OpenErApiProvider()         // Backup
    )

    /**
     * Get exchange rates with USD as base.
     *
     * RATE DIRECTION: Returns rates where value = units of currency per 1 USD
     * Example: RUB = 90.5 means 1 USD = 90.5 RUB
     *
     * Behavior:
     * 1. If cached rates are fresh (<= 1 hour) -> return cache
     * 2. Else try each provider in order until one succeeds
     * 3. If all providers fail AND cache is still valid (<= 24 hours) -> return stale cache
     * 4. If no valid rates available -> return null (BLOCKS non-USD invoice creation)
     *
     * @return RatesSnapshot or null if no valid rates available
     */
    suspend fun getRates(): RatesSnapshot? {
        Log.d(TAG, "━━━━━━━━ FX DEBUG: getRates() ━━━━━━━━")

        // Try to get cached rates
        val cached = dataStoreManager.getRatesSnapshot()

        Log.d(TAG, "  cached snapshot: ${if (cached != null) "EXISTS" else "NULL"}")
        if (cached != null) {
            Log.d(TAG, "  cached provider: ${cached.provider}")
            Log.d(TAG, "  cached date: ${cached.date}")
            Log.d(TAG, "  cached age: ${cached.getAgeString()}")
            Log.d(TAG, "  cached isFresh: ${cached.isFresh()}")
            Log.d(TAG, "  cached isValid: ${cached.isValid()}")
            Log.d(TAG, "  cached currencies: ${cached.rates.size}")
        }

        // If cache is fresh, return it
        if (cached != null && cached.isFresh()) {
            Log.d(TAG, "  RESULT: Using fresh cache")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return cached
        }

        // Try to fetch from network with failover
        Log.d(TAG, "  Fetching from network (failover enabled)...")
        val networkRates = fetchWithFailover()

        return if (networkRates != null) {
            dataStoreManager.saveRatesSnapshot(networkRates)
            Log.d(TAG, "  RESULT: Fresh rates fetched")
            Log.d(TAG, "  network provider: ${networkRates.provider}")
            Log.d(TAG, "  network date: ${networkRates.date}")
            Log.d(TAG, "  network currencies: ${networkRates.rates.size}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            networkRates
        } else {
            // All providers failed - try stale cache
            if (cached != null && cached.isValid()) {
                Log.w(TAG, "  RESULT: Using stale cache (${cached.getAgeString()})")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                cached
            } else {
                Log.e(TAG, "  RESULT: NO VALID RATES")
                Log.e(TAG, "  cached: ${if (cached != null) "exists but expired" else "NULL"}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                null
            }
        }
    }

    /**
     * Force refresh rates from network (ignores cache).
     *
     * @return Fresh rates or null if all providers fail
     */
    suspend fun forceRefresh(): RatesSnapshot? {
        Log.d(TAG, "━━━━━━━━ FX DEBUG: forceRefresh() ━━━━━━━━")

        val networkRates = fetchWithFailover()

        return if (networkRates != null) {
            dataStoreManager.saveRatesSnapshot(networkRates)
            Log.d(TAG, "  RESULT: Force refresh succeeded")
            Log.d(TAG, "  provider: ${networkRates.provider}")
            Log.d(TAG, "  date: ${networkRates.date}")
            Log.d(TAG, "  currencies: ${networkRates.rates.size}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            networkRates
        } else {
            Log.e(TAG, "  RESULT: Force refresh FAILED (all providers)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            null
        }
    }

    /**
     * Check if valid rates are available (without fetching).
     */
    suspend fun hasValidRates(): Boolean {
        val cached = dataStoreManager.getRatesSnapshot()
        return cached != null && cached.isValid()
    }

    /**
     * Fetch rates with multi-provider failover.
     *
     * Tries each provider in order until one succeeds.
     * Each provider has a 3-second timeout.
     *
     * @return RatesSnapshot or null if all providers fail
     */
    private suspend fun fetchWithFailover(): RatesSnapshot? = withContext(Dispatchers.IO) {
        Log.d(TAG, "━━━━━━━━ FX FAILOVER ━━━━━━━━")
        Log.d(TAG, "  Providers: ${providers.map { it.name }.joinToString(" → ")}")

        val failedProviders = mutableListOf<String>()

        for ((index, provider) in providers.withIndex()) {
            val providerNum = index + 1
            Log.d(TAG, "  Provider $providerNum: ${provider.name}")
            Log.d(TAG, "    endpoint: ${provider.endpoint}")

            try {
                val response = provider.fetchRates("USD")

                if (response != null && response.rates.isNotEmpty()) {
                    Log.d(TAG, "  Provider $providerNum: ${provider.name} → SUCCESS")
                    Log.d(TAG, "    currencies: ${response.rates.size}")
                    Log.d(TAG, "    date: ${response.date}")
                    Log.d(TAG, "  Snapshot cached")
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    return@withContext convertToSnapshot(response)
                } else {
                    Log.w(TAG, "  Provider $providerNum: ${provider.name} → FAIL (empty response)")
                    failedProviders.add("${provider.name}: empty response")
                }
            } catch (e: Exception) {
                val reason = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "  Provider $providerNum: ${provider.name} → FAIL ($reason)")
                failedProviders.add("${provider.name}: $reason")
            }
        }

        Log.e(TAG, "  ALL PROVIDERS FAILED")
        failedProviders.forEachIndexed { i, reason ->
            Log.e(TAG, "    ${i + 1}. $reason")
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        null
    }

    /**
     * Convert FxResponse to RatesSnapshot.
     */
    private fun convertToSnapshot(response: FxResponse): RatesSnapshot {
        // Convert BigDecimal rates to Double for storage
        val doubleRates = response.rates.mapValues { it.value.toDouble() }

        return RatesSnapshot(
            base = response.base,
            date = response.date,
            rates = doubleRates,
            lastUpdated = System.currentTimeMillis(),
            provider = response.provider
        )
    }
}
