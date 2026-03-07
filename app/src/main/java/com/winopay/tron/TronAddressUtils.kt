package com.winopay.tron

import android.util.Log
import java.security.MessageDigest

/**
 * TRON address utilities.
 *
 * TRON Address Format:
 * - Base58Check encoded, 34 characters
 * - Always starts with 'T'
 * - Hex address is 42 chars, starts with '41'
 *
 * Unlike Solana, TRON does NOT use Associated Token Accounts.
 * TRC20 balances are tracked by the token contract itself.
 * To poll for payments, we query the recipient's wallet address directly.
 */
object TronAddressUtils {

    private const val TAG = "TronAddressUtils"

    /** Base58 alphabet (same as Bitcoin) */
    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /** Valid base58check address length for TRON */
    private const val ADDRESS_LENGTH = 34

    /** TRON mainnet address prefix in hex */
    private const val HEX_PREFIX = "41"

    /**
     * Validate a TRON base58check address.
     *
     * Rules:
     * 1. Must be exactly 34 characters
     * 2. Must start with 'T'
     * 3. Must be valid base58 (no 0, O, I, l)
     * 4. Checksum must be valid (last 4 bytes)
     *
     * @param address Address to validate
     * @return true if valid TRON address
     */
    fun isValidAddress(address: String?): Boolean {
        if (address == null) return false
        if (address.length != ADDRESS_LENGTH) return false
        if (!address.startsWith("T")) return false

        // Check all characters are valid base58
        if (!address.all { it in BASE58_ALPHABET }) return false

        // Validate checksum
        return try {
            val decoded = decodeBase58(address)
            if (decoded.size != 25) return false // 21 bytes + 4 checksum

            val payload = decoded.sliceArray(0..20)
            val checksum = decoded.sliceArray(21..24)
            val expectedChecksum = sha256(sha256(payload)).sliceArray(0..3)

            checksum.contentEquals(expectedChecksum)
        } catch (e: Exception) {
            Log.w(TAG, "Address validation failed: ${e.message}")
            false
        }
    }

    /**
     * Convert base58check address to hex address.
     *
     * @param base58Address TRON base58check address (e.g., TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t)
     * @return Hex address (e.g., 41a614f803b6fd780986a42c78ec9c7f77e6ded13c)
     */
    fun toHexAddress(base58Address: String): String? {
        if (!isValidAddress(base58Address)) return null

        return try {
            val decoded = decodeBase58(base58Address)
            // Remove checksum (last 4 bytes), keep 21 bytes
            val addressBytes = decoded.sliceArray(0..20)
            bytesToHex(addressBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert to hex: ${e.message}")
            null
        }
    }

    /**
     * Convert hex address to base58check address.
     *
     * @param hexAddress Hex address starting with '41' (e.g., 41a614f803b6fd780986a42c78ec9c7f77e6ded13c)
     * @return Base58check address (e.g., TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t)
     */
    fun toBase58Address(hexAddress: String): String? {
        val normalized = hexAddress.lowercase().removePrefix("0x")
        if (normalized.length != 42) return null
        if (!normalized.startsWith(HEX_PREFIX)) return null

        return try {
            val addressBytes = hexToBytes(normalized)
            val checksum = sha256(sha256(addressBytes)).sliceArray(0..3)
            val full = addressBytes + checksum
            encodeBase58(full)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert to base58: ${e.message}")
            null
        }
    }

    /**
     * Decode base58 string to bytes.
     */
    private fun decodeBase58(input: String): ByteArray {
        var result = java.math.BigInteger.ZERO
        for (c in input) {
            val digit = BASE58_ALPHABET.indexOf(c)
            if (digit < 0) throw IllegalArgumentException("Invalid base58 character: $c")
            result = result.multiply(java.math.BigInteger.valueOf(58))
            result = result.add(java.math.BigInteger.valueOf(digit.toLong()))
        }

        var bytes = result.toByteArray()

        // Remove leading zero if present (BigInteger sign byte)
        if (bytes.isNotEmpty() && bytes[0] == 0.toByte() && bytes.size > 25) {
            bytes = bytes.sliceArray(1 until bytes.size)
        }

        // Handle leading zeros in input
        val leadingZeros = input.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + bytes
    }

    /**
     * Encode bytes to base58 string.
     */
    private fun encodeBase58(input: ByteArray): String {
        var value = java.math.BigInteger(1, input)
        val result = StringBuilder()

        while (value > java.math.BigInteger.ZERO) {
            val (div, mod) = value.divideAndRemainder(java.math.BigInteger.valueOf(58))
            result.insert(0, BASE58_ALPHABET[mod.toInt()])
            value = div
        }

        // Add leading '1's for leading zeros in input
        for (byte in input) {
            if (byte == 0.toByte()) {
                result.insert(0, '1')
            } else {
                break
            }
        }

        return result.toString()
    }

    /**
     * SHA-256 hash.
     */
    private fun sha256(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(input)
    }

    /**
     * Convert bytes to hex string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert hex string to bytes.
     */
    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * Format address for display (truncated).
     */
    fun formatForDisplay(address: String, prefixLen: Int = 6, suffixLen: Int = 4): String {
        if (address.length <= prefixLen + suffixLen + 3) return address
        return "${address.take(prefixLen)}...${address.takeLast(suffixLen)}"
    }
}
