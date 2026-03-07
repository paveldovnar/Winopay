package com.winopay.data.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.pos.InvoiceTimeouts
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background payment detection using WorkManager.
 *
 * AMOUNT-ONLY DETECTION:
 * Polls the merchant address (not reference) since many wallets
 * don't include reference in transactions.
 *
 * Ensures payment detection continues even when app is backgrounded or killed.
 * Each invoice gets a unique work request that polls for payment until:
 * - Payment detected and confirmed
 * - Deadline reached (expired)
 * - Work cancelled (invoice cancelled/failed)
 */
object DetectionScheduler {

    private const val TAG = "DetectionScheduler"
    private const val WORK_NAME_PREFIX = "invoice_detection_"

    /**
     * Schedule detection for an invoice.
     *
     * Creates a unique WorkManager job that will poll for payment
     * until confirmed, failed, or deadline reached.
     *
     * @param context Application context
     * @param invoice Invoice to monitor
     * @param timeoutMs Optional timeout override (default 5 minutes from InvoiceTimeouts)
     */
    fun scheduleDetection(
        context: Context,
        invoice: InvoiceEntity,
        timeoutMs: Long = InvoiceTimeouts.DEFAULT_TIMEOUT_MS
    ) {
        val deadlineAt = System.currentTimeMillis() + timeoutMs

        scheduleDetectionWithDeadline(
            context = context,
            invoice = invoice,
            deadlineAt = deadlineAt
        )
    }

    /**
     * Schedule detection with explicit deadline.
     * Used for resuming detection after app restart.
     */
    fun scheduleDetectionWithDeadline(
        context: Context,
        invoice: InvoiceEntity,
        deadlineAt: Long
    ) {
        // Don't schedule for terminal states
        if (invoice.status !in listOf(InvoiceStatus.CREATED, InvoiceStatus.PENDING)) {
            Log.d(TAG, "Skipping schedule for invoice ${invoice.id}: status=${invoice.status}")
            return
        }

        // Check if deadline already passed
        if (System.currentTimeMillis() >= deadlineAt) {
            Log.d(TAG, "Skipping schedule for invoice ${invoice.id}: deadline already passed")
            return
        }

        // Get correct polling address: ATA for SPL tokens, wallet for SOL
        val pollingAddress = invoice.getPollingAddress()
        val isAtaPolling = invoice.requiresAtaPolling()

        // Pass both recipient (for validation) and polling address (for detection)
        val inputData = workDataOf(
            InvoiceDetectionWorker.KEY_INVOICE_ID to invoice.id,
            InvoiceDetectionWorker.KEY_POLLING_ADDRESS to pollingAddress,
            InvoiceDetectionWorker.KEY_IS_ATA_POLLING to isAtaPolling,
            InvoiceDetectionWorker.KEY_EXPECTED_RECIPIENT to invoice.recipientAddress,
            InvoiceDetectionWorker.KEY_EXPECTED_MINT to invoice.splTokenMint,
            InvoiceDetectionWorker.KEY_EXPECTED_AMOUNT_MINOR to invoice.amount,
            InvoiceDetectionWorker.KEY_DEADLINE_AT to deadlineAt,
            InvoiceDetectionWorker.KEY_CURRENCY to invoice.currency.name,
            InvoiceDetectionWorker.KEY_INVOICE_CREATED_AT to invoice.createdAt,
            InvoiceDetectionWorker.KEY_INVOICE_NETWORK_ID to invoice.networkId
        )

        val workRequest = OneTimeWorkRequestBuilder<InvoiceDetectionWorker>()
            .setInputData(inputData)
            .addTag(getWorkTag(invoice.id))
            .build()

        val workName = getWorkName(invoice.id)

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        val remainingSecs = (deadlineAt - System.currentTimeMillis()) / 1000
        val pollingType = if (isAtaPolling) "ATA (token account)" else "WALLET (direct)"
        Log.d(TAG, "━━━━━ SCHEDULED DETECTION (AMOUNT-ONLY) ━━━━━")
        Log.d(TAG, "Invoice: ${invoice.id}")
        Log.d(TAG, "Currency: ${invoice.currency}")
        Log.d(TAG, "Polling address: $pollingAddress")
        Log.d(TAG, "Polling type: $pollingType")
        if (isAtaPolling) {
            Log.d(TAG, "Owner wallet: ${invoice.recipientAddress}")
            Log.d(TAG, "Token mint: ${invoice.splTokenMint}")
        }
        Log.d(TAG, "Amount: ${invoice.amount} (minor units)")
        Log.d(TAG, "Deadline: ${remainingSecs}s remaining")
        Log.d(TAG, "Work name: $workName")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Cancel detection for an invoice.
     * Call when invoice is cancelled, confirmed, or failed.
     */
    fun cancelDetection(context: Context, invoiceId: String) {
        val workName = getWorkName(invoiceId)
        WorkManager.getInstance(context).cancelUniqueWork(workName)
        Log.d(TAG, "Cancelled detection for invoice: $invoiceId")
    }

    /**
     * Resume detection for pending invoices on app start.
     * Call from Application.onCreate() to ensure detection continues
     * for invoices that were active when app was killed.
     *
     * @param context Application context
     * @param invoices List of pending invoices from database
     * @param timeoutMs Remaining timeout for each invoice
     */
    suspend fun resumePendingDetections(
        context: Context,
        invoices: List<InvoiceEntity>,
        timeoutMs: Long = InvoiceTimeouts.DEFAULT_TIMEOUT_MS
    ) {
        if (invoices.isEmpty()) {
            Log.d(TAG, "No pending invoices to resume")
            return
        }

        Log.d(TAG, "━━━━━ RESUMING ${invoices.size} DETECTIONS ━━━━━")

        invoices.forEach { invoice ->
            // Calculate remaining time based on creation time
            val elapsedMs = System.currentTimeMillis() - invoice.createdAt
            val remainingMs = timeoutMs - elapsedMs

            if (remainingMs > 0) {
                val deadlineAt = System.currentTimeMillis() + remainingMs
                scheduleDetectionWithDeadline(
                    context = context,
                    invoice = invoice,
                    deadlineAt = deadlineAt
                )
                Log.d(TAG, "Resumed: ${invoice.id} (${remainingMs / 1000}s remaining)")
            } else {
                Log.d(TAG, "Skipped expired: ${invoice.id}")
            }
        }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    /**
     * Cancel all pending detection work.
     * Useful for testing or cleanup.
     */
    fun cancelAllDetections(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        Log.d(TAG, "Cancelled all detection work")
    }

    /**
     * Get unique work name for an invoice.
     */
    private fun getWorkName(invoiceId: String): String {
        return "$WORK_NAME_PREFIX$invoiceId"
    }

    /**
     * Get work tag for an invoice.
     */
    private fun getWorkTag(invoiceId: String): String {
        return TAG
    }
}
