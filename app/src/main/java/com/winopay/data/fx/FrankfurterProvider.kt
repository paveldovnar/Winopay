package com.winopay.data.fx

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL

/**
 * Primary FX provider using Frankfurter API (ECB-based).
 *
 * Endpoint: https://api.frankfurter.dev/latest?from=USD
 * Source: European Central Bank
 * Reliability: High (ECB data)
 * Rate limit: None
 */
class FrankfurterProvider : FxProvider {

    companion object {
        private const val TAG = "FrankfurterProvider"
        private const val TIMEOUT_MS = 3000L
        private const val BASE_URL = "https://api.frankfurter.dev/latest"
    }

    override val name: String = "Frankfurter"
    override val endpoint: String = BASE_URL

    override suspend fun fetchRates(base: String): FxResponse? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?from=$base"
        Log.d(TAG, "Fetching rates from: $url")

        try {
            withTimeout(TIMEOUT_MS) {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TIMEOUT_MS.toInt()
                    connection.readTimeout = TIMEOUT_MS.toInt()
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Accept", "application/json")

                    val responseCode = connection.responseCode
                    Log.d(TAG, "HTTP $responseCode")

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "HTTP error: $responseCode")
                        return@withTimeout null
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseResponse(response, base)
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FAIL: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
    }

    private fun parseResponse(json: String, requestedBase: String): FxResponse? {
        return try {
            val obj = JSONObject(json)
            val base = obj.getString("base")
            val date = obj.getString("date")
            val ratesObj = obj.getJSONObject("rates")

            val rates = mutableMapOf<String, BigDecimal>()
            // Add base currency as 1.0
            rates[base] = BigDecimal.ONE

            val keys = ratesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rate = ratesObj.getDouble(key)
                // Sanity check: rate must be positive and reasonable
                if (rate > 0 && rate < 1_000_000) {
                    rates[key] = BigDecimal.valueOf(rate)
                }
            }

            Log.d(TAG, "Parsed ${rates.size} currencies, date: $date")
            FxResponse(
                base = base,
                date = date,
                rates = rates,
                provider = name
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            null
        }
    }
}
