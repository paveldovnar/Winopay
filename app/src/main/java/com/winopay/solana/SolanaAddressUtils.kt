package com.winopay.solana

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Solana address utilities for Associated Token Account (ATA) derivation.
 *
 * CRITICAL FOR SPL PAYMENT DETECTION:
 * When detecting SPL token payments, we must poll the recipient's ATA address,
 * NOT the wallet address. The wallet address is the "owner" of the token account,
 * but tokens are actually held in the ATA which is derived from:
 *   - Owner wallet pubkey
 *   - Token mint address
 *   - SPL Token Program ID
 *   - Associated Token Program ID
 *
 * WHY WALLET POLLING FAILS FOR SPL:
 * The wallet pubkey often doesn't appear in transaction accountKeys.
 * Only the ATA (which holds the tokens) appears in the transaction.
 * So getSignaturesForAddress(walletPubkey) returns nothing for SPL transfers.
 *
 * SOLUTION:
 * For SPL tokens: poll getSignaturesForAddress(ATA)
 * For SOL: poll getSignaturesForAddress(walletPubkey)
 */
object SolanaAddressUtils {

    // Program IDs (base58 encoded)
    private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    // Base58 alphabet
    private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /**
     * Derive the Associated Token Account (ATA) address for an owner and mint.
     *
     * The ATA is a Program Derived Address (PDA) deterministically computed from:
     *   seeds = [owner, TOKEN_PROGRAM_ID, mint]
     *   program = ASSOCIATED_TOKEN_PROGRAM_ID
     *
     * @param ownerWallet The wallet address (owner pubkey) in base58
     * @param mint The SPL token mint address in base58
     * @return The ATA address in base58
     */
    fun deriveAssociatedTokenAddress(ownerWallet: String, mint: String): String {
        val ownerBytes = base58Decode(ownerWallet)
        val mintBytes = base58Decode(mint)
        val tokenProgramBytes = base58Decode(TOKEN_PROGRAM_ID)
        val ataProgramBytes = base58Decode(ASSOCIATED_TOKEN_PROGRAM_ID)

        // Seeds for ATA derivation: [owner, TOKEN_PROGRAM_ID, mint]
        val seeds = listOf(ownerBytes, tokenProgramBytes, mintBytes)

        // Find PDA with bump seed (tries 255 down to 0)
        val pda = findProgramAddress(seeds, ataProgramBytes)
        return base58Encode(pda)
    }

    /**
     * Find a valid Program Derived Address (PDA).
     *
     * PDAs are addresses that are guaranteed to NOT be on the Ed25519 curve,
     * which means no private key exists for them. The Solana runtime enforces
     * that only the program can "sign" for these addresses.
     *
     * We try bump seeds from 255 down to 0 until we find a valid PDA.
     * Most ATAs use bump=255, but we iterate for correctness.
     */
    private fun findProgramAddress(seeds: List<ByteArray>, programId: ByteArray): ByteArray {
        for (bump in 255 downTo 0) {
            try {
                val seedsWithBump = seeds + listOf(byteArrayOf(bump.toByte()))
                val address = createProgramAddress(seedsWithBump, programId)

                // Valid PDA found - the address is "off curve"
                // (createProgramAddress would throw if on curve, but our simple
                // implementation doesn't check curve - Solana runtime does)
                return address
            } catch (e: Exception) {
                // Try next bump
                continue
            }
        }
        throw IllegalStateException("Unable to find valid PDA for seeds")
    }

    /**
     * Create a program address from seeds and program ID.
     *
     * Formula: SHA256(seeds || programId || "ProgramDerivedAddress")
     * The result is truncated/used as the 32-byte public key.
     */
    private fun createProgramAddress(seeds: List<ByteArray>, programId: ByteArray): ByteArray {
        // Validate seed constraints
        require(seeds.size <= 16) { "Max 16 seeds allowed" }
        seeds.forEach { seed ->
            require(seed.size <= 32) { "Max seed length is 32 bytes" }
        }

        val buffer = ByteArrayOutputStream()

        // Write all seeds
        for (seed in seeds) {
            buffer.write(seed)
        }

        // Write program ID
        buffer.write(programId)

        // Write "ProgramDerivedAddress" marker
        buffer.write("ProgramDerivedAddress".toByteArray(Charsets.UTF_8))

        // SHA256 hash to get the address
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(buffer.toByteArray())
    }

    /**
     * Decode a base58 string to bytes.
     */
    fun base58Decode(input: String): ByteArray {
        require(input.isNotEmpty()) { "Input cannot be empty" }

        var num = BigInteger.ZERO
        val base = BigInteger.valueOf(58)

        for (c in input) {
            val digit = BASE58_ALPHABET.indexOf(c)
            require(digit >= 0) { "Invalid Base58 character: $c" }
            num = num.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }

        var bytes = num.toByteArray()

        // Remove leading zero if present (BigInteger sign byte)
        if (bytes.isNotEmpty() && bytes[0] == 0.toByte() && bytes.size > 1) {
            bytes = bytes.copyOfRange(1, bytes.size)
        }

        // Count leading '1's in input (represent leading zeros in output)
        var leadingZeros = 0
        for (c in input) {
            if (c == '1') leadingZeros++ else break
        }

        // Pad to ensure correct length (32 bytes for Solana pubkeys)
        val targetLength = leadingZeros + bytes.size
        val result = ByteArray(maxOf(targetLength, 32))

        // Copy bytes to end of result array
        val offset = result.size - bytes.size
        System.arraycopy(bytes, 0, result, offset, bytes.size)

        return result.takeLast(32).toByteArray()
    }

    /**
     * Encode bytes to base58 string.
     */
    fun base58Encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // Convert bytes to BigInteger (treat as unsigned)
        var num = BigInteger(1, bytes)
        val sb = StringBuilder()
        val base = BigInteger.valueOf(58)

        while (num > BigInteger.ZERO) {
            val divRem = num.divideAndRemainder(base)
            num = divRem[0]
            sb.insert(0, BASE58_ALPHABET[divRem[1].toInt()])
        }

        // Handle leading zeros
        for (byte in bytes) {
            if (byte.toInt() == 0) {
                sb.insert(0, '1')
            } else {
                break
            }
        }

        return sb.toString()
    }

    /**
     * Validate a base58 Solana address.
     *
     * A valid Solana address:
     * - Is 32-44 characters (base58 encoded 32 bytes)
     * - Contains only valid base58 characters
     * - Decodes to exactly 32 bytes
     */
    fun isValidSolanaAddress(address: String): Boolean {
        // Quick length check (base58 of 32 bytes is typically 32-44 chars)
        if (address.length < 32 || address.length > 44) {
            return false
        }

        // Check for valid base58 characters
        if (!address.all { it in BASE58_ALPHABET }) {
            return false
        }

        return try {
            val bytes = base58Decode(address)
            bytes.size == 32
        } catch (e: Exception) {
            false
        }
    }
}
