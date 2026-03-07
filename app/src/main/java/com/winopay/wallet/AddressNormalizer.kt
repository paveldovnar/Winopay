package com.winopay.wallet

import android.util.Log

/**
 * Normalizes wallet addresses for different blockchain networks.
 *
 * Address formats:
 * - Solana: Base58 public key (32-44 chars, alphanumeric excluding 0, O, I, l)
 * - TRON: Base58Check starting with 'T' (34 chars)
 * - EVM: 0x-prefixed hex (42 chars), normalized to lowercase
 */
object AddressNormalizer {
    private const val TAG = "AddressNormalizer"

    // Base58 alphabet (Bitcoin-style, excludes 0, O, I, l)
    private val BASE58_REGEX = Regex("^[1-9A-HJ-NP-Za-km-z]+$")

    // EVM address regex (0x + 40 hex chars)
    private val EVM_REGEX = Regex("^0x[a-fA-F0-9]{40}$")

    // TRON address regex (starts with T, 34 chars base58)
    private val TRON_REGEX = Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$")

    // Solana pubkey length range
    private const val SOLANA_MIN_LENGTH = 32
    private const val SOLANA_MAX_LENGTH = 44

    /**
     * Normalize an address based on the rail type.
     *
     * @param railId The rail identifier ("solana", "tron", "evm")
     * @param address The raw address from WalletConnect
     * @return Normalized address
     */
    fun normalize(railId: String, address: String): String {
        val trimmed = address.trim()

        return when (railId) {
            "solana" -> normalizeSolana(trimmed)
            "tron" -> normalizeTron(trimmed)
            "evm" -> normalizeEvm(trimmed)
            else -> {
                Log.w(TAG, "Unknown rail: $railId, returning address as-is")
                trimmed
            }
        }
    }

    /**
     * Normalize Solana address (Base58 public key).
     * Validates format and returns as-is if valid.
     */
    private fun normalizeSolana(address: String): String {
        if (address.length < SOLANA_MIN_LENGTH || address.length > SOLANA_MAX_LENGTH) {
            Log.w(TAG, "Solana address length invalid: ${address.length}")
            return address
        }

        if (!BASE58_REGEX.matches(address)) {
            Log.w(TAG, "Solana address contains invalid characters")
            return address
        }

        Log.d(TAG, "Solana address valid: ${address.take(8)}...${address.takeLast(4)}")
        return address
    }

    /**
     * Normalize TRON address (Base58Check starting with 'T').
     * Validates format and returns as-is if valid.
     */
    private fun normalizeTron(address: String): String {
        if (!address.startsWith("T")) {
            Log.w(TAG, "TRON address doesn't start with 'T'")
            return address
        }

        if (!TRON_REGEX.matches(address)) {
            Log.w(TAG, "TRON address format invalid")
            return address
        }

        Log.d(TAG, "TRON address valid: ${address.take(8)}...${address.takeLast(4)}")
        return address
    }

    /**
     * Normalize EVM address (0x + 40 hex chars, lowercase).
     * Converts to lowercase for consistency.
     */
    private fun normalizeEvm(address: String): String {
        val normalized = if (address.startsWith("0x") || address.startsWith("0X")) {
            "0x" + address.drop(2).lowercase()
        } else {
            // Add 0x prefix if missing (shouldn't happen from WC)
            "0x" + address.lowercase()
        }

        if (!EVM_REGEX.matches(normalized)) {
            Log.w(TAG, "EVM address format invalid: $normalized")
            return normalized
        }

        Log.d(TAG, "EVM address normalized: ${normalized.take(10)}...${normalized.takeLast(4)}")
        return normalized
    }

    /**
     * Validate address format for a given rail.
     *
     * @param railId The rail identifier
     * @param address The address to validate
     * @return true if the address format is valid
     */
    fun isValid(railId: String, address: String): Boolean {
        val trimmed = address.trim()

        return when (railId) {
            "solana" -> isValidSolana(trimmed)
            "tron" -> isValidTron(trimmed)
            "evm" -> isValidEvm(trimmed)
            else -> false
        }
    }

    private fun isValidSolana(address: String): Boolean {
        return address.length in SOLANA_MIN_LENGTH..SOLANA_MAX_LENGTH &&
                BASE58_REGEX.matches(address)
    }

    private fun isValidTron(address: String): Boolean {
        return TRON_REGEX.matches(address)
    }

    private fun isValidEvm(address: String): Boolean {
        val normalized = if (address.startsWith("0x") || address.startsWith("0X")) {
            "0x" + address.drop(2).lowercase()
        } else {
            "0x" + address.lowercase()
        }
        return EVM_REGEX.matches(normalized)
    }

    /**
     * Get a shortened display version of an address.
     *
     * @param address The full address
     * @param prefixLen Number of characters to show at start (default 6)
     * @param suffixLen Number of characters to show at end (default 4)
     * @return Shortened address like "0x1234...5678"
     */
    fun shorten(address: String, prefixLen: Int = 6, suffixLen: Int = 4): String {
        if (address.length <= prefixLen + suffixLen + 3) {
            return address
        }
        return "${address.take(prefixLen)}...${address.takeLast(suffixLen)}"
    }
}
