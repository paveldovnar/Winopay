package com.winopay.data.model

/**
 * Snapshot of exchange rates from Frankfurter API.
 *
 * RATE DIRECTION: All rates are "units of currency per 1 USD"
 * Example: RUB rate = 90.5 means 1 USD = 90.5 RUB
 *
 * To convert RUB to USD: usdAmount = rubAmount / rubPerUsd
 *
 * @param base Base currency code (always "USD")
 * @param date Date string from API (e.g., "2024-01-15")
 * @param rates Map of currency code to exchange rate (units per 1 USD)
 * @param lastUpdated Timestamp when this snapshot was fetched/cached (millis)
 * @param provider Name of the rate provider (e.g., "Frankfurter")
 */
data class RatesSnapshot(
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
    val lastUpdated: Long,
    val provider: String = "Frankfurter"
) {
    companion object {
        // Cache validity: 1 hour (stricter for financial accuracy)
        const val CACHE_VALIDITY_MS = 1 * 60 * 60 * 1000L

        // Provider name
        const val PROVIDER_FRANKFURTER = "Frankfurter"

        // NO FALLBACK RATES - rates must come from real provider
        // If no rates available, invoice creation must be blocked
    }

    /**
     * Check if rates are fresh (within cache validity).
     */
    fun isFresh(): Boolean {
        val age = System.currentTimeMillis() - lastUpdated
        return age <= CACHE_VALIDITY_MS
    }

    /**
     * Check if rates are valid for use (not too old).
     * More lenient than isFresh() - allows up to 24 hours for degraded operation.
     */
    fun isValid(): Boolean {
        val age = System.currentTimeMillis() - lastUpdated
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours absolute max
        return age <= maxAge && rates.isNotEmpty()
    }

    /**
     * Get age of rates in human-readable format.
     */
    fun getAgeString(): String {
        val ageMs = System.currentTimeMillis() - lastUpdated
        val minutes = ageMs / (60 * 1000)
        val hours = minutes / 60
        return when {
            hours >= 1 -> "${hours}h ${minutes % 60}m ago"
            minutes >= 1 -> "${minutes}m ago"
            else -> "just now"
        }
    }
}
