package com.winopay.solana.transaction

import android.util.Log
import com.winopay.solana.rpc.RpcResult
import com.winopay.solana.rpc.SignatureStatus
import com.winopay.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Result of transaction polling.
 */
sealed class PollingResult {
    data object Pending : PollingResult()
    data class Confirmed(val slot: Long, val confirmationStatus: String) : PollingResult()
    data class Failed(val error: String) : PollingResult()
    data object Expired : PollingResult()
    data class Error(val message: String) : PollingResult()
}

/**
 * Callback for polling completion.
 */
fun interface OnPollingComplete {
    suspend fun onComplete(signature: String, result: PollingResult)
}

/**
 * Polls Solana RPC for transaction confirmation status.
 *
 * Usage:
 * 1. Call pollSignature() with signature and callback
 * 2. Callback receives final result (Confirmed/Failed/Expired)
 * 3. Polling auto-cancels on confirmation or timeout
 */
class TransactionPoller(
    private val rpcClient: SolanaRpcClient = SolanaRpcClient()
) {

    companion object {
        private const val TAG = "TransactionPoller"
        private const val DEFAULT_TIMEOUT_MS = 120_000L // 2 minutes
        private const val POLL_INTERVAL_MS = 2_000L // 2 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val _pollingStates = MutableStateFlow<Map<String, PollingResult>>(emptyMap())
    val pollingStates: StateFlow<Map<String, PollingResult>> = _pollingStates.asStateFlow()

    /**
     * Start polling for a transaction signature.
     *
     * @param signature Transaction signature to poll
     * @param timeoutMs Timeout in milliseconds (default 2 minutes)
     * @param onComplete Callback when polling completes
     */
    fun pollSignature(
        signature: String,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onComplete: OnPollingComplete
    ) {
        // Cancel existing poll for this signature
        cancelPoll(signature)

        Log.d(TAG, "Starting poll for signature: $signature")
        updateState(signature, PollingResult.Pending)

        val job = scope.launch {
            try {
                val result = doPoll(signature, timeoutMs)
                updateState(signature, result)
                onComplete.onComplete(signature, result)
            } catch (e: CancellationException) {
                Log.d(TAG, "Polling cancelled for: $signature")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Polling error for $signature: ${e.message}", e)
                val errorResult = PollingResult.Error(e.message ?: "Unknown error")
                updateState(signature, errorResult)
                onComplete.onComplete(signature, errorResult)
            } finally {
                activeJobs.remove(signature)
            }
        }

        activeJobs[signature] = job
    }

    /**
     * Poll until confirmed, failed, or timeout.
     */
    private suspend fun doPoll(signature: String, timeoutMs: Long): PollingResult {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val result = rpcClient.getSignatureStatus(signature)

            when (result) {
                is RpcResult.Success -> {
                    when (val status = result.data) {
                        is SignatureStatus.Confirmed -> {
                            Log.d(TAG, "Transaction confirmed: $signature (${status.confirmationStatus})")
                            return PollingResult.Confirmed(status.slot, status.confirmationStatus)
                        }
                        is SignatureStatus.Failed -> {
                            Log.d(TAG, "Transaction failed: $signature (${status.error})")
                            return PollingResult.Failed(status.error)
                        }
                        is SignatureStatus.Processing -> {
                            Log.d(TAG, "Transaction processing: $signature (slot: ${status.slot})")
                        }
                        is SignatureStatus.NotFound -> {
                            Log.d(TAG, "Transaction not found yet: $signature")
                        }
                    }
                }
                is RpcResult.Error -> {
                    Log.w(TAG, "RPC error while polling: ${result.message}")
                    // Continue polling on RPC errors
                }
            }

            delay(POLL_INTERVAL_MS)
        }

        Log.d(TAG, "Polling timeout for: $signature")
        return PollingResult.Expired
    }

    /**
     * Cancel polling for a signature.
     */
    fun cancelPoll(signature: String) {
        activeJobs[signature]?.let { job ->
            Log.d(TAG, "Cancelling poll for: $signature")
            job.cancel()
            activeJobs.remove(signature)
        }
    }

    /**
     * Cancel all active polls.
     */
    fun cancelAll() {
        Log.d(TAG, "Cancelling all polls (${activeJobs.size} active)")
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    /**
     * Check if a signature is being polled.
     */
    fun isPolling(signature: String): Boolean {
        return activeJobs[signature]?.isActive == true
    }

    /**
     * Get current polling state for a signature.
     */
    fun getState(signature: String): PollingResult? {
        return _pollingStates.value[signature]
    }

    /**
     * Update polling state.
     */
    private fun updateState(signature: String, result: PollingResult) {
        _pollingStates.value = _pollingStates.value + (signature to result)
    }

    /**
     * Clear state for a signature.
     */
    fun clearState(signature: String) {
        _pollingStates.value = _pollingStates.value - signature
    }
}
