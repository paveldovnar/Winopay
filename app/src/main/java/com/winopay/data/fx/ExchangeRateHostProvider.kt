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
 * Fallback FX provider using exchangerate.host API.
 *
 * Endpoint: https://api.exchangerate.host/latest?base=USD
 * Source: Multiple aggregated sources
 * Reliability: Medium-High
 * Rate limit: 100 requests/month (free tier)
 *
 * Note: Free tier may have limitations. This is a fallback provider.
 */
class ExchangeRateHostProvider : FxProvider {

    companion object {
        private const val TAG = "ExchangeRateHost"
        private const val TIMEOUT_MS = 3000L
        private const val BASE_URL = "https://api.exchangerate.host/latest"
    }

    override val name: String = "ExchangeRateHost"
    override val endpoint: String = BASE_URL

    override suspend fun fetchRates(base: String): FxResponse? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL?base=$base"
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

            // Check success field if present
            if (obj.has("success") && !obj.getBoolean("success")) {
                Log.e(TAG, "API returned success=false")
                return null
            }

            val base = obj.optString("base", requestedBase)
            val date = obj.optString("date", "")
            val ratesObj = obj.optJSONObject("rates")

            if (ratesObj == null) {
                Log.e(TAG, "No rates object in response")
                return null
            }

            val rates = mutableMapOf<String, BigDecimal>()
            // Add base currency as 1.0
            rates[base] = BigDecimal.ONE

            val keys = ratesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rate = ratesObj.getDouble(key)
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
