package com.winopay.solana.wallet

/**
 * Represents the current state of wallet connection.
 */
sealed class WalletConnectionState {
    /**
     * No wallet connected.
     */
    data object Disconnected : WalletConnectionState()

    /**
     * Connection in progress.
     */
    data object Connecting : WalletConnectionState()

    /**
     * Wallet connected successfully.
     */
    data class Connected(
        val publicKey: String,
        val walletName: String?,
        val authToken: String?
    ) : WalletConnectionState()

    /**
     * Connection failed.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : WalletConnectionState()
}

/**
 * Result of a wallet operation.
 */
sealed class WalletResult<out T> {
    data class Success<T>(val data: T) : WalletResult<T>()
    data class Failure(val error: String, val cause: Throwable? = null) : WalletResult<Nothing>()
}
