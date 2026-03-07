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
 * Backup FX provider using Open Exchange Rates API (open.er-api.com).
 *
 * Endpoint: https://open.er-api.com/v6/latest/USD
 * Source: Open Exchange Rates
 * Reliability: Medium
 * Rate limit: 1500 requests/month (free tier)
 *
 * This is a last-resort fallback provider.
 */
class OpenErApiProvider : FxProvider {

    companion object {
        private const val TAG = "OpenErApiProvider"
        private const val TIMEOUT_MS = 3000L
        private const val BASE_URL = "https://open.er-api.com/v6/latest"
    }

    override val name: String = "OpenErApi"
    override val endpoint: String = BASE_URL

    override suspend fun fetchRates(base: String): FxResponse? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$base"
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

            // Check result field
            val result = obj.optString("result", "")
            if (result == "error") {
                val errorType = obj.optString("error-type", "unknown")
                Log.e(TAG, "API error: $errorType")
                return null
            }

            val base = obj.optString("base_code", requestedBase)
            // This API returns time_last_update_utc instead of date
            val timeUpdated = obj.optString("time_last_update_utc", "")
            // Extract date from "Mon, 15 Jan 2024 00:00:01 +0000" format
            val date = extractDate(timeUpdated)

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

    /**
     * Extract date from RFC 2822 format.
     * Input: "Mon, 15 Jan 2024 00:00:01 +0000"
     * Output: "2024-01-15"
     */
    private fun extractDate(timeString: String): String {
        if (timeString.isEmpty()) return ""
        return try {
            // Simple extraction: find day, month, year
            val parts = timeString.split(" ")
            if (parts.size >= 4) {
                val day = parts[1].padStart(2, '0')
                val monthStr = parts[2]
                val year = parts[3]
                val month = monthToNumber(monthStr)
                "$year-$month-$day"
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun monthToNumber(month: String): String {
        return when (month.lowercase()) {
            "jan" -> "01"
            "feb" -> "02"
            "mar" -> "03"
            "apr" -> "04"
            "may" -> "05"
            "jun" -> "06"
            "jul" -> "07"
            "aug" -> "08"
            "sep" -> "09"
            "oct" -> "10"
            "nov" -> "11"
            "dec" -> "12"
            else -> "01"
        }
    }
}
