package com.winopay.solana.wallet

import android.net.Uri
import android.util.Log
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.winopay.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles connection to external Solana wallets via Mobile Wallet Adapter.
 *
 * Usage:
 * 1. Call connect() with an ActivityResultSender
 * 2. Observe connectionState for updates
 * 3. Use the connected public key for transactions
 */
class WalletConnector {

    companion object {
        private const val TAG = "WalletConnector"
        private const val APP_IDENTITY_NAME = "WinoPay"
        private const val APP_IDENTITY_URI = "https://winopay.app"
        private const val APP_ICON_URI = "favicon.ico"

        // CAIP-2 chain identifiers for Solana
        // Format: solana:<genesisHash>
        private const val CHAIN_MAINNET = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdpKuc147dw2N9d"
        private const val CHAIN_DEVNET = "solana:EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG"
        private const val CHAIN_TESTNET = "solana:4uhcVJyU9pJkvQyS88uRDiswHXSCkY3zQawwpjk2NsNY"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow<WalletConnectionState>(WalletConnectionState.Disconnected)
    val connectionState: StateFlow<WalletConnectionState> = _connectionState.asStateFlow()

    // Cached auth token for reconnection
    private var cachedAuthToken: String? = null
    private var cachedPublicKey: String? = null
    private var cachedWalletName: String? = null

    // Connection identity for MWA
    private val connectionIdentity = ConnectionIdentity(
        identityUri = Uri.parse(APP_IDENTITY_URI),
        iconUri = Uri.parse(APP_ICON_URI),
        identityName = APP_IDENTITY_NAME
    )

    // MWA instance
    private val mobileWalletAdapter = MobileWalletAdapter(connectionIdentity)

    /**
     * Get the Solana cluster name (human-readable) based on build config.
     * Used for logging and user-facing messages only.
     */
    private fun getClusterName(): String {
        return when (val cluster = BuildConfig.SOLANA_CLUSTER.lowercase()) {
            "mainnet-beta", "mainnet", "production" -> "mainnet-beta"
            "testnet" -> "testnet"
            "devnet", "development", "" -> "devnet"
            else -> {
                Log.w(TAG, "Unknown SOLANA_CLUSTER value: '$cluster', defaulting to devnet")
                "devnet"
            }
        }
    }

    /**
     * Get the CAIP-2 chain identifier for MWA.
     * Format: solana:<genesisHash>
     *
     * CRITICAL: This MUST return a valid CAIP-2 identifier.
     * Invalid identifiers will cause wallet connection failures:
     * - Solflare: "provided chain is not valid chain identifier"
     * - Phantom: "Failed establishing local association"
     *
     * NOTE: No require/check/assert here - validation happens in connect() via validateChainId()
     *
     * @see https://github.com/ChainAgnostic/CAIPs/blob/master/CAIPs/caip-2.md
     */
    private fun getChainId(): String {
        return when (val cluster = BuildConfig.SOLANA_CLUSTER.lowercase()) {
            "mainnet-beta", "mainnet", "production" -> CHAIN_MAINNET
            "testnet" -> CHAIN_TESTNET
            "devnet", "development", "" -> CHAIN_DEVNET
            else -> {
                Log.w(TAG, "Unknown SOLANA_CLUSTER '$cluster', using devnet chain")
                CHAIN_DEVNET
            }
        }
    }

    /**
     * Validate chain ID before authorize.
     * Returns error message if invalid, null if valid.
     */
    private fun validateChainId(chainId: String): String? {
        if (!chainId.startsWith("solana:")) {
            return "Invalid chain identifier format. Expected 'solana:<genesisHash>', got: $chainId"
        }
        if (chainId.length < 50) { // "solana:" + 43-44 char base58 hash
            return "Chain identifier too short: $chainId"
        }
        return null
    }

    /**
     * Get network configuration hint for user-facing messages.
     */
    fun getNetworkHint(): NetworkHint {
        val cluster = getClusterName()
        return when (cluster) {
            "mainnet-beta" -> NetworkHint(
                cluster = cluster,
                displayName = "Mainnet",
                phantomInstructions = "Ensure Phantom is set to Mainnet (default setting)"
            )
            "testnet" -> NetworkHint(
                cluster = cluster,
                displayName = "Testnet",
                phantomInstructions = "Phantom: Settings → Developer Settings → Change Network → Testnet"
            )
            else -> NetworkHint(
                cluster = cluster,
                displayName = "Devnet",
                phantomInstructions = "Phantom: Settings → Developer Settings → Change Network → Devnet"
            )
        }
    }

    /**
     * Network configuration hint for UI.
     */
    data class NetworkHint(
        val cluster: String,
        val displayName: String,
        val phantomInstructions: String
    )

    /**
     * Connect to a wallet using Mobile Wallet Adapter.
     *
     * @param sender ActivityResultSender from the current activity
     * @param existingAuthToken Optional auth token from previous session for reauthorization
     */
    suspend fun connect(
        sender: ActivityResultSender,
        existingAuthToken: String? = null
    ): WalletResult<WalletConnectionState.Connected> {
        _connectionState.value = WalletConnectionState.Connecting

        val cluster = getClusterName()
        val chainId = getChainId()

        // ━━━━━ STRUCTURED LOG: START ━━━━━
        Log.i(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        Log.i(TAG, "┃           MWA CONNECT REQUEST                 ┃")
        Log.i(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        Log.i(TAG, "┃ Cluster (human):    $cluster")
        Log.i(TAG, "┃ Chain ID (CAIP-2):  $chainId")
        Log.i(TAG, "┃ Identity:           $APP_IDENTITY_NAME")
        Log.i(TAG, "┃ Identity URI:       $APP_IDENTITY_URI")
        Log.i(TAG, "┃ Reauthorization:    ${existingAuthToken != null}")
        Log.i(TAG, "┃ BuildConfig.CLUSTER: ${BuildConfig.SOLANA_CLUSTER}")
        Log.i(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        // ━━━━━ RUNTIME ASSERTION: Chain ID must be valid CAIP-2 ━━━━━
        val validationError = validateChainId(chainId)
        if (validationError != null) {
            Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            Log.e(TAG, "┃           CHAIN ID VALIDATION FAILED          ┃")
            Log.e(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
            Log.e(TAG, "┃ Error: $validationError")
            Log.e(TAG, "┃ Chain ID: $chainId")
            Log.e(TAG, "┃ Expected format: solana:<genesisHash>")
            Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

            val errorMsg = "Configuration error: Invalid chain identifier. Please reinstall the app."
            _connectionState.value = WalletConnectionState.Error(errorMsg)
            return WalletResult.Failure(errorMsg)
        }

        return try {
            val result = mobileWalletAdapter.transact(sender) {
                // Try to reauthorize with existing token first
                if (existingAuthToken != null) {
                    Log.d(TAG, "Attempting reauthorization with existing token")
                    try {
                        val reauthorizeResult = reauthorize(
                            identityUri = Uri.parse(APP_IDENTITY_URI),
                            iconUri = Uri.parse(APP_ICON_URI),
                            identityName = APP_IDENTITY_NAME,
                            authToken = existingAuthToken
                        )

                        cachedAuthToken = reauthorizeResult.authToken
                        cachedPublicKey = reauthorizeResult.publicKey.toBase58()
                        cachedWalletName = reauthorizeResult.walletUriBase?.host

                        Log.d(TAG, "Reauthorization successful: ${cachedPublicKey}")
                        Log.d(TAG, "Wallet name: ${cachedWalletName}")
                        return@transact
                    } catch (e: Exception) {
                        Log.w(TAG, "Reauthorization failed, trying fresh authorize: ${e.message}")
                    }
                }

                // Fresh authorization with CAIP-2 chain identifier
                Log.d(TAG, "Performing fresh authorization with chain: $chainId")
                val authorizeResult = authorize(
                    identityUri = Uri.parse(APP_IDENTITY_URI),
                    iconUri = Uri.parse(APP_ICON_URI),
                    identityName = APP_IDENTITY_NAME,
                    chain = chainId
                )

                cachedAuthToken = authorizeResult.authToken
                cachedPublicKey = authorizeResult.publicKey.toBase58()
                cachedWalletName = authorizeResult.walletUriBase?.host

                Log.d(TAG, "Authorization successful: ${cachedPublicKey}")
                Log.d(TAG, "Wallet name: ${cachedWalletName}")
            }

            when (result) {
                is TransactionResult.Success -> {
                    val connectedState = WalletConnectionState.Connected(
                        publicKey = cachedPublicKey ?: "",
                        walletName = cachedWalletName,
                        authToken = cachedAuthToken
                    )
                    _connectionState.value = connectedState

                    // ━━━━━ STRUCTURED LOG: SUCCESS ━━━━━
                    Log.i(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                    Log.i(TAG, "┃           MWA CONNECT SUCCESS                 ┃")
                    Log.i(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                    Log.i(TAG, "┃ Public key: ${cachedPublicKey?.take(8)}...${cachedPublicKey?.takeLast(8)}")
                    Log.i(TAG, "┃ Wallet:     ${cachedWalletName ?: "unknown"}")
                    Log.i(TAG, "┃ Cluster:    $cluster")
                    Log.i(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                    WalletResult.Success(connectedState)
                }
                is TransactionResult.Failure -> {
                    val rawMessage = result.message ?: "Unknown error"

                    // ━━━━━ STRUCTURED LOG: FAILURE ━━━━━
                    Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                    Log.e(TAG, "┃           MWA CONNECT FAILED                  ┃")
                    Log.e(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                    Log.e(TAG, "┃ Raw error:  $rawMessage")
                    Log.e(TAG, "┃ Cluster:    $cluster")
                    Log.e(TAG, "┃ Chain ID:   $chainId")
                    Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                    // Detect and provide user-friendly error messages
                    val errorMsg = parseWalletError(rawMessage, cluster)
                    _connectionState.value = WalletConnectionState.Error(errorMsg)
                    WalletResult.Failure(errorMsg)
                }
                is TransactionResult.NoWalletFound -> {
                    Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                    Log.e(TAG, "┃           NO WALLET FOUND                     ┃")
                    Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                    val errorMsg = "No compatible wallet found. Please install a Solana wallet app."
                    _connectionState.value = WalletConnectionState.Error(errorMsg)
                    WalletResult.Failure(errorMsg)
                }
            }
        } catch (e: Exception) {
            // ━━━━━ STRUCTURED LOG: EXCEPTION ━━━━━
            Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            Log.e(TAG, "┃           MWA CONNECT EXCEPTION               ┃")
            Log.e(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
            Log.e(TAG, "┃ Exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "┃ Message:   ${e.message}")
            Log.e(TAG, "┃ Cluster:   $cluster")
            Log.e(TAG, "┃ Chain ID:  $chainId")
            Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", e)

            val errorMsg = parseWalletError(e.message ?: "Unknown error", cluster)
            _connectionState.value = WalletConnectionState.Error(errorMsg, e)
            WalletResult.Failure(errorMsg, e)
        }
    }

    /**
     * Parse wallet error messages and provide user-friendly descriptions.
     *
     * Common errors:
     * - Solflare: "provided chain is not valid chain identifier"
     * - Phantom: "Failed establishing local association with wallet"
     */
    private fun parseWalletError(rawMessage: String, appCluster: String): String {
        val lowerMessage = rawMessage.lowercase()

        return when {
            // Chain/network mismatch errors
            lowerMessage.contains("chain") && lowerMessage.contains("not valid") -> {
                "Network mismatch: Wallet doesn't support $appCluster. " +
                "Please switch your wallet to $appCluster network."
            }
            lowerMessage.contains("chain") && lowerMessage.contains("identifier") -> {
                "Invalid network: Please ensure your wallet is set to $appCluster."
            }

            // Phantom local association errors (often network-related)
            lowerMessage.contains("local association") || lowerMessage.contains("establishing") -> {
                if (appCluster == "devnet") {
                    "Connection failed: If using Phantom, ensure it's set to Devnet " +
                    "(Settings → Developer Settings → Change Network → Devnet)"
                } else {
                    "Connection failed: Could not establish connection with wallet. " +
                    "Please ensure the wallet app is running and try again."
                }
            }

            // User declined
            lowerMessage.contains("declined") || lowerMessage.contains("rejected") ||
            lowerMessage.contains("cancelled") || lowerMessage.contains("canceled") -> {
                "Connection cancelled by user."
            }

            // Timeout
            lowerMessage.contains("timeout") -> {
                "Connection timed out. Please try again."
            }

            // Default: show original message
            else -> "Connection failed: $rawMessage"
        }
    }

    /**
     * Disconnect from the wallet.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting wallet")
        cachedAuthToken = null
        cachedPublicKey = null
        cachedWalletName = null
        _connectionState.value = WalletConnectionState.Disconnected
    }

    /**
     * Check if we have a cached session that could be restored.
     */
    fun hasCachedSession(): Boolean {
        return cachedAuthToken != null && cachedPublicKey != null
    }

    /**
     * Restore connection state from persisted data.
     * Does not verify the token - call connect() to verify.
     */
    fun restoreFromCache(publicKey: String, authToken: String, walletName: String?) {
        cachedPublicKey = publicKey
        cachedAuthToken = authToken
        cachedWalletName = walletName

        _connectionState.value = WalletConnectionState.Connected(
            publicKey = publicKey,
            walletName = walletName,
            authToken = authToken
        )

        Log.d(TAG, "Restored wallet from cache: $publicKey")
    }

    /**
     * Get the current public key if connected.
     */
    fun getPublicKey(): String? {
        return (connectionState.value as? WalletConnectionState.Connected)?.publicKey
    }

    /**
     * Get the current auth token if connected.
     */
    fun getAuthToken(): String? {
        return (connectionState.value as? WalletConnectionState.Connected)?.authToken
    }

    /**
     * Sign and send a transaction using the connected wallet.
     *
     * REFUND FLOW:
     * 1. Build unsigned transaction (SolanaTransactionBuilder)
     * 2. Call this method with serialized transaction
     * 3. Wallet signs and sends to network
     * 4. Returns transaction signature on success
     *
     * @param sender ActivityResultSender from the current activity
     * @param transactionBytes Serialized unsigned transaction
     * @return Transaction signature on success, error message on failure
     */
    suspend fun signAndSendTransaction(
        sender: ActivityResultSender,
        transactionBytes: ByteArray
    ): WalletResult<String> {
        val authToken = getAuthToken()
        if (authToken == null) {
            Log.e(TAG, "signAndSendTransaction: Not connected")
            return WalletResult.Failure("Wallet not connected")
        }

        Log.i(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        Log.i(TAG, "┃           MWA SIGN & SEND REQUEST             ┃")
        Log.i(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        Log.i(TAG, "┃ Transaction size: ${transactionBytes.size} bytes")
        Log.i(TAG, "┃ Cluster:          ${getClusterName()}")
        Log.i(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        var resultSignature: String? = null

        return try {
            val result = mobileWalletAdapter.transact(sender) {
                // Reauthorize with existing token
                Log.d(TAG, "Reauthorizing for transaction signing")
                val reauthorizeResult = reauthorize(
                    identityUri = Uri.parse(APP_IDENTITY_URI),
                    iconUri = Uri.parse(APP_ICON_URI),
                    identityName = APP_IDENTITY_NAME,
                    authToken = authToken
                )

                // Update cached auth token
                cachedAuthToken = reauthorizeResult.authToken

                // Sign and send the transaction
                Log.d(TAG, "Calling signAndSendTransactions")
                val signResult = signAndSendTransactions(
                    transactions = arrayOf(transactionBytes)
                )

                // Extract signature from result
                // signAndSendTransactions returns SignaturesResult with signatures array
                val signatures = signResult.signatures
                if (signatures.isNotEmpty()) {
                    resultSignature = signatures[0].toBase58()
                    Log.d(TAG, "Transaction signed and sent: $resultSignature")
                }
            }

            when (result) {
                is TransactionResult.Success -> {
                    if (resultSignature != null) {
                        Log.i(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                        Log.i(TAG, "┃           MWA SIGN & SEND SUCCESS             ┃")
                        Log.i(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                        Log.i(TAG, "┃ Signature: ${resultSignature?.take(20)}...")
                        Log.i(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
                        WalletResult.Success(resultSignature!!)
                    } else {
                        Log.e(TAG, "Transaction succeeded but no signature returned")
                        WalletResult.Failure("No signature returned from wallet")
                    }
                }
                is TransactionResult.Failure -> {
                    val errorMsg = result.message ?: "Unknown error"
                    Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                    Log.e(TAG, "┃           MWA SIGN & SEND FAILED              ┃")
                    Log.e(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
                    Log.e(TAG, "┃ Error: $errorMsg")
                    Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
                    WalletResult.Failure(parseTransactionError(errorMsg))
                }
                is TransactionResult.NoWalletFound -> {
                    WalletResult.Failure("No wallet found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
            Log.e(TAG, "┃           MWA SIGN & SEND EXCEPTION           ┃")
            Log.e(TAG, "┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
            Log.e(TAG, "┃ Exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "┃ Message:   ${e.message}")
            Log.e(TAG, "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛", e)
            WalletResult.Failure(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Parse transaction-specific error messages.
     */
    private fun parseTransactionError(rawMessage: String): String {
        val lowerMessage = rawMessage.lowercase()

        return when {
            lowerMessage.contains("insufficient") && lowerMessage.contains("balance") -> {
                "Insufficient balance to complete the transaction."
            }
            lowerMessage.contains("declined") || lowerMessage.contains("rejected") ||
            lowerMessage.contains("cancelled") || lowerMessage.contains("canceled") -> {
                "Transaction cancelled by user."
            }
            lowerMessage.contains("timeout") -> {
                "Transaction timed out. Please try again."
            }
            else -> "Transaction failed: $rawMessage"
        }
    }
}

/**
 * Extension to convert ByteArray to Base58 string.
 */
private fun ByteArray.toBase58(): String {
    val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    var num = java.math.BigInteger(1, this)
    val sb = StringBuilder()
    val base = java.math.BigInteger.valueOf(58)

    while (num > java.math.BigInteger.ZERO) {
        val divRem = num.divideAndRemainder(base)
        num = divRem[0]
        sb.insert(0, alphabet[divRem[1].toInt()])
    }

    // Handle leading zeros
    for (byte in this) {
        if (byte.toInt() == 0) {
            sb.insert(0, '1')
        } else {
            break
        }
    }

    return sb.toString()
}
