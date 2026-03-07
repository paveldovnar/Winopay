package com.winopay.data.pos

/**
 * Central source of truth for invoice timeout configuration.
 *
 * PRODUCT RULES:
 * - Default timeout: 5 minutes (300 seconds)
 * - After timeout: soft-expire (merchant chooses extend/revoke)
 * - Extension period: 5 more minutes
 * - Payment detection continues during soft-expire
 */
object InvoiceTimeouts {

    /**
     * Default timeout for invoice payment detection (5 minutes).
     * After this time, invoice enters soft-expire state (awaiting merchant decision).
     */
    const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes

    /**
     * Extension period when merchant chooses "Wait 5 more minutes" (5 minutes).
     */
    const val EXTENSION_TIMEOUT_MS = 5 * 60 * 1000L  // 5 minutes

    /**
     * Polling interval for payment detection (2 seconds).
     */
    const val POLL_INTERVAL_MS = 2_000L

    /**
     * Grace period for last-chance sweep before showing soft-expire (3 seconds).
     * Allows catching transactions that arrived at the last moment.
     */
    const val LAST_CHANCE_SWEEP_MS = 3_000L
}
