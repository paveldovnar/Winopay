package com.winopay.payments

import android.util.Log
import com.winopay.BuildConfig
import com.winopay.data.local.InvoiceEntity

/**
 * Shared logging helper for payment detection.
 *
 * Provides consistent logging format between:
 * - SolanaPayPaymentDetector (foreground)
 * - InvoiceDetectionWorker (background)
 *
 * KEY FORMAT: Single-line summary for easy filtering:
 * DETECT|invoiceId|railId|networkId|currency|targets|strategy|result
 */
object DetectionLogger {

    private const val TAG = "DetectionLog"

    /**
     * Log detection start with all key info in ONE LINE for filtering.
     */
    fun logDetectionStart(
        source: String, // "FOREGROUND" or "WORKER"
        invoice: InvoiceEntity,
        pollingTargets: List<PollingTarget>
    ) {
        val targetsSummary = pollingTargets.joinToString(",") {
            "${it.strategy.name.take(3)}:${it.label}"
        }

        // ONE-LINE SUMMARY for grep/filter
        Log.i(TAG, "START|$source|${invoice.id.take(8)}|${invoice.railId}|${invoice.networkId}|${invoice.currency}|targets=[$targetsSummary]")

        // Detailed log
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "DETECTION START [$source]")
        Log.d(TAG, "  invoiceId:  ${invoice.id}")
        Log.d(TAG, "  railId:     ${invoice.railId}")
        Log.d(TAG, "  networkId:  ${invoice.networkId}")
        Log.d(TAG, "  currency:   ${invoice.currency}")
        Log.d(TAG, "  amount:     ${invoice.amount} (minor units)")
        Log.d(TAG, "  recipient:  ${invoice.recipientAddress.take(20)}...")
        Log.d(TAG, "  mint:       ${invoice.splTokenMint?.take(20) ?: "SOL"}...")
        Log.d(TAG, "  createdAt:  ${invoice.createdAt}")
        Log.d(TAG, "  USDC_MINT:  ${BuildConfig.USDC_MINT.take(16)}...")
        Log.d(TAG, "  USDT_MINT:  ${BuildConfig.USDT_MINT.ifBlank { "(none)" }.take(16)}...")
        Log.d(TAG, "  ────────────────────────────────────────────")
        Log.d(TAG, "  POLLING TARGETS (${pollingTargets.size}):")
        pollingTargets.forEachIndexed { idx, target ->
            Log.d(TAG, "    [$idx] ${target.strategy} | ${target.label} | ${target.address.take(20)}...")
            if (target.tokenMint != null) {
                Log.d(TAG, "        mint: ${target.tokenMint.take(20)}...")
            }
        }
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Log fallback trigger with ONE-LINE summary.
     */
    fun logFallbackTriggered(
        source: String,
        invoiceId: String,
        pollCount: Int,
        reason: String,
        discoveredTargets: List<PollingTarget>
    ) {
        val targetsSummary = discoveredTargets.joinToString(",") {
            "${it.strategy.name.take(3)}:${it.label}"
        }

        // ONE-LINE SUMMARY
        Log.w(TAG, "FALLBACK|$source|${invoiceId.take(8)}|poll=$pollCount|discovered=${discoveredTargets.size}|targets=[$targetsSummary]")

        // Detailed log
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.w(TAG, "⚡ FALLBACK TRIGGERED [$source]")
        Log.w(TAG, "  invoiceId: ${invoiceId.take(8)}...")
        Log.w(TAG, "  pollCount: $pollCount")
        Log.w(TAG, "  reason:    $reason")
        Log.w(TAG, "  DISCOVERED TARGETS (${discoveredTargets.size}):")
        discoveredTargets.forEachIndexed { idx, target ->
            Log.w(TAG, "    [$idx] ${target.strategy} | ${target.label} | ${target.address.take(20)}...")
        }
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Log signature found on a target with ONE-LINE summary.
     */
    fun logSignatureFound(
        source: String,
        invoiceId: String,
        pollCount: Int,
        target: PollingTarget,
        signatureCount: Int,
        providerName: String
    ) {
        // ONE-LINE SUMMARY
        Log.i(TAG, "SIGS|$source|${invoiceId.take(8)}|poll=$pollCount|${target.strategy}:${target.label}|count=$signatureCount|via=$providerName")
    }

    /**
     * Log signature validation attempt with ONE-LINE summary.
     */
    fun logSignatureCheck(
        source: String,
        invoiceId: String,
        signature: String,
        blockTime: Long,
        invoiceCreatedAt: Long,
        target: PollingTarget
    ) {
        val timeDelta = blockTime - invoiceCreatedAt
        val gate = if (timeDelta >= 0) "PASS" else "FAIL"

        // ONE-LINE SUMMARY
        Log.d(TAG, "CHECK|$source|${invoiceId.take(8)}|sig=${signature.take(12)}|blockTime=$blockTime|gate=$gate|delta=${timeDelta}ms|${target.strategy}:${target.label}")
    }

    /**
     * Log signature rejection with ONE-LINE summary.
     */
    fun logSignatureRejected(
        source: String,
        invoiceId: String,
        signature: String,
        reason: String,
        target: PollingTarget
    ) {
        // ONE-LINE SUMMARY
        Log.w(TAG, "REJECT|$source|${invoiceId.take(8)}|sig=${signature.take(12)}|reason=$reason|${target.strategy}:${target.label}")
    }

    /**
     * Log successful detection with ONE-LINE summary.
     */
    fun logDetectionSuccess(
        source: String,
        invoiceId: String,
        signature: String,
        target: PollingTarget,
        pollCount: Int,
        actualMint: String?,
        warningCode: String?,
        providerName: String
    ) {
        val mintSymbol = when (actualMint) {
            BuildConfig.USDC_MINT -> "USDC"
            BuildConfig.USDT_MINT -> "USDT"
            null -> "SOL"
            else -> "UNKNOWN"
        }

        // ONE-LINE SUMMARY (most important!)
        Log.i(TAG, "✅ FOUND|$source|${invoiceId.take(8)}|sig=${signature.take(16)}|${target.strategy}:${target.label}|poll=$pollCount|mint=$mintSymbol|via=$providerName")

        // Detailed log
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "✅ PAYMENT DETECTED [$source]")
        Log.i(TAG, "  invoiceId:    ${invoiceId}")
        Log.i(TAG, "  signature:    ${signature}")
        Log.i(TAG, "  strategy:     ${target.strategy}")
        Log.i(TAG, "  target:       ${target.label}")
        Log.i(TAG, "  targetAddr:   ${target.address}")
        Log.i(TAG, "  pollCount:    $pollCount")
        Log.i(TAG, "  actualMint:   $actualMint ($mintSymbol)")
        Log.i(TAG, "  provider:     $providerName")
        if (warningCode != null) {
            Log.w(TAG, "  ⚠ WARNING:   $warningCode")
        }
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Log detection timeout with ONE-LINE summary.
     */
    fun logDetectionTimeout(
        source: String,
        invoiceId: String,
        pollCount: Int,
        totalSignaturesSeen: Int,
        fallbackAttempted: Boolean,
        targetCount: Int
    ) {
        // ONE-LINE SUMMARY
        Log.w(TAG, "TIMEOUT|$source|${invoiceId.take(8)}|polls=$pollCount|sigs=$totalSignaturesSeen|fallback=$fallbackAttempted|targets=$targetCount")

        // Detailed log
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.w(TAG, "⏰ DETECTION TIMEOUT [$source]")
        Log.w(TAG, "  invoiceId:         $invoiceId")
        Log.w(TAG, "  totalPolls:        $pollCount")
        Log.w(TAG, "  totalSignatures:   $totalSignaturesSeen")
        Log.w(TAG, "  fallbackAttempted: $fallbackAttempted")
        Log.w(TAG, "  targetCount:       $targetCount")
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Log poll summary (every N polls).
     */
    fun logPollSummary(
        source: String,
        invoiceId: String,
        pollCount: Int,
        elapsedSec: Long,
        remainingSec: Long,
        totalSignatures: Int,
        targetCount: Int,
        fallbackAttempted: Boolean
    ) {
        Log.d(TAG, "POLL|$source|${invoiceId.take(8)}|#$pollCount|elapsed=${elapsedSec}s|remaining=${remainingSec}s|sigs=$totalSignatures|targets=$targetCount|fallback=$fallbackAttempted")
    }
}
