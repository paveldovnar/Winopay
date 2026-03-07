package com.winopay.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.winopay.WinoPayApplication
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.pos.InvoiceTimeouts
import com.winopay.data.local.PaymentCurrency
import com.winopay.payments.DetectionLogger
import com.winopay.payments.PaymentRail
import com.winopay.payments.PaymentRailFactory
import com.winopay.payments.PollingTarget
import com.winopay.payments.ValidationParams
import com.winopay.payments.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Background worker for detecting Solana Pay payments (AMOUNT-ONLY detection).
 *
 * AMOUNT-ONLY DETECTION:
 * Many wallets do NOT include reference/memo from Solana Pay QR codes.
 * We poll the MERCHANT ADDRESS and validate incoming transactions by
 * AMOUNT + MINT + RECIPIENT + BLOCKTIME.
 *
 * SINGLE-ACTIVE-INVOICE INVARIANT:
 * Only ONE invoice can be active at a time (enforced at DB/PosManager level).
 * This ensures amount-only detection doesn't match wrong payments.
 *
 * SECURITY: Uses TransactionValidator for STRICT validation (shared with foreground detector).
 * - Recipient MUST receive tokens
 * - Amount MUST be >= expected
 * - Transaction MUST be after invoice creation
 * - Signatures are iterated until a VALID one is found
 *
 * MULTI-RPC FAILOVER:
 * Uses RpcProviderManager for production-grade reliability.
 * Same failover logic as foreground detector (single source of truth).
 *
 * Runs independently of UI lifecycle, ensuring payment detection
 * continues even when app is backgrounded or killed.
 *
 * Input data:
 * - INVOICE_ID: Invoice identifier
 * - EXPECTED_RECIPIENT: Merchant wallet address (used for polling)
 * - EXPECTED_MINT: SPL token mint (null for SOL)
 * - EXPECTED_AMOUNT_MINOR: Amount in minor units
 * - DEADLINE_AT: Timestamp when detection should stop
 * - CURRENCY: Payment currency (SOL/USDC)
 * - INVOICE_CREATED_AT: When invoice was created (for blockTime filtering)
 */
class InvoiceDetectionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "InvoiceDetectionWorker"

        /** Number of poll iterations before trying fallback strategy */
        private const val FALLBACK_AFTER_POLLS = 3

        // Input keys
        const val KEY_INVOICE_ID = "invoice_id"
        const val KEY_POLLING_ADDRESS = "polling_address"  // ATA for SPL, wallet for SOL
        const val KEY_IS_ATA_POLLING = "is_ata_polling"    // true if polling ATA
        const val KEY_EXPECTED_RECIPIENT = "expected_recipient"  // Owner wallet (for validation)
        const val KEY_EXPECTED_MINT = "expected_mint"
        const val KEY_EXPECTED_AMOUNT_MINOR = "expected_amount_minor"
        const val KEY_DEADLINE_AT = "deadline_at"
        const val KEY_CURRENCY = "currency"
        const val KEY_INVOICE_CREATED_AT = "invoice_created_at"
        const val KEY_INVOICE_NETWORK_ID = "invoice_network_id"

        // Output keys
        const val KEY_RESULT_STATUS = "result_status"
        const val KEY_RESULT_SIGNATURE = "result_signature"
        const val KEY_RESULT_ERROR = "result_error"
        const val KEY_RESULT_ACTUAL_MINT = "result_actual_mint"
        const val KEY_RESULT_WARNING_CODE = "result_warning_code"

        // Result statuses
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_FAILED = "failed"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_ERROR = "error"
    }

    private val app by lazy { applicationContext as WinoPayApplication }

    // Track checked signatures to avoid re-validation
    private val checkedSignatures = mutableSetOf<String>()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Extract input data
        val invoiceId = inputData.getString(KEY_INVOICE_ID)
            ?: return@withContext failWithError("Missing invoice_id")
        val pollingAddress = inputData.getString(KEY_POLLING_ADDRESS)
            ?: return@withContext failWithError("Missing polling_address")
        val isAtaPolling = inputData.getBoolean(KEY_IS_ATA_POLLING, false)
        val expectedRecipient = inputData.getString(KEY_EXPECTED_RECIPIENT)
            ?: return@withContext failWithError("Missing expected_recipient")
        val expectedMint = inputData.getString(KEY_EXPECTED_MINT)
        val expectedAmountMinor = inputData.getLong(KEY_EXPECTED_AMOUNT_MINOR, -1L)
        val deadlineAt = inputData.getLong(KEY_DEADLINE_AT, 0L)
        val invoiceCreatedAt = inputData.getLong(KEY_INVOICE_CREATED_AT, 0L)
        val invoiceNetworkId = inputData.getString(KEY_INVOICE_NETWORK_ID) ?: ""
        val currencyStr = inputData.getString(KEY_CURRENCY) ?: "USDC"
        val currency = try {
            PaymentCurrency.valueOf(currencyStr)
        } catch (e: Exception) {
            PaymentCurrency.USDC
        }

        if (expectedAmountMinor < 0 || deadlineAt == 0L || invoiceCreatedAt <= 0L || invoiceNetworkId.isBlank()) {
            return@withContext failWithError("Invalid input parameters")
        }

        // Get PaymentRail for this invoice's network
        val paymentRail: PaymentRail = try {
            PaymentRailFactory.getRail(invoiceNetworkId)
        } catch (e: IllegalArgumentException) {
            return@withContext failWithError("Unsupported network: $invoiceNetworkId")
        }

        // Load invoice from database for polling targets derivation
        val invoice = app.invoiceRepository.getInvoice(invoiceId)
            ?: return@withContext failWithError("Invoice not found: $invoiceId")

        // STRATEGY A: Get polling targets from PaymentRail (derived ATAs)
        val pollingTargets = paymentRail.derivePollingTargets(invoice).toMutableList()

        // ONE-LINE LOG for easy filtering + detailed log
        DetectionLogger.logDetectionStart("WORKER", invoice, pollingTargets)

        // Track poll iterations for fallback
        var pollCount = 0
        var fallbackAttempted = false
        var totalSignaturesSeen = 0

        // Track current deadline (may be extended via DB)
        var currentDeadline = deadlineAt

        // Poll loop - runs until terminal state or cancelled
        // SOFT-EXPIRE: Worker does NOT auto-expire. Only UI (PosManager) can mark as EXPIRED.
        while (true) {
            pollCount++
            // Check if work is cancelled
            if (isStopped) {
                Log.d(TAG, "Worker stopped for invoice: $invoiceId")
                return@withContext Result.success()
            }

            // IDEMPOTENCY CHECK: Skip if invoice in terminal state (race with foreground/user action)
            val currentInvoice = app.invoiceRepository.getInvoice(invoiceId)
            if (currentInvoice == null) {
                Log.w(TAG, "Invoice no longer exists, stopping worker: $invoiceId")
                return@withContext Result.success()
            }

            // SOFT-EXPIRE: Read deadline from DB (may have been extended)
            val dbDeadline = currentInvoice.deadlineAt
            if (dbDeadline > currentDeadline) {
                Log.i(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|DEADLINE_EXTENDED|old=$currentDeadline|new=$dbDeadline")
                currentDeadline = dbDeadline
            }

            // Check ALL terminal states - stop polling if invoice is no longer active
            when (currentInvoice.status) {
                InvoiceStatus.CONFIRMED -> {
                    Log.i(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|STOP_REASON=CONFIRMED")
                    return@withContext Result.success(
                        workDataOf(
                            KEY_RESULT_STATUS to STATUS_CONFIRMED,
                            KEY_RESULT_SIGNATURE to (currentInvoice.foundSignature ?: "")
                        )
                    )
                }
                InvoiceStatus.CANCELED -> {
                    Log.i(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|STOP_REASON=CANCELED")
                    return@withContext Result.success()
                }
                InvoiceStatus.EXPIRED -> {
                    // SOFT-EXPIRE: Invoice was marked EXPIRED by UI (user clicked "Revoke")
                    Log.i(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|STOP_REASON=EXPIRED_BY_UI")
                    return@withContext Result.success(
                        workDataOf(KEY_RESULT_STATUS to STATUS_EXPIRED)
                    )
                }
                InvoiceStatus.FAILED -> {
                    Log.i(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|STOP_REASON=FAILED")
                    return@withContext Result.success(
                        workDataOf(KEY_RESULT_STATUS to STATUS_FAILED)
                    )
                }
                // CREATED, PENDING - continue polling
                InvoiceStatus.CREATED, InvoiceStatus.PENDING -> {
                    // Check if deadline passed (but DON'T auto-expire!)
                    if (currentDeadline > 0 && System.currentTimeMillis() > currentDeadline) {
                        // Log soft-expire reached but continue polling
                        // UI will show dialog, user will either extend or revoke
                        if (pollCount % 10 == 0) {
                            Log.d(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|PAST_DEADLINE|poll=$pollCount|awaiting_ui_decision")
                        }
                    }
                }
            }

            // STRATEGY B FALLBACK: After FALLBACK_AFTER_POLLS with no signatures, discover accounts
            if (!fallbackAttempted && pollCount >= FALLBACK_AFTER_POLLS && totalSignaturesSeen == 0 && expectedMint != null) {
                fallbackAttempted = true

                val discoveredTargets = paymentRail.discoverTokenAccountTargets(invoice)

                // ONE-LINE LOG for fallback
                DetectionLogger.logFallbackTriggered(
                    source = "WORKER",
                    invoiceId = invoiceId,
                    pollCount = pollCount,
                    reason = "$pollCount polls with 0 signatures",
                    discoveredTargets = discoveredTargets
                )

                if (discoveredTargets.isNotEmpty()) {
                    pollingTargets.addAll(discoveredTargets)
                }
            }

            // Poll ALL targets for candidates (dual stablecoin + fallback support)
            var signaturesThisPoll = 0
            for (target in pollingTargets) {
                val candidateResult = paymentRail.listCandidates(target, limit = 10)

                val candidates = candidateResult.candidates
                val providerHint = candidateResult.providerHint
                signaturesThisPoll += candidates.size

                if (candidates.isNotEmpty()) {
                    // ONE-LINE LOG for signatures found
                    DetectionLogger.logSignatureFound(
                        source = "WORKER",
                        invoiceId = invoiceId,
                        pollCount = pollCount,
                        target = target,
                        signatureCount = candidates.size,
                        providerName = candidateResult.providerName
                    )
                }

                // Iterate through candidates (newest first)
                for (candidate in candidates) {
                    val txId = candidate.transactionId
                    val blockTime = candidate.blockTime

                    // Skip already checked transactions
                    if (txId in checkedSignatures) {
                        continue
                    }
                    checkedSignatures.add(txId)

                    // ONE-LINE LOG for signature check
                    DetectionLogger.logSignatureCheck(
                        source = "WORKER",
                        invoiceId = invoiceId,
                        signature = txId,
                        blockTime = blockTime,
                        invoiceCreatedAt = invoiceCreatedAt,
                        target = target
                    )

                    if (blockTime <= 0L) {
                        DetectionLogger.logSignatureRejected("WORKER", invoiceId, txId, "missing_blockTime", target)
                        continue
                    }

                    // Skip if transaction predates invoice
                    if (blockTime < invoiceCreatedAt) {
                        DetectionLogger.logSignatureRejected("WORKER", invoiceId, txId, "predates_invoice", target)
                        continue
                    }

                    // Validate using PaymentRail
                    val validationParams = ValidationParams(
                        transactionId = txId,
                        expectedRecipient = expectedRecipient,
                        expectedAmountMinor = expectedAmountMinor,
                        expectedTokenMint = expectedMint,
                        invoiceNetworkId = invoiceNetworkId,
                        invoiceCreatedAt = invoiceCreatedAt,
                        providerHint = providerHint
                    )

                    val validationResult = paymentRail.validateCandidate(validationParams)

                    when (validationResult) {
                        is ValidationResult.Valid -> {
                            val actualMint = validationResult.actualTokenUsed
                            val warningCode = validationResult.warningCode
                            val payerAddress = validationResult.payerAddress
                            val payerTokenAccount = validationResult.payerTokenAccount

                            // Check confirmation status
                            val isConfirmed = paymentRail.checkConfirmation(txId, providerHint)

                            if (isConfirmed) {
                                // Update database with dual stablecoin info + payer info
                                app.invoiceRepository.confirmInvoice(
                                    invoiceId = invoiceId,
                                    signature = txId,
                                    actualMintUsed = actualMint,
                                    warningCode = warningCode,
                                    payerAddress = payerAddress,
                                    payerTokenAccount = payerTokenAccount
                                )

                                // ONE-LINE LOG for detection success
                                DetectionLogger.logDetectionSuccess(
                                    source = "WORKER",
                                    invoiceId = invoiceId,
                                    signature = txId,
                                    target = target,
                                    pollCount = pollCount,
                                    actualMint = actualMint,
                                    warningCode = warningCode,
                                    providerName = candidateResult.providerName
                                )

                                return@withContext Result.success(
                                    workDataOf(
                                        KEY_RESULT_STATUS to STATUS_CONFIRMED,
                                        KEY_RESULT_SIGNATURE to txId,
                                        KEY_RESULT_ACTUAL_MINT to (actualMint ?: ""),
                                        KEY_RESULT_WARNING_CODE to (warningCode ?: "")
                                    )
                                )
                            } else {
                                // Transaction found but not yet confirmed, mark pending and continue
                                app.invoiceRepository.markPending(
                                    invoiceId = invoiceId,
                                    signature = txId,
                                    actualMintUsed = actualMint,
                                    warningCode = warningCode,
                                    payerAddress = payerAddress,
                                    payerTokenAccount = payerTokenAccount
                                )
                                Log.d(TAG, "  Transaction found but not confirmed yet, continuing poll...")
                            }
                        }
                        is ValidationResult.Invalid -> {
                            DetectionLogger.logSignatureRejected("WORKER", invoiceId, txId, validationResult.reason, target)
                            // Continue to next candidate
                        }
                        is ValidationResult.Error -> {
                            DetectionLogger.logSignatureRejected("WORKER", invoiceId, txId, "error:${validationResult.message}", target)
                            // Continue to next candidate
                        }
                    }
                }
            }

            totalSignaturesSeen += signaturesThisPoll

            // Log poll summary periodically
            if (pollCount % 5 == 0) {
                val elapsed = (System.currentTimeMillis() - (deadlineAt - InvoiceTimeouts.DEFAULT_TIMEOUT_MS)) / 1000
                val remaining = (deadlineAt - System.currentTimeMillis()) / 1000
                DetectionLogger.logPollSummary(
                    source = "WORKER",
                    invoiceId = invoiceId,
                    pollCount = pollCount,
                    elapsedSec = elapsed,
                    remainingSec = remaining,
                    totalSignatures = totalSignaturesSeen,
                    targetCount = pollingTargets.size,
                    fallbackAttempted = fallbackAttempted
                )
            }

            delay(InvoiceTimeouts.POLL_INTERVAL_MS)
        }

        // This point is reached only if the while(true) loop is broken somehow
        // (should not happen in normal flow - loop exits via return statements)
        Log.w(TAG, "SOFT_EXPIRE|WORKER|${invoiceId.take(8)}|UNEXPECTED_LOOP_EXIT")
        Result.success()
    }

    private fun failWithError(message: String): Result {
        Log.e(TAG, "Worker failed: $message")
        return Result.failure(
            workDataOf(
                KEY_RESULT_STATUS to STATUS_ERROR,
                KEY_RESULT_ERROR to message
            )
        )
    }
}
