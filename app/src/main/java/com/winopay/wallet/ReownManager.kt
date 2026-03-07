package com.winopay.wallet

import android.app.Application
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.winopay.BuildConfig
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * Manages Reown (WalletConnect v2) SDK initialization.
 *
 * SAFETY: All methods are safe to call even if SDK is not initialized.
 * App will NOT crash if REOWN_PROJECT_ID is missing or invalid.
 *
 * Requires REOWN_PROJECT_ID to be set in:
 * - local.properties: REOWN_PROJECT_ID=your_id
 * - or environment variable: REOWN_PROJECT_ID
 *
 * Get project ID from: https://cloud.reown.com
 */
object ReownManager {
    private const val TAG = "ReownManager"

    private const val RELAY_URL = "wss://relay.walletconnect.com"
    private const val APP_NAME = "WinoPay"
    private const val APP_DESCRIPTION = "Accept crypto payments"
    private const val APP_URL = "https://winopay.com"
    private const val APP_REDIRECT = "winopay://wc"
    private const val INIT_TIMEOUT_MS = 10_000L
    private const val MAX_CRYPTO_ERRORS = 3  // Max crypto errors before forced reset

    // Icon URLs for WalletConnect UI
    private val APP_ICONS = listOf(
        "https://winopay.com/icon-192.png"
    )

    // Initialization state
    @Volatile
    private var _initState: InitState = InitState.NotStarted

    @Volatile
    private var _initError: String? = null

    @Volatile
    private var _application: Application? = null

    // Crypto health tracking
    @Volatile
    private var _cryptoErrorCount = 0

    @Volatile
    private var _lastCryptoError: String? = null

    // Thread-safety
    private val initMutex = Mutex()

    private sealed class InitState {
        data object NotStarted : InitState()
        data object Initializing : InitState()
        data object Ready : InitState()
        data class Failed(val reason: String) : InitState()
        data object Resetting : InitState()  // Health reset in progress
    }

