package com.winopay.solana.pay

import android.util.Log
import com.winopay.BuildConfig
import com.winopay.data.local.InvoiceEntity
import com.winopay.payments.DetectionLogger
import com.winopay.payments.PaymentRail
import com.winopay.payments.PollingStrategy
import com.winopay.payments.PollingTarget
import com.winopay.solana.SolanaAddressUtils
import com.winopay.solana.rpc.RpcProviderManager
import com.winopay.solana.rpc.SolanaRpcProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.winopay.data.pos.InvoiceTimeouts
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Result of payment detection.
 *
 * For Found/Confirmed with SPL tokens, includes:
 * - actualMintUsed: The mint that was actually paid (may differ for stablecoins)
 * - warningCode: Warning if paid with different stablecoin (USDT instead of USDC or vice versa)
 * - payerAddress: Wallet address of payer (owner for SPL, sender for SOL)
 * - payerTokenAccount: Token account that sent payment (source ATA for SPL)
 */
sealed class PaymentDetectionResult {
    data object Searching : PaymentDetectionResult()
    data class Found(
        val signature: String,
        val isConfirmed: Boolean,
        val actualMintUsed: String? = null,
        val warningCode: String? = null,
        val payerAddress: String? = null,
        val payerTokenAccount: String? = null
    ) : PaymentDetectionResult()
    data class Confirmed(
        val signature: String,
        val actualMintUsed: String? = null,
        val warningCode: String? = null,
        val payerAddress: String? = null,
        val payerTokenAccount: String? = null
    ) : PaymentDetectionResult()
    data class Invalid(val signature: String, val reason: String) : PaymentDetectionResult()
    data object Expired : PaymentDetectionResult()
    data class Error(val message: String) : PaymentDetectionResult()
}

/**
 * Callback for payment detection completion.
 */
fun interface OnPaymentDetected {
    suspend fun onDetected(invoiceId: String, result: PaymentDetectionResult)
}

/**
 * Detects Solana Pay payments by polling the MERCHANT ADDRESS.
 *
 * AMOUNT-ONLY DETECTION:
 * Many wallets do NOT include reference/memo from Solana Pay QR codes.
 * We poll the merchant's token account (or wallet for SOL) and validate
 * incoming transactions by AMOUNT + MINT + RECIPIENT + BLOCKTIME.
 *
 * SINGLE-ACTIVE-INVOICE INVARIANT:
 * Only ONE invoice can be active at a time (enforced at DB/PosManager level).
 * This ensures amount-only detection doesn't match wrong payments.
 *
 * SECURITY: Uses TransactionValidator for STRICT validation.
 * - Recipient MUST receive tokens
 * - Amount MUST be >= expected
 * - Transaction MUST be after invoice creation
 * - Signatures are iterated until a VALID one is found
 *
 * MULTI-RPC FAILOVER:
 * Uses RpcProviderManager for production-grade reliability.
 * If primary RPC is blocked, automatically fails over to backups.
 * Same provider is used for getSignatures + getTransaction for consistency.
 *
 * Flow:
 * 1. Poll getSignaturesForAddress(merchantAddress) with failover
 * 2. Iterate signatures (newest first), validate each strictly
 * 3. Use same provider for validation (cross-call consistency)
 * 4. First valid transaction is accepted
 * 5. Check confirmation status
 * 6. Callback with result
 *
 * DEPENDENCY INJECTION:
 * Detector is owned by PaymentRail, which passes itself via constructor.
 * This breaks circular dependency (detector doesn't create rail).
 */
