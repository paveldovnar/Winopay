package com.winopay.data.fx

import java.math.BigDecimal

/**
 * Response from an FX provider.
 *
 * @param base Base currency (always "USD")
 * @param date Date string from provider (e.g., "2024-01-15")
 * @param rates Map of currency code to rate (units per 1 USD)
 * @param provider Provider name for logging/display
 */
data class FxResponse(
    val base: String,
    val date: String,
    val rates: Map<String, BigDecimal>,
    val provider: String
)

/**
 * Interface for FX rate providers.
 *
 * Each provider must:
 * - Return rates as BigDecimal for financial precision
 * - Return null on failure (NO exception propagation)
 * - Respect timeout limits
 * - Log HTTP status and errors
 */
interface FxProvider {
    /**
     * Provider name for logging.
     */
    val name: String

    /**
     * HTTP endpoint URL for logging.
     */
    val endpoint: String

    /**
     * Fetch exchange rates from this provider.
     *
     * @param base Base currency (default "USD")
     * @return FxResponse or null if fetch failed
     */
    suspend fun fetchRates(base: String = "USD"): FxResponse?
}
