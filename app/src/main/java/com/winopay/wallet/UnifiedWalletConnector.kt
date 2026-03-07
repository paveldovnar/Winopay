package com.winopay.wallet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import com.winopay.BuildConfig
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.profile.RailConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Unified wallet connector using Reown (WalletConnect v2) SDK.
 *
 * MULTI-CHAIN SUPPORT:
 * Extracts Solana + TRON + EVM addresses from a single WalletConnect session
 * and saves them ALL to MerchantProfileStore.connectedRails.
 *
 * NON-DESTRUCTIVE:
 * Connecting via WalletConnect does NOT remove existing rails.
 * Each rail is updated independently.
 *
 * SESSION PERSISTENCE:
 * Session topic is stored in each RailConnection.sessionId for future signing.
 */
class UnifiedWalletConnector(
    private val context: Context,
    private val profileStore: MerchantProfileStore
) {
    companion object {
        private const val TAG = "UnifiedWalletConnector"
        private const val CONNECTION_TIMEOUT_MS = 90_000L // 90 seconds
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Pending connection state - track for cleanup
    @Volatile
    private var pendingConnection: CompletableDeferred<ConnectResult>? = null
    @Volatile
    private var pendingRequestedRails: List<RailRequest>? = null
    @Volatile
    private var pendingPairingTopic: String? = null
    @Volatile
    private var pendingProposalId: Long? = null
    @Volatile
    private var currentSessionTopic: String? = null

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // State management
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Result types
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    sealed class ConnectResult {
        data class Success(
            val sessionId: String,
            val connectedRails: Map<String, RailConnection>,
            val walletName: String
        ) : ConnectResult()

        data class PartialSuccess(
            val sessionId: String,
            val connectedRails: Map<String, RailConnection>,
            val failedRails: List<String>,
            val walletName: String
        ) : ConnectResult()

        data class Failure(val error: String) : ConnectResult()

        /**
         * Crypto error (ChaCha20Poly1305 reuse, etc.)
         * UI should offer "Retry" which triggers health reset.
         */
        data class CryptoError(
            val error: String,
            val needsHealthReset: Boolean = true
        ) : ConnectResult()
    }

    sealed class SessionState {
        data object Disconnected : SessionState()
        data object Connecting : SessionState()
        data class WaitingForApproval(val uri: String) : SessionState()
        data class Connected(
            val sessionId: String,
            val connectedRails: Map<String, RailConnection>,
            val walletName: String
        ) : SessionState()
        data class Error(val message: String) : SessionState()
    }

    data class RailRequest(
        val railId: String,    // "solana", "tron", "evm"
        val networkId: String  // "devnet", "tron-nile", "56"
    )

    /**
     * Wallet presets for multichain connect.
     *
     * Strategy:
     * - requiredNamespaces = what MUST be supported (usually eip155)
     * - optionalNamespaces = what we'd like (solana, tron) but won't fail without
     *
     * This ensures EVM always connects, and Solana/TRON connect where supported.
     */
    enum class WalletPreset(
        val displayName: String,
        val requiredNamespaces: List<String>,   // MUST be approved, or connect fails
        val optionalNamespaces: List<String>    // Nice to have, wallet can ignore
    ) {
        /** MetaMask, Rainbow, Coinbase Wallet - EVM only */
        EVM_ONLY(
            displayName = "EVM Wallet",
            requiredNamespaces = listOf(CaipUtils.NAMESPACE_EIP155),
            optionalNamespaces = emptyList()
        ),

        /** OKX Wallet - EVM required, Solana/TRON optional */
        OKX(
            displayName = "OKX Wallet",
            requiredNamespaces = listOf(CaipUtils.NAMESPACE_EIP155),
            optionalNamespaces = listOf(CaipUtils.NAMESPACE_SOLANA, CaipUtils.NAMESPACE_TRON)
        ),

        /** Trust Wallet - EVM required, Solana optional */
        TRUST(
            displayName = "Trust Wallet",
            requiredNamespaces = listOf(CaipUtils.NAMESPACE_EIP155),
            optionalNamespaces = listOf(CaipUtils.NAMESPACE_SOLANA)
        ),

        /** SafePal - EVM required, Solana optional */
        SAFEPAL(
            displayName = "SafePal",
            requiredNamespaces = listOf(CaipUtils.NAMESPACE_EIP155),
            optionalNamespaces = listOf(CaipUtils.NAMESPACE_SOLANA)
        ),

        /** Default: EVM required, try Solana/TRON as optional */
        DEFAULT(
            displayName = "Other Wallet",
            requiredNamespaces = listOf(CaipUtils.NAMESPACE_EIP155),
            optionalNamespaces = listOf(CaipUtils.NAMESPACE_SOLANA, CaipUtils.NAMESPACE_TRON)
        );

        /** All namespaces this preset supports (required + optional) */
        val supportedNamespaces: List<String>
            get() = requiredNamespaces + optionalNamespaces
    }

    /**
     * What namespaces to request in staged connect.
     */
    enum class ConnectStage {
        /** Step 1: EVM only (BSC/Ethereum) */
        EVM,
        /** Step 2: Add Solana to existing session */
        SOLANA,
        /** Step 3: Add TRON to existing session */
        TRON,
        /** Request all supported namespaces at once */
        ALL
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Initialization
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    // NO init block - NO SignClient calls in constructor!
    // Delegate is set lazily in ensureDelegateSet() only after SDK is ready.

    @Volatile
    private var _delegateSet = false

    /**
     * Set up DappDelegate ONLY if SDK is ready.
     * Safe to call multiple times - idempotent.
     */
    private fun ensureDelegateSet(): Boolean {
        if (_delegateSet) return true

        if (!ReownManager.isReady()) {
            Log.w(TAG, "WC|DELEGATE|SKIP|sdk_not_ready")
            return false
        }

        return try {
            val delegate = object : SignClient.DappDelegate {
                override fun onSessionApproved(approvedSession: Sign.Model.ApprovedSession) = safeCallback("onSessionApproved") {
                    Log.i(TAG, "WC|SESSION|APPROVED|topic=${approvedSession.topic}|wallet=${approvedSession.metaData?.name}")
                    Log.i(TAG, "WC|SESSION|APPROVED|namespaces=${approvedSession.namespaces.keys}|accountCount=${approvedSession.namespaces.values.sumOf { it.accounts.size }}")
                    for ((ns, sessionNs) in approvedSession.namespaces) {
                        Log.i(TAG, "WC|NAMESPACE|APPROVED|$ns|accounts=${sessionNs.accounts}")
                    }
                    handleSessionApproved(approvedSession)
                }

                override fun onSessionRejected(rejectedSession: Sign.Model.RejectedSession) = safeCallback("onSessionRejected") {
                    Log.w(TAG, "WC|SESSION|REJECTED|reason=${rejectedSession.reason}")
                    handleSessionRejected(rejectedSession.reason)
                }

                override fun onSessionUpdate(updatedSession: Sign.Model.UpdatedSession) = safeCallback("onSessionUpdate") {
                    Log.i(TAG, "WC|SESSION|UPDATE|topic=${updatedSession.topic}")
                }

                override fun onSessionEvent(sessionEvent: Sign.Model.SessionEvent) = safeCallback("onSessionEvent") {
                    Log.i(TAG, "WC|SESSION|EVENT|name=${sessionEvent.name}|data=${sessionEvent.data}")
                }

                override fun onSessionExtend(session: Sign.Model.Session) = safeCallback("onSessionExtend") {
                    Log.i(TAG, "WC|SESSION|EXTEND|topic=${session.topic}")
                }

                override fun onSessionDelete(deletedSession: Sign.Model.DeletedSession) = safeCallback("onSessionDelete") {
                    val info = when (deletedSession) {
                        is Sign.Model.DeletedSession.Success -> "status=success|topic=${deletedSession.topic}"
                        is Sign.Model.DeletedSession.Error -> "status=error|error=${deletedSession.error}"
                    }
                    Log.i(TAG, "WC|SESSION|DELETE|$info")
                    _sessionState.value = SessionState.Disconnected
                }

                override fun onSessionRequestResponse(response: Sign.Model.SessionRequestResponse) = safeCallback("onSessionRequestResponse") {
                    Log.i(TAG, "WC|SESSION|RESPONSE|topic=${response.topic}|result=${response.result}")
                }

                override fun onConnectionStateChange(state: Sign.Model.ConnectionState) = safeCallback("onConnectionStateChange") {
                    Log.i(TAG, "WC|RELAY|STATE|isAvailable=${state.isAvailable}")
                }

                override fun onError(error: Sign.Model.Error) = safeCallback("onError") {
                    Log.e(TAG, "WC|SESSION|ERROR|class=${error.throwable.javaClass.simpleName}|msg=${error.throwable.message}", error.throwable)
                    handleError(error.throwable.message ?: "Unknown error")
                }

                override fun onProposalExpired(proposal: Sign.Model.ExpiredProposal) = safeCallback("onProposalExpired") {
                    Log.w(TAG, "WC|PROPOSAL|EXPIRED|pairingTopic=${proposal.pairingTopic}")
                    handleError("Connection proposal expired. Please try again.")
                }

                override fun onRequestExpired(request: Sign.Model.ExpiredRequest) = safeCallback("onRequestExpired") {
                    Log.w(TAG, "WC|REQUEST|EXPIRED|topic=${request.topic}")
                }
            }

            SignClient.setDappDelegate(delegate)
            _delegateSet = true
            Log.i(TAG, "WC|DELEGATE|SET|success=true")
            true
        } catch (e: Exception) {
            Log.e(TAG, "WC|DELEGATE|SET|success=false|class=${e.javaClass.simpleName}|msg=${e.message}")
            e.printStackTrace()
            false
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Connection state management
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Log current connection state in one line for debugging.
     */
    private fun logState(step: String) {
        Log.i(TAG, "WC|STATE|$step|pending=${pendingConnection != null}|pairingTopic=$pendingPairingTopic|proposalId=$pendingProposalId|sessionTopic=$currentSessionTopic")
    }

    /**
     * Cleanup pending connection state.
     * Call on timeout, error, or rejection.
     */
    private fun cleanupPendingConnection(reason: String) {
        Log.i(TAG, "WC|CLEANUP|START|reason=$reason")
        logState("before_cleanup")

        // Complete pending connection if any
        pendingConnection?.complete(ConnectResult.Failure(reason))
        pendingConnection = null

        // Clear tracking state
        val oldPairingTopic = pendingPairingTopic
        val oldProposalId = pendingProposalId

        pendingPairingTopic = null
        pendingProposalId = null
        pendingRequestedRails = null

        logState("after_cleanup")

        // Try to disconnect pairing if possible
        if (oldPairingTopic != null) {
            try {
                Log.i(TAG, "WC|CLEANUP|PAIRING_DISCONNECT|topic=$oldPairingTopic")
                CoreClient.Pairing.disconnect(
                    disconnect = Core.Params.Disconnect(oldPairingTopic)
                ) { error ->
                    Log.w(TAG, "WC|CLEANUP|PAIRING_DISCONNECT_ERROR|${error.throwable.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "WC|CLEANUP|PAIRING_DISCONNECT_ERROR|${e.message}")
            }
        }

        Log.i(TAG, "WC|CLEANUP|DONE|reason=$reason")
    }

    /**
     * Check if there's an active session and validate it.
     * Returns session topic if valid, null otherwise.
     */
    private suspend fun checkActiveSession(): String? {
        if (!ReownManager.isReady()) return null

        return try {
            val sessions = SignClient.getListOfActiveSessions()
            if (sessions.isEmpty()) {
                Log.i(TAG, "WC|ACTIVE_SESSION|NONE")
                return null
            }

            // Take the first session (usually there's only one)
            val session = sessions.first()
            Log.i(TAG, "WC|ACTIVE_SESSION|FOUND|topic=${session.topic}|wallet=${session.metaData?.name}|namespaces=${session.namespaces.keys}")

            // Verify session has accounts
            val hasAccounts = session.namespaces.values.any { it.accounts.isNotEmpty() }
            if (!hasAccounts) {
                Log.w(TAG, "WC|ACTIVE_SESSION|INVALID|no_accounts|disconnecting")
                disconnectSession(session.topic)
                return null
            }

            session.topic
        } catch (e: Exception) {
            Log.e(TAG, "WC|ACTIVE_SESSION|ERROR|${e.message}")
            null
        }
    }

    /**
     * Disconnect a specific session.
     */
    private suspend fun disconnectSession(topic: String) {
        try {
            Log.i(TAG, "WC|DISCONNECT_SESSION|topic=$topic")
            SignClient.disconnect(
                disconnect = Sign.Params.Disconnect(sessionTopic = topic),
                onSuccess = { Log.i(TAG, "WC|DISCONNECT_SESSION|SUCCESS|topic=$topic") },
                onError = { error -> Log.e(TAG, "WC|DISCONNECT_SESSION|ERROR|${error.throwable.message}") }
            )
            if (currentSessionTopic == topic) {
                currentSessionTopic = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "WC|DISCONNECT_SESSION|EXCEPTION|${e.message}")
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Public API
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Connect to wallet requesting specific rails.
     *
     * @param requestedRails List of rails to request (e.g., solana, tron, evm)
     * @param preset Wallet preset (determines required vs optional namespaces)
     * @return ConnectResult with connected rails or error
     */
    suspend fun connect(
        requestedRails: List<RailRequest>,
        preset: WalletPreset = WalletPreset.DEFAULT
    ): ConnectResult {
        val projectIdPresent = BuildConfig.REOWN_PROJECT_ID.isNotBlank()
        val hasInternet = context.checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED

        Log.i(TAG, "WC|CONNECT|START|rails=${requestedRails.map { it.railId }}|projectIdPresent=$projectIdPresent|hasInternet=$hasInternet")
        logState("connect_start")

        // Check for existing active session first
        val existingSession = checkActiveSession()
        if (existingSession != null) {
            Log.w(TAG, "WC|CONNECT|EXISTING_SESSION|topic=$existingSession")
            Log.w(TAG, "WC|CONNECT|RECONNECT_MODE|disconnecting_old_session")
            disconnectSession(existingSession)
            delay(500) // Give SDK time to clean up
        }

        // CRITICAL: Ensure SDK is ready before any SignClient calls
        val sdkReady = ReownManager.ensureInitialized()
        Log.i(TAG, "WC|CONNECT|SDK_CHECK|ready=$sdkReady|error=${ReownManager.getInitError()}")
        logState("after_sdk_check")

        if (!sdkReady) {
            val error = ReownManager.getInitError() ?: "WalletConnect SDK not ready"
            Log.e(TAG, "WC|CONNECT|FAIL|reason=sdk_not_ready|error=$error")
            return ConnectResult.Failure("SDK not ready: $error")
        }

        // Set delegate (only once, after SDK is ready)
        val delegateOk = ensureDelegateSet()
        Log.i(TAG, "WC|CONNECT|DELEGATE_CHECK|set=$delegateOk")
        logState("after_delegate_check")

        if (!delegateOk) {
            val error = "Failed to set up WalletConnect delegate"
            Log.e(TAG, "WC|CONNECT|FAIL|reason=delegate_error")
            return ConnectResult.Failure(error)
        }

        _sessionState.value = SessionState.Connecting
        pendingRequestedRails = requestedRails
        pendingConnection = CompletableDeferred()
        logState("after_state_setup")

        return try {
            // Build required and optional namespaces based on preset
            val (requiredNamespaces, optionalNamespaces) = buildNamespacesWithPreset(requestedRails, preset)

            // Fail-fast: if no required namespaces, abort connect
            if (requiredNamespaces.isEmpty()) {
                val error = "No valid required namespaces. At least EVM (eip155) is required."
                Log.e(TAG, "WC|CONNECT|ABORT|reason=no_required_namespaces")
                _sessionState.value = SessionState.Error(error)
                pendingConnection?.complete(ConnectResult.Failure(error))
                return ConnectResult.Failure(error)
            }

            // Log what we're requesting
            val reqKeys = requiredNamespaces.keys.toList()
            val optKeys = optionalNamespaces.keys.toList()
            Log.i(TAG, "WC|CONNECT|REQ|required=$reqKeys|optional=$optKeys|preset=${preset.name}")

            for ((ns, proposal) in requiredNamespaces) {
                Log.i(TAG, "WC|NS|REQUIRED|$ns|chains=${proposal.chains}|methods=${proposal.methods}")
            }
            for ((ns, proposal) in optionalNamespaces) {
                Log.i(TAG, "WC|NS|OPTIONAL|$ns|chains=${proposal.chains}|methods=${proposal.methods}")
            }

            // Create pairing
            Log.i(TAG, "WC|PAIRING|CREATE|starting...")
            logState("before_pairing_create")

            var pairingError: String? = null
            val pairing = CoreClient.Pairing.create { error ->
                pairingError = error.throwable.message
                Log.e(TAG, "WC|PAIRING|ERROR|class=${error.throwable.javaClass.simpleName}|msg=${error.throwable.message}")
                error.throwable.printStackTrace()
                handleError(error.throwable.message ?: "Pairing failed")
            }

            if (pairing == null) {
                val error = pairingError ?: "Failed to create pairing (null returned)"
                Log.e(TAG, "WC|PAIRING|FAIL|error=$error")
                _sessionState.value = SessionState.Error(error)
                cleanupPendingConnection("pairing_creation_failed")
                return ConnectResult.Failure(error)
            }

            // Track pairing for cleanup
            pendingPairingTopic = pairing.topic

            Log.i(TAG, "WC|PAIRING|CREATED|topic=${pairing.topic}|uriLen=${pairing.uri.length}")
            Log.d(TAG, "WC|PAIRING|URI|${pairing.uri}")
            logState("after_pairing_created")

            // Connect with session proposal
            Log.i(TAG, "WC|PROPOSAL|SENDING|topic=${pairing.topic}")

            // Strategy: EVM in required (must connect), Solana/TRON in optional (nice to have)
            // This ensures EVM always connects, and others connect where supported
            @Suppress("DEPRECATION")
            val connectParams = Sign.Params.Connect(
                namespaces = requiredNamespaces,      // MUST be approved
                optionalNamespaces = optionalNamespaces.ifEmpty { null },  // Nice to have
                properties = null,
                pairing = pairing
            )

            Log.i(TAG, "WC|CONNECT_PARAMS|BUILT|pairing=${pairing.topic}|required=${requiredNamespaces.keys}|optional=${optionalNamespaces.keys}")
            logState("before_proposal_send")

            var proposalSent = false
            var proposalError: String? = null

            @Suppress("DEPRECATION")
            SignClient.connect(
                connect = connectParams,
                onSuccess = {
                    proposalSent = true
                    Log.i(TAG, "WC|PROPOSAL|SENT|topic=${pairing.topic}|awaiting_approval")
                    logState("after_proposal_sent")
                    _sessionState.value = SessionState.WaitingForApproval(pairing.uri)
                    // Open wallet app
                    openWalletApp(pairing.uri)
                },
                onError = { error ->
                    proposalError = error.throwable.message
                    Log.e(TAG, "WC|PROPOSAL|ERROR|class=${error.throwable.javaClass.simpleName}|msg=${error.throwable.message}")
                    error.throwable.printStackTrace()
                    logState("after_proposal_error")
                    handleError(error.throwable.message ?: "Connection failed")
                }
            )

            Log.i(TAG, "WC|CONNECT|WAITING|timeout=${CONNECTION_TIMEOUT_MS}ms")
            logState("waiting_for_approval")

            // Wait for result with timeout
            val result = withTimeout(CONNECTION_TIMEOUT_MS) {
                pendingConnection?.await() ?: ConnectResult.Failure("Connection cancelled")
            }

            when (result) {
                is ConnectResult.Success -> {
                    Log.i(TAG, "WC|CONNECT|DONE|status=success|rails=${result.connectedRails.keys}|wallet=${result.walletName}")
                    logState("connect_success")
                }
                is ConnectResult.PartialSuccess -> {
                    Log.i(TAG, "WC|CONNECT|DONE|status=partial|rails=${result.connectedRails.keys}|failed=${result.failedRails}")
                    logState("connect_partial_success")
                }
                is ConnectResult.Failure -> {
                    Log.e(TAG, "WC|CONNECT|DONE|status=failure|error=${result.error}")
                    logState("connect_failure")
                }
                is ConnectResult.CryptoError -> {
                    Log.e(TAG, "WC|CONNECT|DONE|status=crypto_error|error=${result.error}")
                    logState("connect_crypto_error")
                }
            }

            result
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "WC|CONNECT|TIMEOUT|after=${CONNECTION_TIMEOUT_MS}ms")
            Log.e(TAG, "WC|CONNECT|TIMEOUT|state=${_sessionState.value}")
            logState("timeout_caught")

            // Cleanup: cancel pending connection and disconnect pairing
            cleanupPendingConnection("timeout_after_${CONNECTION_TIMEOUT_MS}ms")

            _sessionState.value = SessionState.Error("Connection timed out after ${CONNECTION_TIMEOUT_MS / 1000}s")
            ConnectResult.Failure("Connection timed out. Make sure wallet app is installed and responding.")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            val className = e.javaClass.simpleName
            Log.e(TAG, "WC|CONNECT|EXCEPTION|class=$className|msg=$msg")
            e.printStackTrace()
            logState("exception_caught")

            // Check if this is a crypto error (ChaCha20Poly1305 reuse)
            val isCryptoError = msg.contains("ChaCha20Poly1305", ignoreCase = true) ||
                    msg.contains("cannot be reused", ignoreCase = true) ||
                    msg.contains("cipher", ignoreCase = true) ||
                    className.contains("IllegalState")

            // Cleanup on unexpected exception
            cleanupPendingConnection("exception_$className")

            if (isCryptoError) {
                Log.e(TAG, "WC|CRYPTO|CONNECT_ERROR|$msg")
                ReownManager.reportCryptoError(msg)
                _sessionState.value = SessionState.Error("Crypto error - please retry")
                ConnectResult.CryptoError(
                    error = "Encryption error. Tap Retry to reconnect.",
                    needsHealthReset = true
                )
            } else {
                _sessionState.value = SessionState.Error(msg)
                ConnectResult.Failure("$className: $msg")
            }
        }
    }

    /**
     * Connect using a wallet preset.
     *
     * This is the RECOMMENDED way to connect - use preset instead of connectAll().
     * Presets are tailored to what each wallet actually supports.
     *
     * @param preset The wallet preset (EVM_ONLY, OKX, TRUST, etc.)
     * @param stage Which chains to request (EVM, SOLANA, TRON, or ALL)
     */
    suspend fun connectWithPreset(
        preset: WalletPreset = WalletPreset.DEFAULT,
        stage: ConnectStage = ConnectStage.EVM
    ): ConnectResult {
        Log.i(TAG, "WC|CONNECT_PRESET|START|preset=${preset.name}|stage=${stage.name}")

        val rails = buildRailsForStage(preset, stage)

        if (rails.isEmpty()) {
            Log.e(TAG, "WC|CONNECT_PRESET|FAIL|no_rails_for_stage|preset=${preset.name}|stage=${stage.name}")
            return ConnectResult.Failure("No supported chains for ${preset.displayName} at stage ${stage.name}")
        }

        Log.i(TAG, "WC|CONNECT_PRESET|RAILS|${rails.map { "${it.railId}:${it.networkId}" }}")

        return connect(rails)
    }

    /**
     * Build rails list for a given preset and stage.
     */
    private fun buildRailsForStage(preset: WalletPreset, stage: ConnectStage): List<RailRequest> {
        val rails = mutableListOf<RailRequest>()

        val namespacesToRequest = when (stage) {
            ConnectStage.EVM -> listOf(CaipUtils.NAMESPACE_EIP155)
            ConnectStage.SOLANA -> listOf(CaipUtils.NAMESPACE_SOLANA)
            ConnectStage.TRON -> listOf(CaipUtils.NAMESPACE_TRON)
            ConnectStage.ALL -> preset.supportedNamespaces
        }

        for (namespace in namespacesToRequest) {
            // Only request if preset supports this namespace
            if (namespace !in preset.supportedNamespaces) {
                Log.w(TAG, "WC|BUILD_RAILS|SKIP|namespace=$namespace|reason=not_in_preset|preset=${preset.name}")
                continue
            }

            when (namespace) {
                CaipUtils.NAMESPACE_EIP155 -> {
                    rails.add(RailRequest("evm", CaipUtils.getPrimaryEvmNetworkId()))
                }
                CaipUtils.NAMESPACE_SOLANA -> {
                    rails.add(RailRequest("solana", BuildConfig.SOLANA_CLUSTER))
                }
                CaipUtils.NAMESPACE_TRON -> {
                    rails.add(RailRequest("tron", BuildConfig.TRON_NETWORK))
                }
            }
        }

        return rails
    }

    /**
     * Add a new namespace to existing session (staged connect).
     *
     * WalletConnect v2 doesn't support true session extension, so this:
     * 1. Disconnects current session
     * 2. Creates new session with current + new namespaces
     *
     * @param stage Which namespace to add (SOLANA or TRON)
     * @return ConnectResult with updated rails
     */
    suspend fun addNamespace(stage: ConnectStage): ConnectResult {
        Log.i(TAG, "WC|ADD_NAMESPACE|START|stage=${stage.name}")

        // Get currently connected namespaces
        val currentNamespaces = getConnectedNamespaces()
        Log.i(TAG, "WC|ADD_NAMESPACE|CURRENT|namespaces=$currentNamespaces")

        // Map stage to namespace
        val newNamespace = when (stage) {
            ConnectStage.SOLANA -> CaipUtils.NAMESPACE_SOLANA
            ConnectStage.TRON -> CaipUtils.NAMESPACE_TRON
            ConnectStage.EVM -> CaipUtils.NAMESPACE_EIP155
            ConnectStage.ALL -> {
                Log.e(TAG, "WC|ADD_NAMESPACE|FAIL|invalid_stage=ALL")
                return ConnectResult.Failure("Use connectWithPreset() for ALL stage")
            }
        }

        // Check if already connected
        if (newNamespace in currentNamespaces) {
            Log.w(TAG, "WC|ADD_NAMESPACE|SKIP|already_connected=$newNamespace")
            return ConnectResult.Failure("$newNamespace is already connected")
        }

        // Build combined rails list
        val rails = mutableListOf<RailRequest>()

        // Add existing namespaces
        for (ns in currentNamespaces) {
            when (ns) {
                CaipUtils.NAMESPACE_EIP155 -> rails.add(RailRequest("evm", CaipUtils.getPrimaryEvmNetworkId()))
                CaipUtils.NAMESPACE_SOLANA -> rails.add(RailRequest("solana", BuildConfig.SOLANA_CLUSTER))
                CaipUtils.NAMESPACE_TRON -> rails.add(RailRequest("tron", BuildConfig.TRON_NETWORK))
            }
        }

        // Add new namespace
        when (newNamespace) {
            CaipUtils.NAMESPACE_EIP155 -> rails.add(RailRequest("evm", CaipUtils.getPrimaryEvmNetworkId()))
            CaipUtils.NAMESPACE_SOLANA -> rails.add(RailRequest("solana", BuildConfig.SOLANA_CLUSTER))
            CaipUtils.NAMESPACE_TRON -> rails.add(RailRequest("tron", BuildConfig.TRON_NETWORK))
        }

        Log.i(TAG, "WC|ADD_NAMESPACE|COMBINED_RAILS|${rails.map { it.railId }}")

        // Note: connect() will automatically disconnect existing session
        return connect(rails)
    }

    /**
     * Get namespaces from currently connected session.
     */
    fun getConnectedNamespaces(): Set<String> {
        if (!ReownManager.isReady()) return emptySet()

        return try {
            val sessions = SignClient.getListOfActiveSessions()
            if (sessions.isEmpty()) return emptySet()

            val session = sessions.first()
            session.namespaces.keys.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "WC|GET_NAMESPACES|ERROR|${e.message}")
            emptySet()
        }
    }

    /**
     * Retry connection after performing health reset.
     *
     * Call this after receiving ConnectResult.CryptoError.
     * Performs SDK health reset and then retries the connection.
     *
     * @param requestedRails Rails to connect (use same as original request)
     * @return ConnectResult from retry attempt
     */
    suspend fun retryWithHealthReset(requestedRails: List<RailRequest>): ConnectResult {
        Log.i(TAG, "WC|RETRY|START|rails=${requestedRails.map { it.railId }}")

        // Perform health reset
        val resetOk = ReownManager.performHealthReset()
        Log.i(TAG, "WC|RETRY|HEALTH_RESET|success=$resetOk")

        if (!resetOk) {
            Log.e(TAG, "WC|RETRY|FAIL|health_reset_failed")
            return ConnectResult.Failure("Could not reset connection. Please restart the app.")
        }

        // Retry connection
        Log.i(TAG, "WC|RETRY|RECONNECT")
        return connect(requestedRails)
    }

    /**
     * Retry EVM-only connection after health reset.
     */
    suspend fun retryEvmOnlyWithHealthReset(): ConnectResult {
        return retryWithHealthReset(listOf(RailRequest("evm", CaipUtils.getPrimaryEvmNetworkId())))
    }

    /**
     * Check which stages can still be added (not yet connected).
     */
    fun getAvailableStages(): List<ConnectStage> {
        val connected = getConnectedNamespaces()
        val available = mutableListOf<ConnectStage>()

        if (CaipUtils.NAMESPACE_EIP155 !in connected) {
            available.add(ConnectStage.EVM)
        }
        if (CaipUtils.NAMESPACE_SOLANA !in connected) {
            available.add(ConnectStage.SOLANA)
        }
        if (CaipUtils.NAMESPACE_TRON !in connected) {
            available.add(ConnectStage.TRON)
        }

        Log.i(TAG, "WC|AVAILABLE_STAGES|connected=$connected|available=${available.map { it.name }}")
        return available
    }

    /**
     * Check if a specific stage can be added.
     */
    fun canAddStage(stage: ConnectStage): Boolean {
        return stage in getAvailableStages()
    }

    /**
     * Connect requesting all supported rails for current build flavor.
     *
     * DEPRECATED: Use connectWithPreset() instead for better compatibility.
     * This requests all chains at once which many wallets don't support.
     */
    @Deprecated("Use connectWithPreset() for better wallet compatibility")
    suspend fun connectAll(): ConnectResult {
        val rails = mutableListOf<RailRequest>()

        // Solana
        rails.add(RailRequest("solana", BuildConfig.SOLANA_CLUSTER))

        // TRON
        rails.add(RailRequest("tron", BuildConfig.TRON_NETWORK))

        // EVM - add primary network (BSC)
        rails.add(RailRequest("evm", CaipUtils.getPrimaryEvmNetworkId()))

        return connect(rails)
    }

    /**
     * Connect with EVM only (safest, works with all wallets).
     */
    suspend fun connectEvmOnly(): ConnectResult {
        return connectWithPreset(WalletPreset.DEFAULT, ConnectStage.EVM)
    }

    /**
     * Disconnect a WalletConnect session.
     *
     * @param sessionId Session topic to disconnect
     * @return true if disconnected successfully
     */
    suspend fun disconnect(sessionId: String): Boolean {
        Log.i(TAG, "WC|DISCONNECT|session=$sessionId")

        if (!ReownManager.isReady()) {
            Log.w(TAG, "WC|DISCONNECT|SKIP|sdk_not_ready")
            return false
        }

        return try {
            val deferred = CompletableDeferred<Boolean>()

            SignClient.disconnect(
                disconnect = Sign.Params.Disconnect(sessionTopic = sessionId),
                onSuccess = {
                    Log.i(TAG, "WC|DISCONNECT|SUCCESS")
                    deferred.complete(true)
                },
                onError = { error ->
                    Log.e(TAG, "WC|DISCONNECT|ERROR|${error.throwable.message}")
                    deferred.complete(false)
                }
            )

            val result = deferred.await()

            // Clear session from rails
            if (result) {
                withContext(Dispatchers.IO) {
                    val connectedRails = profileStore.getConnectedRails()
                    for ((railId, connection) in connectedRails) {
                        if (connection.sessionId == sessionId) {
                            Log.i(TAG, "WC|CLEAR_SESSION|rail=$railId")
                            val updated = connection.copy(sessionId = null)
                            profileStore.connectRail(updated, setAsActive = false)
                        }
                    }
                }
                _sessionState.value = SessionState.Disconnected
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "WC|DISCONNECT|EXCEPTION|${e.message}", e)
            false
        }
    }

    /**
     * Observe session state changes.
     */
    fun observeSessionState(): Flow<SessionState> = sessionState

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Internal: Build namespaces for session proposal
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Build required and optional namespaces based on preset.
     *
     * Strategy:
     * - Required = what MUST be approved (usually EVM/eip155)
     * - Optional = what we'd like but won't fail without (Solana, TRON)
     *
     * This ensures EVM always connects, and Solana/TRON connect where supported.
     */
    private fun buildNamespacesWithPreset(
        requestedRails: List<RailRequest>,
        preset: WalletPreset
    ): Pair<Map<String, Sign.Model.Namespace.Proposal>, Map<String, Sign.Model.Namespace.Proposal>> {

        val requiredNamespaces = mutableMapOf<String, Sign.Model.Namespace.Proposal>()
        val optionalNamespaces = mutableMapOf<String, Sign.Model.Namespace.Proposal>()

        // Build all namespaces from requested rails
        val allNamespaces = buildNamespaces(requestedRails) ?: emptyMap()

        // Split into required vs optional based on preset
        for ((namespace, proposal) in allNamespaces) {
            when {
                namespace in preset.requiredNamespaces -> {
                    requiredNamespaces[namespace] = proposal
                    Log.i(TAG, "WC|BUILD_NS|REQUIRED|$namespace|chains=${proposal.chains}")
                }
                namespace in preset.optionalNamespaces -> {
                    optionalNamespaces[namespace] = proposal
                    Log.i(TAG, "WC|BUILD_NS|OPTIONAL|$namespace|chains=${proposal.chains}")
                }
                else -> {
                    // Not in preset - add as optional anyway (be permissive)
                    optionalNamespaces[namespace] = proposal
                    Log.i(TAG, "WC|BUILD_NS|OPTIONAL_DEFAULT|$namespace|chains=${proposal.chains}")
                }
            }
        }

        // Ensure EVM is always in required if not already
        if (requiredNamespaces.isEmpty() && CaipUtils.NAMESPACE_EIP155 in allNamespaces) {
            val evmProposal = allNamespaces[CaipUtils.NAMESPACE_EIP155]!!
            requiredNamespaces[CaipUtils.NAMESPACE_EIP155] = evmProposal
            optionalNamespaces.remove(CaipUtils.NAMESPACE_EIP155)
            Log.w(TAG, "WC|BUILD_NS|FORCE_EVM_REQUIRED|no_required_in_preset")
        }

        Log.i(TAG, "WC|BUILD_NS|SPLIT|required=${requiredNamespaces.keys}|optional=${optionalNamespaces.keys}")

        return Pair(requiredNamespaces, optionalNamespaces)
    }

    /**
     * Build WalletConnect namespaces from requested rails.
     * Returns null if no valid chains could be built.
     */
    private fun buildNamespaces(requestedRails: List<RailRequest>): Map<String, Sign.Model.Namespace.Proposal>? {
        val namespaces = mutableMapOf<String, Sign.Model.Namespace.Proposal>()

        // Group chains by namespace
        val chainsByNamespace = mutableMapOf<String, MutableList<String>>()
        val invalidChains = mutableListOf<String>()

        Log.i(TAG, "WC|BUILD_NS|START|railCount=${requestedRails.size}")

        // Log all known chain IDs for debugging
        Log.i(TAG, "WC|BUILD_NS|KNOWN_CHAINS|${CaipUtils.getAllKnownChainIds()}")

        for (rail in requestedRails) {
            Log.i(TAG, "WC|BUILD_NS|RAIL|railId=${rail.railId}|networkId=${rail.networkId}")

            // Use single source of truth function
            val chainId = CaipUtils.chainIdForRail(rail.railId, rail.networkId)
            if (chainId == null) {
                val msg = "toChainId_returned_null"
                Log.e(TAG, "WC|BUILD_NS|INVALID|railId=${rail.railId}|networkId=${rail.networkId}|reason=$msg")
                invalidChains.add("${rail.railId}:${rail.networkId} -> $msg")
                continue
            }

            // Strict CAIP-2 validation
            val validation = CaipUtils.validateChainId(chainId)
            if (!validation.isValid) {
                Log.e(TAG, "WC|BUILD_NS|INVALID|chainId=$chainId|error=${validation.error}")
                invalidChains.add("$chainId -> ${validation.error}")
                continue
            }

            // Double-check reference length (CAIP-2 requires ≤32 chars for reference)
            val reference = chainId.substringAfter(":")
            if (reference.length > 32) {
                val msg = "reference_too_long (${reference.length} > 32)"
                Log.e(TAG, "WC|BUILD_NS|INVALID|chainId=$chainId|reason=$msg")
                invalidChains.add("$chainId -> $msg")
                continue
            }

            Log.i(TAG, "WC|BUILD_NS|VALID|chainId=$chainId|refLen=${reference.length}")

            val namespace = when (rail.railId) {
                "solana" -> CaipUtils.NAMESPACE_SOLANA
                "tron" -> CaipUtils.NAMESPACE_TRON
                "evm" -> CaipUtils.NAMESPACE_EIP155
                else -> {
                    Log.e(TAG, "WC|BUILD_NS|INVALID|railId=${rail.railId}|reason=unknown_rail")
                    invalidChains.add("${rail.railId} -> unknown_rail")
                    continue
                }
            }

            chainsByNamespace.getOrPut(namespace) { mutableListOf() }.add(chainId)
        }

        // Log summary of invalid chains
        if (invalidChains.isNotEmpty()) {
            Log.e(TAG, "WC|BUILD_NS|INVALID_SUMMARY|count=${invalidChains.size}|chains=$invalidChains")
        }

        // Fail-fast: if no valid chains, return null
        if (chainsByNamespace.isEmpty()) {
            Log.e(TAG, "WC|BUILD_NS|FAIL|reason=no_valid_chains|invalidCount=${invalidChains.size}")
            return null
        }

        Log.i(TAG, "WC|BUILD_NS|GROUPED|namespaceCount=${chainsByNamespace.size}")

        // Build namespace proposals
        for ((namespace, chains) in chainsByNamespace) {
            val methods = getMethodsForNamespace(namespace)
            val events = getEventsForNamespace(namespace)

            Log.i(TAG, "WC|BUILD_NS|PROPOSAL|namespace=$namespace")
            Log.i(TAG, "WC|BUILD_NS|PROPOSAL|chains=$chains")
            Log.i(TAG, "WC|BUILD_NS|PROPOSAL|methods=$methods")
            Log.i(TAG, "WC|BUILD_NS|PROPOSAL|events=$events")

            // Final validation: log each chain being added
            for (chain in chains) {
                Log.i(TAG, "WC|BUILD_NS|CHAIN_ADDED|namespace=$namespace|chain=$chain")
            }

            namespaces[namespace] = Sign.Model.Namespace.Proposal(
                chains = chains,
                methods = methods,
                events = events
            )
        }

        Log.i(TAG, "WC|BUILD_NS|DONE|totalNamespaces=${namespaces.size}|keys=${namespaces.keys}")
        return namespaces
    }

    private fun getMethodsForNamespace(namespace: String): List<String> {
        return when (namespace) {
            CaipUtils.NAMESPACE_SOLANA -> listOf(
                "solana_signTransaction",
                "solana_signMessage",
                "solana_signAllTransactions"
            )
            CaipUtils.NAMESPACE_TRON -> listOf(
                "tron_signTransaction",
                "tron_signMessage"
            )
            CaipUtils.NAMESPACE_EIP155 -> listOf(
                "eth_sendTransaction",
                "eth_signTransaction",
                "personal_sign",
                "eth_signTypedData",
                "eth_signTypedData_v4"
            )
            else -> emptyList()
        }
    }

    private fun getEventsForNamespace(namespace: String): List<String> {
        return when (namespace) {
            CaipUtils.NAMESPACE_EIP155 -> listOf("accountsChanged", "chainChanged")
            else -> emptyList()
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Internal: Handle delegate callbacks
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun handleSessionApproved(session: Sign.Model.ApprovedSession) {
        scope.launch {
            try {
                val sessionId = session.topic
                val walletName = session.metaData?.name ?: "Unknown Wallet"

                // Log approved namespaces immediately
                val approvedNamespaces = session.namespaces.keys.toList()
                Log.i(TAG, "WC|CONNECT|APPROVED|namespaces=$approvedNamespaces|wallet=$walletName|topic=$sessionId")
                logState("session_approved")

                // Save current session topic
                currentSessionTopic = sessionId

                // Clear pending pairing (approved, no need to clean it up)
                pendingPairingTopic = null
                pendingProposalId = null

                // Extract all accounts from approved namespaces
                val allAccounts = mutableListOf<String>()
                for ((namespace, sessionNamespace) in session.namespaces) {
                    Log.i(TAG, "WC|APPROVED_NS|$namespace|accounts=${sessionNamespace.accounts.size}|chains=${sessionNamespace.chains}")
                    for (account in sessionNamespace.accounts) {
                        Log.d(TAG, "WC|ACCOUNT|RAW|$account")
                    }
                    allAccounts.addAll(sessionNamespace.accounts)
                }

                Log.i(TAG, "WC|ACCOUNTS|TOTAL|count=${allAccounts.size}")

                // Parse accounts to rails
                val connectedRails = parseAccountsToRails(allAccounts, sessionId)

                Log.i(TAG, "WC|RAILS|PARSED|count=${connectedRails.size}|rails=${connectedRails.keys}")

                // Log all connected accounts
                for ((railId, connection) in connectedRails) {
                    Log.i(TAG, "WC|RAIL|$railId|network=${connection.networkId}|address=${connection.accountId.take(16)}...")
                }

                // Save rails to profile store
                withContext(Dispatchers.IO) {
                    for ((_, connection) in connectedRails) {
                        profileStore.connectRail(connection, setAsActive = false)
                        Log.i(TAG, "WC|SAVE|OK|rail=${connection.railId}|network=${connection.networkId}")
                    }
                }

                // Determine result based on requested vs connected
                val requestedRailIds = pendingRequestedRails?.map { it.railId }?.toSet() ?: emptySet()
                val connectedRailIds = connectedRails.keys
                val failedRails = requestedRailIds.minus(connectedRailIds).toList()

                Log.i(TAG, "WC|RESULT|requested=$requestedRailIds|connected=$connectedRailIds|failed=$failedRails")

                val result = if (failedRails.isEmpty()) {
                    ConnectResult.Success(
                        sessionId = sessionId,
                        connectedRails = connectedRails,
                        walletName = walletName
                    )
                } else {
                    ConnectResult.PartialSuccess(
                        sessionId = sessionId,
                        connectedRails = connectedRails,
                        failedRails = failedRails,
                        walletName = walletName
                    )
                }

                _sessionState.value = SessionState.Connected(
                    sessionId = sessionId,
                    connectedRails = connectedRails,
                    walletName = walletName
                )

                Log.i(TAG, "WC|CONNECT|COMPLETE|status=${if (failedRails.isEmpty()) "success" else "partial"}|wallet=$walletName")
                pendingConnection?.complete(result)

            } catch (e: Exception) {
                Log.e(TAG, "WC|HANDLE|ERROR|class=${e.javaClass.simpleName}|msg=${e.message}")
                e.printStackTrace()
                handleError(e.message ?: "Failed to process session")
            }
        }
    }

    private fun handleSessionRejected(reason: String) {
        Log.w(TAG, "WC|SESSION_REJECTED|$reason")
        logState("session_rejected")

        // Special handling for "chains not supported" - dump what we requested
        if (reason.contains("chain", ignoreCase = true) ||
            reason.contains("not supported", ignoreCase = true) ||
            reason.contains("unsupported", ignoreCase = true)) {
            Log.e(TAG, "WC|ERROR|CHAINS_NOT_SUPPORTED|reason=$reason")
            dumpRequestedNamespaces("rejection")
        }

        // Cleanup on rejection
        cleanupPendingConnection("wallet_rejected_$reason")

        _sessionState.value = SessionState.Error("Wallet rejected: $reason")
    }

    private fun handleError(message: String) {
        Log.e(TAG, "WC|ERROR|$message")
        logState("error_handler")

        // Special handling for "chains not supported" - dump what we requested
        if (message.contains("chain", ignoreCase = true) ||
            message.contains("not supported", ignoreCase = true) ||
            message.contains("unsupported", ignoreCase = true)) {
            Log.e(TAG, "WC|ERROR|CHAINS_NOT_SUPPORTED|msg=$message")
            dumpRequestedNamespaces("error")
        }

        // Cleanup on error
        cleanupPendingConnection("error_$message")

        _sessionState.value = SessionState.Error(message)
    }

    /**
     * Dump requested namespaces for debugging "chains not supported" errors.
     * Shows exactly what we asked for so we can see what wallet rejected.
     */
    private fun dumpRequestedNamespaces(context: String) {
        val requestedRails = pendingRequestedRails ?: return

        Log.e(TAG, "WC|DUMP|$context|START|requestedRails=${requestedRails.map { it.railId }}")

        val namespaces = buildNamespaces(requestedRails) ?: emptyMap()

        for ((ns, proposal) in namespaces) {
            Log.e(TAG, "WC|DUMP|$context|NS=$ns|chains=${proposal.chains}|methods=${proposal.methods}|events=${proposal.events}")
            proposal.chains?.forEach { chain ->
                Log.e(TAG, "WC|DUMP|$context|CHAIN=$chain|refLen=${chain.substringAfter(":").length}")
            }
        }

        Log.e(TAG, "WC|DUMP|$context|END")
    }

    /**
     * Safe callback wrapper - prevents crashes from uncaught exceptions in Reown SDK callbacks.
     *
     * CRITICAL: Reown SDK callbacks run on background threads. If an exception is thrown,
     * Android shows a red full-screen crash dialog. This wrapper:
     * 1. Catches ALL exceptions
     * 2. Logs full stacktrace to logcat (for debugging)
     * 3. Updates UI state to show error message (no crash)
     * 4. Cleans up pending connection state
     */
    private inline fun safeCallback(callbackName: String, crossinline block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // Log full stack for debugging
            Log.e(TAG, "WC|CALLBACK|CRASH_PREVENTED|name=$callbackName|class=${e.javaClass.simpleName}|msg=${e.message}", e)

            // Check if crypto error
            val msg = e.message ?: ""
            val isCryptoError = msg.contains("ChaCha20Poly1305", ignoreCase = true) ||
                    msg.contains("cannot be reused", ignoreCase = true) ||
                    msg.contains("cipher", ignoreCase = true)

            if (isCryptoError) {
                Log.e(TAG, "WC|CALLBACK|CRYPTO_ERROR_DETECTED|reporting_to_manager")
                ReownManager.reportCryptoError(msg)
            }

            // Update UI with user-friendly error
            val userMessage = "WalletConnect error. Tap 'Connect Wallet' to retry."
            _sessionState.value = SessionState.Error(userMessage)

            // Cleanup pending state
            cleanupPendingConnection("callback_crash_$callbackName")
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Internal: Parse accounts
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Parse accounts from CAIP-10 format and convert to RailConnections.
     * Applies address normalization for each chain type.
     */
    internal fun parseAccountsToRails(
        accounts: List<String>,
        sessionId: String
    ): Map<String, RailConnection> {
        val rails = mutableMapOf<String, RailConnection>()

        for (account in accounts) {
            val parsed = CaipUtils.parseAccount(account) ?: continue
            val mapping = CaipUtils.fromChainId(parsed.fullChainId) ?: continue

            val (railId, networkId) = mapping

            // Normalize address based on chain type
            val normalizedAddress = AddressNormalizer.normalize(railId, parsed.address)

            val connection = RailConnection(
                railId = railId,
                networkId = networkId,
                accountId = normalizedAddress,
                connectedAt = System.currentTimeMillis(),
                sessionId = sessionId
            )

            rails[railId] = connection
            Log.d(TAG, "WC|PARSE|rail=$railId|network=$networkId|address=${normalizedAddress.take(12)}...")
        }

        return rails
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // Internal: Open wallet app
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun openWalletApp(uri: String) {
        try {
            val parsedUri = Uri.parse(uri)
            Log.i(TAG, "WC|REDIRECT|PREPARE|scheme=${parsedUri.scheme}|uriLen=${uri.length}")

            val intent = Intent(Intent.ACTION_VIEW, parsedUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Check if any app can handle this intent
            val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            Log.i(TAG, "WC|REDIRECT|RESOLVE|hasHandler=${resolveInfo != null}|handler=${resolveInfo?.activityInfo?.packageName}")

            context.startActivity(intent)
            Log.i(TAG, "WC|REDIRECT|LAUNCHED|success=true")
        } catch (e: Exception) {
            Log.w(TAG, "WC|REDIRECT|ERROR|class=${e.javaClass.simpleName}|msg=${e.message}")
            // Don't fail - user can manually switch to wallet app
        }
    }
}