class SolanaPayPaymentDetector(
    private val paymentRail: PaymentRail
) {

    companion object {
        private const val TAG = "PaymentDetector"

        /** Number of poll iterations before trying fallback strategy */
        private const val FALLBACK_AFTER_POLLS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    // Track checked signatures per invoice to avoid re-validation
    private val checkedSignatures = mutableMapOf<String, MutableSet<String>>()

    private val _detectionStates = MutableStateFlow<Map<String, PaymentDetectionResult>>(emptyMap())
    val detectionStates: StateFlow<Map<String, PaymentDetectionResult>> = _detectionStates.asStateFlow()

    /**
     * Start detecting payment for an invoice.
     *
     * @param invoice Invoice to detect payment for
     * @param timeoutMs Timeout in milliseconds
     * @param onDetected Callback when detection completes
     */
    fun startDetection(
        invoice: InvoiceEntity,
        timeoutMs: Long = InvoiceTimeouts.DEFAULT_TIMEOUT_MS,
        onDetected: OnPaymentDetected
    ) {
        // Cancel existing detection for this invoice
        cancelDetection(invoice.id)

        // Initialize checked signatures set for this invoice
        checkedSignatures[invoice.id] = mutableSetOf()

        // Get all polling targets (may include both USDC and USDT ATAs for stablecoins)
        val pollingTargets = paymentRail.derivePollingTargets(invoice)

        // ONE-LINE LOG for easy filtering + detailed log
        DetectionLogger.logDetectionStart("FOREGROUND", invoice, pollingTargets)
        updateState(invoice.id, PaymentDetectionResult.Searching)

        val job = scope.launch {
            try {
                val result = detectPayment(invoice, timeoutMs)
                updateState(invoice.id, result)
                onDetected.onDetected(invoice.id, result)
            } catch (e: CancellationException) {
                Log.d(TAG, "Detection cancelled for: ${invoice.id}")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Detection error for ${invoice.id}: ${e.message}", e)
                val errorResult = PaymentDetectionResult.Error(e.message ?: "Unknown error")
                updateState(invoice.id, errorResult)
                onDetected.onDetected(invoice.id, errorResult)
            } finally {
                activeJobs.remove(invoice.id)
                checkedSignatures.remove(invoice.id)
            }
        }

        activeJobs[invoice.id] = job
    }

    /**
     * Poll for payment and validate STRICTLY with multi-RPC failover.
     *
     * MULTI-STRATEGY DETECTION:
     * 1. Strategy A (DERIVED_ATA): Poll derived ATAs (fast, deterministic)
     * 2. Strategy B (DISCOVERED_ACCOUNT): After FALLBACK_AFTER_POLLS with no signatures,
     *    discover actual token accounts via RPC and poll those too
     * 3. Continue polling all targets until payment found or timeout
     *
     * AMOUNT-ONLY DETECTION:
     * Polls the MERCHANT ADDRESS (not reference) since wallets often omit reference.
     * Validates by: recipient + amount + mint + blockTime.
     * Single-active-invoice rule ensures we don't match wrong payments.
     *
     * DUAL STABLECOIN ACCEPTANCE:
     * For stablecoin invoices with dual acceptance enabled, polls BOTH
     * USDC ATA and USDT ATA to detect payments in either stablecoin.
     *
     * CONSISTENCY: Uses same provider for getSignatures + getTransaction
     * to avoid cross-provider inconsistency.
     */
    private suspend fun detectPayment(
        invoice: InvoiceEntity,
        timeoutMs: Long
    ): PaymentDetectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val checked = checkedSignatures[invoice.id] ?: mutableSetOf()

        // STRATEGY A: Start with derived ATAs
        var pollingTargets = paymentRail.derivePollingTargets(invoice).toMutableList()

        // Track poll iterations and whether fallback has been tried
        var pollCount = 0
        var fallbackAttempted = false
        var totalSignaturesSeen = 0

        // DIAGNOSTIC: Track if we've done initial diagnostic dump
        var diagnosticDumpDone = false

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            pollCount++

            // DIAGNOSTIC: On first poll iteration, check wallet vs ATA signatures
            if (!diagnosticDumpDone && invoice.splTokenMint != null) {
                diagnosticDumpDone = true
                performDiagnosticDump(invoice.recipientAddress, pollingTargets)
            }

            // STRATEGY B FALLBACK: After FALLBACK_AFTER_POLLS with no new signatures, discover accounts
            if (!fallbackAttempted && pollCount >= FALLBACK_AFTER_POLLS && totalSignaturesSeen == 0 && invoice.splTokenMint != null) {
                fallbackAttempted = true

                val discoveredTargets = paymentRail.discoverTokenAccountTargets(invoice)

                // ONE-LINE LOG for fallback
                DetectionLogger.logFallbackTriggered(
                    source = "FOREGROUND",
                    invoiceId = invoice.id,
                    pollCount = pollCount,
                    reason = "$pollCount polls with 0 signatures",
                    discoveredTargets = discoveredTargets
                )

                if (discoveredTargets.isNotEmpty()) {
                    pollingTargets.addAll(discoveredTargets)
                }
            }

            // Poll ALL targets for signatures
            var signaturesThisPoll = 0

            for (target in pollingTargets) {
                val signaturesResult = TransactionValidator.getSignaturesForAddressWithProvider(
                    address = target.address,
                    limit = 10
                )

                val signaturesWithTime = signaturesResult.signatures
                val sigProvider = signaturesResult.provider

                if (signaturesWithTime.isNotEmpty()) {
                    signaturesThisPoll += signaturesWithTime.size
                    // ONE-LINE LOG for signatures found
                    DetectionLogger.logSignatureFound(
                        source = "FOREGROUND",
                        invoiceId = invoice.id,
                        pollCount = pollCount,
                        target = target,
                        signatureCount = signaturesWithTime.size,
                        providerName = signaturesResult.providerName
                    )
                }

                // Iterate through signatures (newest first)
                for ((signature, blockTime) in signaturesWithTime) {
                    // Skip already checked signatures
                    if (signature in checked) {
                        continue
                    }
                    checked.add(signature)

                    // ONE-LINE LOG for signature check
                    DetectionLogger.logSignatureCheck(
                        source = "FOREGROUND",
                        invoiceId = invoice.id,
                        signature = signature,
                        blockTime = blockTime,
                        invoiceCreatedAt = invoice.createdAt,
                        target = target
                    )

                    if (invoice.createdAt <= 0L) {
                        DetectionLogger.logSignatureRejected("FOREGROUND", invoice.id, signature, "invalid_createdAt", target)
                        continue
                    }

                    if (blockTime <= 0L) {
                        DetectionLogger.logSignatureRejected("FOREGROUND", invoice.id, signature, "missing_blockTime", target)
                        continue
                    }

                    // Skip if transaction predates invoice
                    if (blockTime < invoice.createdAt) {
                        DetectionLogger.logSignatureRejected("FOREGROUND", invoice.id, signature, "predates_invoice", target)
                        continue
                    }

                    // Validate strictly using shared validator (AMOUNT-ONLY)
                    // IMPORTANT: Pass the provider that found signatures for consistency
                    val validationParams = TransactionValidator.ValidationParams(
                        signature = signature,
                        expectedRecipient = invoice.recipientAddress,
                        expectedAmountMinor = invoice.amount,
                        expectedCurrency = invoice.currency,
                        expectedMint = invoice.splTokenMint,
                        invoiceNetworkId = invoice.networkId,
                        invoiceCreatedAt = invoice.createdAt,
                        preferredProvider = sigProvider  // Use same provider for consistency
                    )

                    val validationResult = TransactionValidator.validateTransaction(validationParams)

                    when (validationResult) {
                        is TransactionValidator.Result.Valid -> {
                            val actualMint = validationResult.actualMintUsed
                            val warningCode = validationResult.warningCode
                            val payerAddress = validationResult.payerAddress
                            val payerTokenAccount = validationResult.payerTokenAccount

                            // ONE-LINE LOG for detection success
                            DetectionLogger.logDetectionSuccess(
                                source = "FOREGROUND",
                                invoiceId = invoice.id,
                                signature = signature,
                                target = target,
                                pollCount = pollCount,
                                actualMint = actualMint,
                                warningCode = warningCode,
                                providerName = signaturesResult.providerName
                            )
                            Log.d(TAG, "  Payer extracted: address=$payerAddress, tokenAccount=$payerTokenAccount")

                            // Check confirmation status (use same provider)
                            val isConfirmed = TransactionValidator.checkConfirmation(signature, sigProvider)
                            return@withContext if (isConfirmed) {
                                PaymentDetectionResult.Confirmed(
                                    signature = signature,
                                    actualMintUsed = actualMint,
                                    warningCode = warningCode,
                                    payerAddress = payerAddress,
                                    payerTokenAccount = payerTokenAccount
                                )
                            } else {
                                PaymentDetectionResult.Found(
                                    signature = signature,
                                    isConfirmed = false,
                                    actualMintUsed = actualMint,
                                    warningCode = warningCode,
                                    payerAddress = payerAddress,
                                    payerTokenAccount = payerTokenAccount
                                )
                            }
                        }
                        is TransactionValidator.Result.Invalid -> {
                            DetectionLogger.logSignatureRejected("FOREGROUND", invoice.id, signature, validationResult.reason, target)
                            // Continue to next signature
                        }
                        is TransactionValidator.Result.Error -> {
                            DetectionLogger.logSignatureRejected("FOREGROUND", invoice.id, signature, "error:${validationResult.message}", target)
                            // Continue to next signature
                        }
                    }
                }
            }

            totalSignaturesSeen += signaturesThisPoll

            // Log poll summary periodically
            if (pollCount % 5 == 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                val remaining = (timeoutMs - (System.currentTimeMillis() - startTime)) / 1000
                DetectionLogger.logPollSummary(
                    source = "FOREGROUND",
                    invoiceId = invoice.id,
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

        // ONE-LINE LOG for timeout
        DetectionLogger.logDetectionTimeout(
            source = "FOREGROUND",
            invoiceId = invoice.id,
            pollCount = pollCount,
            totalSignaturesSeen = totalSignaturesSeen,
            fallbackAttempted = fallbackAttempted,
            targetCount = pollingTargets.size
        )
        PaymentDetectionResult.Expired
    }

    /**
     * Cancel detection for an invoice.
     */
    fun cancelDetection(invoiceId: String) {
        activeJobs[invoiceId]?.let { job ->
            Log.d(TAG, "Cancelling detection for: $invoiceId")
            job.cancel()
            activeJobs.remove(invoiceId)
            checkedSignatures.remove(invoiceId)
        }
    }

    /**
     * Cancel all active detections.
     */
    fun cancelAll() {
        Log.d(TAG, "Cancelling all detections (${activeJobs.size} active)")
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        checkedSignatures.clear()
    }

    /**
     * Check if detection is active for an invoice.
     */
    fun isDetecting(invoiceId: String): Boolean {
        return activeJobs[invoiceId]?.isActive == true
    }

    /**
     * Get current detection state for an invoice.
     */
    fun getState(invoiceId: String): PaymentDetectionResult? {
        return _detectionStates.value[invoiceId]
    }

    private fun updateState(invoiceId: String, result: PaymentDetectionResult) {
        _detectionStates.value = _detectionStates.value + (invoiceId to result)
    }

    /**
     * DIAGNOSTIC: Compare wallet signatures vs ATA signatures.
     *
     * If ATA has no signatures but wallet does, likely causes:
     * 1. Mint mismatch - user sent different "USDC" than app expects
     * 2. Wrong ATA derivation
     */
    private fun performDiagnosticDump(
        ownerWallet: String,
        pollingTargets: List<PollingTarget>
    ) {
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.w(TAG, "DIAGNOSTIC DUMP: Wallet vs ATA Signatures")
        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.w(TAG, "  Owner wallet: $ownerWallet")
        Log.w(TAG, "  Expected USDC mint: ${BuildConfig.USDC_MINT}")
        Log.w(TAG, "  Expected USDT mint: ${BuildConfig.USDT_MINT.ifBlank { "(not configured)" }}")

        // Check owner wallet signatures
        val walletSigs = TransactionValidator.getSignaturesForAddressWithProvider(ownerWallet, 5)
        Log.w(TAG, "  ┌── OWNER WALLET SIGNATURES: ${walletSigs.signatures.size}")
        walletSigs.signatures.take(3).forEach { (sig, blockTime) ->
            Log.w(TAG, "  │   $sig (blockTime=$blockTime)")
        }

        // Check each polling target (ATA)
        var ataTotalSigs = 0
        for (target in pollingTargets) {
            val ataSigs = TransactionValidator.getSignaturesForAddressWithProvider(target.address, 5)
            ataTotalSigs += ataSigs.signatures.size
            Log.w(TAG, "  ├── ${target.label} (${target.address.take(12)}...): ${ataSigs.signatures.size} signatures")
            if (target.tokenMint != null) {
                Log.w(TAG, "  │   mint: ${target.tokenMint}")
            }
            ataSigs.signatures.take(3).forEach { (sig, blockTime) ->
                Log.w(TAG, "  │     $sig (blockTime=$blockTime)")
            }
        }

        // Alert if wallet has sigs but ATAs don't
        if (walletSigs.signatures.isNotEmpty() && ataTotalSigs == 0) {
            Log.e(TAG, "  ╔═══════════════════════════════════════════════════════════╗")
            Log.e(TAG, "  ║ ⚠️ MINT MISMATCH SUSPECTED!                               ║")
            Log.e(TAG, "  ║ Owner wallet has signatures but derived ATAs have NONE.   ║")
            Log.e(TAG, "  ║ User likely sent tokens with DIFFERENT mint than:         ║")
            Log.e(TAG, "  ║   ${BuildConfig.USDC_MINT} ")
            Log.e(TAG, "  ║ Check if user's USDC is from Circle faucet (correct)      ║")
            Log.e(TAG, "  ║ or another devnet mint (wrong).                           ║")
            Log.e(TAG, "  ╚═══════════════════════════════════════════════════════════╝")
        }

        Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
