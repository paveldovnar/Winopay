package com.winopay.solana.wallet

import android.util.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.winopay.data.local.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Session state for the Solana wallet.
 */
sealed class SessionState {
    data object Initializing : SessionState()
    data object NoSession : SessionState()
    data object Connecting : SessionState()
    data class Active(
        val publicKey: String,
        val walletName: String?
    ) : SessionState()
    data class SessionError(val message: String) : SessionState()
}

/**
 * Manages Solana wallet sessions with persistence and auto-reconnect.
 *
 * Responsibilities:
 * - Persist wallet address and auth token to DataStore
 * - Auto-restore session on app launch
 * - Handle session lifecycle
 * - Coordinate between WalletConnector and DataStore
 */
class SolanaSessionManager(
    private val walletConnector: WalletConnector,
    private val dataStoreManager: DataStoreManager
) {

    companion object {
        private const val TAG = "SolanaSessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initializing)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var isInitialized = false

    /**
     * Initialize the session manager.
     * Attempts to restore session from persisted data.
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        scope.launch {
            Log.d(TAG, "Initializing session manager...")
            isInitialized = true

            try {
                // Try to restore from DataStore
                val walletData = dataStoreManager.getWalletSession()

                if (walletData != null) {
                    Log.d(TAG, "Found persisted wallet session: ${walletData.publicKey}")

                    // Restore to WalletConnector
                    walletConnector.restoreFromCache(
                        publicKey = walletData.publicKey,
                        authToken = walletData.authToken,
                        walletName = walletData.walletName
                    )

                    _sessionState.value = SessionState.Active(
                        publicKey = walletData.publicKey,
                        walletName = walletData.walletName
                    )

                    Log.d(TAG, "Session restored successfully")
                } else {
                    Log.d(TAG, "No persisted session found")
                    _sessionState.value = SessionState.NoSession
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize session: ${e.message}", e)
                _sessionState.value = SessionState.NoSession
            }
        }
    }

    /**
     * Connect to a wallet and persist the session.
     *
     * @param sender ActivityResultSender from the current activity
     * @return Result of the connection attempt
     */
    suspend fun connectWallet(sender: ActivityResultSender): WalletResult<SessionState.Active> {
        Log.d(TAG, "━━━━━ CONNECT WALLET START ━━━━━")
        _sessionState.value = SessionState.Connecting

        // Get existing auth token for reauthorization
        val existingSession = dataStoreManager.getWalletSession()
        val authToken = existingSession?.authToken

        // Attempt connection
        val result = walletConnector.connect(sender, authToken)

        return when (result) {
            is WalletResult.Success -> {
                val connected = result.data

                // Persist the session
                dataStoreManager.saveWalletSession(
                    publicKey = connected.publicKey,
                    authToken = connected.authToken ?: "",
                    walletName = connected.walletName
                )

                val activeState = SessionState.Active(
                    publicKey = connected.publicKey,
                    walletName = connected.walletName
                )
                _sessionState.value = activeState

                Log.d(TAG, "Wallet connected and session persisted: ${connected.publicKey}")
                WalletResult.Success(activeState)
            }
            is WalletResult.Failure -> {
                Log.e(TAG, "Wallet connection failed: ${result.error}")
                _sessionState.value = SessionState.SessionError(result.error)
                WalletResult.Failure(result.error, result.cause)
            }
        }
    }

    /**
     * Verify the current session is still valid by attempting reauthorization.
     *
     * @param sender ActivityResultSender from the current activity
     * @return true if session is valid or was successfully reauthorized
     */
    suspend fun verifySession(sender: ActivityResultSender): Boolean {
        val currentState = _sessionState.value

        if (currentState !is SessionState.Active) {
            Log.d(TAG, "No active session to verify")
            return false
        }

        Log.d(TAG, "Verifying session...")

        val existingSession = dataStoreManager.getWalletSession()
        if (existingSession?.authToken == null) {
            Log.d(TAG, "No auth token to verify")
            return false
        }

        // Attempt reauthorization
        val result = walletConnector.connect(sender, existingSession.authToken)

        return when (result) {
            is WalletResult.Success -> {
                // Update persisted session with new auth token
                dataStoreManager.saveWalletSession(
                    publicKey = result.data.publicKey,
                    authToken = result.data.authToken ?: "",
                    walletName = result.data.walletName
                )
                Log.d(TAG, "Session verified and updated")
                true
            }
            is WalletResult.Failure -> {
                Log.w(TAG, "Session verification failed: ${result.error}")
                false
            }
        }
    }

    /**
     * Disconnect the wallet and clear persisted session.
     */
    suspend fun disconnectWallet() {
        Log.d(TAG, "Disconnecting wallet...")

        walletConnector.disconnect()
        dataStoreManager.clearWalletSession()

        _sessionState.value = SessionState.NoSession
        Log.d(TAG, "Wallet disconnected and session cleared")
    }

    /**
     * Get the current wallet public key if connected.
     */
    fun getPublicKey(): String? {
        return (sessionState.value as? SessionState.Active)?.publicKey
    }

    /**
     * Check if there's an active session.
     */
    fun hasActiveSession(): Boolean {
        return sessionState.value is SessionState.Active
    }

    /**
     * Get the auth token for the current session.
     */
    suspend fun getAuthToken(): String? {
        return dataStoreManager.getWalletSession()?.authToken
    }
}