    /**
     * CRITICAL: Register BouncyCastle provider BEFORE any crypto operations.
     *
     * Android has a stripped-down BouncyCastle in system classpath.
     * Reown SDK requires full BC (bcprov-jdk18on) for ChaCha20Poly1305.
     * We must ensure Reown's BC is used, not Android's.
     */
    private fun ensureBouncyCastleProvider() {
        try {
            val bcProvider = BouncyCastleProvider()
            val existingBc = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)

            if (existingBc != null) {
                // Remove existing (possibly Android's stripped BC)
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
                Log.i(TAG, "WC|CRYPTO|BC_REMOVED|existing=${existingBc.javaClass.name}|version=${existingBc.version}")
            }

            // Insert our BC at position 1 (highest priority)
            val position = Security.insertProviderAt(bcProvider, 1)
            Log.i(TAG, "WC|CRYPTO|BC_REGISTERED|position=$position|version=${bcProvider.version}|class=${bcProvider.javaClass.name}")

            // Verify ChaCha20Poly1305 is available
            val chacha = bcProvider.getService("Cipher", "ChaCha20-Poly1305")
            Log.i(TAG, "WC|CRYPTO|CHACHA_CHECK|available=${chacha != null}|algo=${chacha?.algorithm}")

        } catch (e: Exception) {
            Log.e(TAG, "WC|CRYPTO|BC_REGISTER_ERROR|${e.javaClass.simpleName}|${e.message}", e)
        }
    }

    /**
     * Start SDK initialization (non-blocking).
     *
     * Called early in Application.onCreate(). Does NOT wait for completion.
     * Safe to call multiple times - will skip if already started.
     *
     * @param application The application instance
     * @return true if initialization was started, false if skipped or failed
     */
    fun initialize(application: Application): Boolean {
        // Skip if already started/completed
        if (_initState != InitState.NotStarted && _initState != InitState.Resetting) {
            Log.d(TAG, "WC|INIT|SKIP|state=$_initState")
            return _initState == InitState.Ready
        }

        _application = application
        val projectId = BuildConfig.REOWN_PROJECT_ID

        if (projectId.isBlank()) {
            _initState = InitState.Failed("no_project_id")
            _initError = "REOWN_PROJECT_ID not set"
            Log.w(TAG, "WC|INIT|SKIP|reason=project_id_missing")
            Log.w(TAG, "  Set REOWN_PROJECT_ID in local.properties or environment")
            Log.w(TAG, "  Get project ID from https://cloud.reown.com")
            return false
        }

        Log.i(TAG, "WC|INIT|START|projectId=${projectId.take(8)}...|thread=${Thread.currentThread().name}")

        // CRITICAL: Register BouncyCastle provider BEFORE any crypto operations
        // This prevents "ChaCha20Poly1305 cannot be reused for encryption" errors
        ensureBouncyCastleProvider()

        _initState = InitState.Initializing
        _cryptoErrorCount = 0  // Reset error count on new init

        return try {
            val serverUrl = "$RELAY_URL?projectId=$projectId"

            val appMetaData = Core.Model.AppMetaData(
                name = APP_NAME,
                description = APP_DESCRIPTION,
                url = APP_URL,
                icons = APP_ICONS,
                redirect = APP_REDIRECT,
                appLink = null,
                linkMode = false
            )

            Log.i(TAG, "WC|CORE|INIT_START|relay=$RELAY_URL")

            CoreClient.initialize(
                relayServerUrl = serverUrl,
                connectionType = ConnectionType.AUTOMATIC,
                application = application,
                metaData = appMetaData,
                onError = { error ->
                    safeCallback("CoreClient.onError") {
                        val msg = error.throwable.message ?: "unknown"
                        Log.e(TAG, "WC|CORE|ERROR|${error.throwable.javaClass.simpleName}|$msg", error.throwable)
                        handleCoreError(error.throwable)
                    }
                }
            )

            Log.i(TAG, "WC|CORE|INITIALIZED")

            SignClient.initialize(
                init = Sign.Params.Init(core = CoreClient),
                onSuccess = {
                    safeCallback("SignClient.onSuccess") {
                        Log.i(TAG, "WC|SIGN|INITIALIZED")
                        Log.i(TAG, "WC|INIT|OK|thread=${Thread.currentThread().name}")
                        _initState = InitState.Ready
                        _cryptoErrorCount = 0
                    }
                },
                onError = { error ->
                    safeCallback("SignClient.onError") {
                        val msg = error.throwable.message ?: "unknown"
                        Log.e(TAG, "WC|SIGN|INIT_ERROR|${error.throwable.javaClass.simpleName}|$msg", error.throwable)
                        handleCoreError(error.throwable)
                    }
                }
            )

            // NOTE: We return true to indicate init was STARTED, not completed
            // Use isReady() or ensureInitialized() to check actual readiness
            Log.i(TAG, "WC|INIT|STARTED")
            true
        } catch (e: Exception) {
            Log.e(TAG, "WC|INIT|FAIL|${e.javaClass.simpleName}|${e.message}", e)
            handleCoreError(e)
            false
        }
    }

    /**
     * Handle errors from CoreClient/SignClient.
     * Tracks crypto errors and triggers health reset if needed.
     */
    private fun handleCoreError(throwable: Throwable) {
        val msg = throwable.message ?: ""
        val className = throwable.javaClass.simpleName

        // Check if this is a crypto error (ChaCha20Poly1305 reuse)
        val isCryptoError = msg.contains("ChaCha20Poly1305", ignoreCase = true) ||
                msg.contains("cannot be reused", ignoreCase = true) ||
                msg.contains("cipher", ignoreCase = true) ||
                className.contains("IllegalState")

        if (isCryptoError) {
            _cryptoErrorCount++
            _lastCryptoError = msg
            Log.e(TAG, "WC|CRYPTO|ERROR|count=$_cryptoErrorCount|max=$MAX_CRYPTO_ERRORS|msg=$msg")
            Log.e(TAG, "WC|CRYPTO|ERROR|thread=${Thread.currentThread().name}")

            if (_cryptoErrorCount >= MAX_CRYPTO_ERRORS) {
                Log.e(TAG, "WC|CRYPTO|MAX_ERRORS_REACHED|triggering_health_reset")
                // Don't auto-reset here, let the app handle it via needsHealthReset()
            }
        }

        _initError = msg
        _initState = InitState.Failed(if (isCryptoError) "crypto_error" else "core_error")
    }

    /**
     * Check if SDK is fully ready (initialization completed successfully).
     *
     * @return true only if SignClient is initialized and ready
     */
    fun isReady(): Boolean = _initState == InitState.Ready

    /**
     * Check if Reown is available (project ID configured and init didn't fail hard).
     *
     * Used for UI display purposes - shows if WC button should be enabled.
     */
    fun isAvailable(): Boolean {
        val hasProjectId = BuildConfig.REOWN_PROJECT_ID.isNotBlank()
        val notFailed = _initState !is InitState.Failed
        return hasProjectId && notFailed
    }

    /**
     * Ensure SDK is initialized and ready. Idempotent, safe, never throws.
     *
     * Call this before any SignClient operations.
     * Uses mutex to prevent concurrent initialization attempts.
     *
     * @return true if SDK is ready to use, false otherwise
     */
    suspend fun ensureInitialized(): Boolean {
        return try {
            initMutex.withLock {
                when (_initState) {
                    is InitState.Ready -> {
                        Log.d(TAG, "WC|ENSURE|already_ready")
                        true
                    }
                    is InitState.Failed -> {
                        val reason = (_initState as InitState.Failed).reason
                        Log.w(TAG, "WC|ENSURE|previously_failed|reason=$reason|error=$_initError")

                        // If it was a crypto error, suggest health reset
                        if (reason == "crypto_error") {
                            Log.w(TAG, "WC|ENSURE|crypto_error_detected|needsHealthReset=true")
                        }
                        false
                    }
                    is InitState.NotStarted -> {
                        Log.w(TAG, "WC|ENSURE|not_started")
                        // Try to initialize if we have application reference
                        _application?.let { initialize(it) }
                        waitForReady()
                    }
                    is InitState.Initializing -> {
                        Log.d(TAG, "WC|ENSURE|waiting_for_init")
                        waitForReady()
                    }
                    is InitState.Resetting -> {
                        Log.d(TAG, "WC|ENSURE|health_reset_in_progress")
                        waitForReady()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WC|ENSURE|error=${e.message}", e)
            false
        }
    }

    /**
     * Wait for initialization to complete with timeout.
     */
    private suspend fun waitForReady(): Boolean {
        return try {
            withTimeout(INIT_TIMEOUT_MS) {
                // Poll for ready state
                var attempts = 0
                while ((_initState == InitState.Initializing || _initState == InitState.Resetting) && attempts < 100) {
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
                val ready = _initState == InitState.Ready
                Log.d(TAG, "WC|WAIT_READY|result=$ready|state=$_initState|attempts=$attempts")
                ready
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "WC|ENSURE|timeout|state=$_initState")
            false
        }
    }

    /**
     * Get initialization error if any.
     */
    fun getInitError(): String? = _initError

    /**
     * Check if health reset is needed due to repeated crypto errors.
     */
    fun needsHealthReset(): Boolean {
        return _cryptoErrorCount >= MAX_CRYPTO_ERRORS ||
                _initState is InitState.Failed &&
                (_initState as InitState.Failed).reason == "crypto_error"
    }

    /**
     * Get last crypto error message.
     */
    fun getLastCryptoError(): String? = _lastCryptoError

    /**
     * Perform health reset after crypto errors.
     *
     * This clears all sessions/pairings and reinitializes the SDK.
     * Should be called when needsHealthReset() returns true.
     *
     * @return true if reset was successful and SDK is ready
     */
    suspend fun performHealthReset(): Boolean {
        Log.w(TAG, "WC|HEALTH_RESET|START|cryptoErrors=$_cryptoErrorCount|lastError=$_lastCryptoError")

        return initMutex.withLock {
            try {
                _initState = InitState.Resetting

                // Clear any existing sessions/pairings
                try {
                    if (isReady()) {
                        val sessions = SignClient.getListOfActiveSessions()
                        Log.i(TAG, "WC|HEALTH_RESET|CLEARING|sessions=${sessions.size}")
                        for (session in sessions) {
                            try {
                                SignClient.disconnect(
                                    disconnect = Sign.Params.Disconnect(sessionTopic = session.topic),
                                    onSuccess = { Log.i(TAG, "WC|HEALTH_RESET|SESSION_CLEARED|${session.topic}") },
                                    onError = { /* ignore */ }
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "WC|HEALTH_RESET|SESSION_CLEAR_ERROR|${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "WC|HEALTH_RESET|CLEAR_ERROR|${e.message}")
                }

                // Reset state
                _initState = InitState.NotStarted
                _initError = null
                _cryptoErrorCount = 0
                _lastCryptoError = null

                // Re-register BC provider (critical!)
                ensureBouncyCastleProvider()

                // Wait a bit for cleanup
                kotlinx.coroutines.delay(500)

                // Reinitialize
                val app = _application
                if (app == null) {
                    Log.e(TAG, "WC|HEALTH_RESET|FAIL|no_application_reference")
                    return@withLock false
                }

                Log.i(TAG, "WC|HEALTH_RESET|REINIT")
                val started = initialize(app)
                if (!started) {
                    Log.e(TAG, "WC|HEALTH_RESET|REINIT_FAILED")
                    return@withLock false
                }

                // Wait for ready
                val ready = ensureInitialized()
                Log.i(TAG, "WC|HEALTH_RESET|DONE|ready=$ready")
                ready

            } catch (e: Exception) {
                Log.e(TAG, "WC|HEALTH_RESET|EXCEPTION|${e.javaClass.simpleName}|${e.message}", e)
                _initState = InitState.Failed("health_reset_failed")
                _initError = e.message
                false
            }
        }
    }

    /**
     * Report a crypto error from external code (e.g., UnifiedWalletConnector).
     * Call this when you catch a ChaCha20Poly1305 or similar crypto exception.
     */
    fun reportCryptoError(error: String) {
        _cryptoErrorCount++
        _lastCryptoError = error
        Log.e(TAG, "WC|CRYPTO|REPORTED|count=$_cryptoErrorCount|error=$error")
    }

    /**
     * Get current pairings count. Safe - returns 0 if not initialized.
     */
    fun getPairingsCount(): Int {
        if (_initState != InitState.Ready) return 0
        return try {
            CoreClient.Pairing.getPairings().size
        } catch (e: Exception) {
            Log.w(TAG, "WC|GET_PAIRINGS|ERROR|${e.message}")
            0
        }
    }

    /**
     * Get active sessions count. Safe - returns 0 if not initialized.
     */
    fun getSessionsCount(): Int {
        if (_initState != InitState.Ready) return 0
        return try {
            SignClient.getListOfActiveSessions().size
        } catch (e: Exception) {
            Log.w(TAG, "WC|GET_SESSIONS|ERROR|${e.message}")
            0
        }
    }

    /**
     * Disconnect all sessions. Safe - no-op if not initialized.
     */
    suspend fun disconnectAll() {
        if (_initState != InitState.Ready) {
            Log.w(TAG, "WC|DISCONNECT_ALL|SKIP|not_ready")
            return
        }
        try {
            val sessions = SignClient.getListOfActiveSessions()
            for (session in sessions) {
                Log.i(TAG, "WC|DISCONNECT|session=${session.topic}")
                SignClient.disconnect(
                    disconnect = Sign.Params.Disconnect(sessionTopic = session.topic),
                    onSuccess = { Log.i(TAG, "WC|DISCONNECT|SUCCESS|${session.topic}") },
                    onError = { error -> Log.e(TAG, "WC|DISCONNECT|ERROR|${error.throwable.message}") }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "WC|DISCONNECT_ALL|ERROR|${e.message}", e)
        }
    }

    /**
     * Safe callback wrapper - prevents crashes from uncaught exceptions in SDK callbacks.
     *
     * CRITICAL: SDK callbacks can throw exceptions on background threads, causing red crash screens.
     * This wrapper catches all exceptions, logs full stacktrace, and prevents app crashes.
     */
    private inline fun safeCallback(callbackName: String, crossinline block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Log full stack for debugging - this goes to logcat, not user UI
            Log.e(TAG, "WC|CALLBACK|CRASH_PREVENTED|name=$callbackName|class=${e.javaClass.simpleName}|msg=${e.message}", e)

            // Check if crypto error and report it
            val msg = e.message ?: ""
            val isCryptoError = msg.contains("ChaCha20Poly1305", ignoreCase = true) ||
                    msg.contains("cannot be reused", ignoreCase = true) ||
                    msg.contains("cipher", ignoreCase = true)

            if (isCryptoError) {
                Log.e(TAG, "WC|CALLBACK|CRYPTO_ERROR_DETECTED|count_will_increase")
                reportCryptoError(msg)
            }

            // Mark as failed so app can show error UI instead of crashing
            _initError = e.message
            _initState = InitState.Failed(if (isCryptoError) "crypto_error" else "callback_error")
        }
    }
}
