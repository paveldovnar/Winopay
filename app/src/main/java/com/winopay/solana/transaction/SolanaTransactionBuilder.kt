package com.winopay.solana.transaction

import android.util.Log
import com.winopay.BuildConfig
import com.winopay.solana.rpc.RpcResult
import com.winopay.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds Solana transactions for SPL token transfers with memo.
 */
class SolanaTransactionBuilder(
    private val rpcUrl: String = BuildConfig.SOLANA_RPC_URL
) {

    companion object {
        private const val TAG = "SolanaTransactionBuilder"

        // Program IDs
        private val TOKEN_PROGRAM_ID = base58Decode("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
        private val ASSOCIATED_TOKEN_PROGRAM_ID = base58Decode("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")
        private val MEMO_PROGRAM_ID = base58Decode("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        private val SYSTEM_PROGRAM_ID = base58Decode("11111111111111111111111111111111")

        // SPL Token instruction discriminators
        private const val TRANSFER_CHECKED_INSTRUCTION: Byte = 12
    }

    /**
     * Build a refund transaction (SPL token transfer back to payer).
     *
     * REFUND FLOW:
     * - Merchant (sender) sends tokens back to payer (recipient)
     * - Uses the actual mint from the original payment
     * - Includes memo with invoice ID for tracking
     *
     * @param merchantPublicKey The merchant's public key (sender)
     * @param payerPublicKey The payer's public key (recipient of refund)
     * @param tokenMint The SPL token mint address
     * @param amount The refund amount in raw units
     * @param decimals Token decimals (USDC/USDT = 6)
     * @param invoiceId Invoice ID for memo
     * @return Serialized unsigned transaction bytes or error
     */
    suspend fun buildRefundTransaction(
        merchantPublicKey: String,
        payerPublicKey: String,
        tokenMint: String,
        amount: Long,
        decimals: Int,
        invoiceId: String
    ): RpcResult<ByteArray> {
        val memo = "WinoPay Refund: $invoiceId"
        Log.d(TAG, "REFUND|BUILD|invoice=$invoiceId|amount=$amount|mint=${tokenMint.take(8)}...")
        return buildTransferWithMemo(
            senderPublicKey = merchantPublicKey,
            recipientPublicKey = payerPublicKey,
            tokenMint = tokenMint,
            amount = amount,
            decimals = decimals,
            memo = memo
        )
    }

    /**
     * Build a SPL token transfer transaction with memo.
     *
     * @param senderPublicKey The sender's public key (wallet address)
     * @param recipientPublicKey The recipient's public key
     * @param tokenMint The SPL token mint address
     * @param amount The amount in raw units (e.g., for USDC: amount * 10^6)
     * @param decimals Token decimals (USDC = 6)
     * @param memo Memo string to include
     * @return Serialized transaction bytes or error
     */
    suspend fun buildTransferWithMemo(
        senderPublicKey: String,
        recipientPublicKey: String,
        tokenMint: String,
        amount: Long,
        decimals: Int,
        memo: String
    ): RpcResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Building transfer: $amount tokens from $senderPublicKey to $recipientPublicKey")

            // Get recent blockhash
            val blockhashResult = getRecentBlockhash()
            if (blockhashResult is RpcResult.Error) {
                return@withContext blockhashResult
            }
            val blockhash = (blockhashResult as RpcResult.Success).data

            // Derive Associated Token Accounts
            val senderAta = deriveAssociatedTokenAddress(senderPublicKey, tokenMint)
            val recipientAta = deriveAssociatedTokenAddress(recipientPublicKey, tokenMint)

            Log.d(TAG, "Sender ATA: $senderAta")
            Log.d(TAG, "Recipient ATA: $recipientAta")

            // Check if recipient ATA exists
            val recipientAtaExists = checkAccountExists(recipientAta)

            // Build instructions
            val instructions = mutableListOf<Instruction>()

            // If recipient ATA doesn't exist, create it
            if (!recipientAtaExists) {
                Log.d(TAG, "Recipient ATA doesn't exist, adding create instruction")
                instructions.add(
                    createAssociatedTokenAccountInstruction(
                        payer = senderPublicKey,
                        associatedToken = recipientAta,
                        owner = recipientPublicKey,
                        mint = tokenMint
                    )
                )
            }

            // Add transfer instruction
            instructions.add(
                createTransferCheckedInstruction(
                    source = senderAta,
                    mint = tokenMint,
                    destination = recipientAta,
                    owner = senderPublicKey,
                    amount = amount,
                    decimals = decimals
                )
            )

            // Add memo instruction
            instructions.add(
                createMemoInstruction(memo)
            )

            // Build transaction
            val transaction = buildTransaction(
                feePayer = senderPublicKey,
                recentBlockhash = blockhash,
                instructions = instructions
            )

            Log.d(TAG, "Transaction built: ${transaction.size} bytes")
            RpcResult.Success(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build transaction: ${e.message}", e)
            RpcResult.Error("Failed to build transaction: ${e.message}", e)
        }
    }

    /**
     * Get recent blockhash from RPC.
     */
    private suspend fun getRecentBlockhash(): RpcResult<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getLatestBlockhash")
                put("params", JSONArray().apply {
                    put(JSONObject().apply {
                        put("commitment", "finalized")
                    })
                })
            }

            val response = executeRpc(body)

            if (response.has("error")) {
                val error = response.getJSONObject("error")
                return@withContext RpcResult.Error(error.optString("message", "Unknown error"))
            }

            val blockhash = response
                .getJSONObject("result")
                .getJSONObject("value")
                .getString("blockhash")

            Log.d(TAG, "Got blockhash: $blockhash")
            RpcResult.Success(blockhash)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get blockhash: ${e.message}", e)
            RpcResult.Error("Failed to get blockhash: ${e.message}", e)
        }
    }

    /**
     * Check if an account exists.
     */
    private suspend fun checkAccountExists(address: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getAccountInfo")
                put("params", JSONArray().apply {
                    put(address)
                    put(JSONObject().apply {
                        put("encoding", "base64")
                    })
                })
            }

            val response = executeRpc(body)
            val value = response.getJSONObject("result").opt("value")
            value != null && value != JSONObject.NULL
        } catch (e: Exception) {
            Log.w(TAG, "Error checking account: ${e.message}")
            false
        }
    }

    /**
     * Derive Associated Token Address.
     */
    private fun deriveAssociatedTokenAddress(owner: String, mint: String): String {
        val ownerBytes = base58Decode(owner)
        val mintBytes = base58Decode(mint)

        // PDA derivation: seeds = [owner, TOKEN_PROGRAM_ID, mint]
        val seeds = listOf(
            ownerBytes,
            TOKEN_PROGRAM_ID,
            mintBytes
        )

        val pda = findProgramAddress(seeds, ASSOCIATED_TOKEN_PROGRAM_ID)
        return base58Encode(pda)
    }

    /**
     * Find Program Derived Address.
     */
    private fun findProgramAddress(seeds: List<ByteArray>, programId: ByteArray): ByteArray {
        var nonce = 255
        while (nonce >= 0) {
            try {
                val seedsWithNonce = seeds + listOf(byteArrayOf(nonce.toByte()))
                val address = createProgramAddress(seedsWithNonce, programId)
                // Check if it's off curve (valid PDA)
                return address
            } catch (e: Exception) {
                nonce--
            }
        }
        throw IllegalStateException("Unable to find valid PDA")
    }

    /**
     * Create program address from seeds.
     */
    private fun createProgramAddress(seeds: List<ByteArray>, programId: ByteArray): ByteArray {
        val buffer = ByteArrayOutputStream()
        for (seed in seeds) {
            buffer.write(seed)
        }
        buffer.write(programId)
        buffer.write("ProgramDerivedAddress".toByteArray())

        // SHA256 hash
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(buffer.toByteArray())
    }

    /**
     * Create Associated Token Account instruction.
     */
    private fun createAssociatedTokenAccountInstruction(
        payer: String,
        associatedToken: String,
        owner: String,
        mint: String
    ): Instruction {
        return Instruction(
            programId = ASSOCIATED_TOKEN_PROGRAM_ID,
            accounts = listOf(
                AccountMeta(base58Decode(payer), isSigner = true, isWritable = true),
                AccountMeta(base58Decode(associatedToken), isSigner = false, isWritable = true),
                AccountMeta(base58Decode(owner), isSigner = false, isWritable = false),
                AccountMeta(base58Decode(mint), isSigner = false, isWritable = false),
                AccountMeta(SYSTEM_PROGRAM_ID, isSigner = false, isWritable = false),
                AccountMeta(TOKEN_PROGRAM_ID, isSigner = false, isWritable = false)
            ),
            data = byteArrayOf()
        )
    }

    /**
     * Create SPL Token TransferChecked instruction.
     */
    private fun createTransferCheckedInstruction(
        source: String,
        mint: String,
        destination: String,
        owner: String,
        amount: Long,
        decimals: Int
    ): Instruction {
        val data = ByteBuffer.allocate(10)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(TRANSFER_CHECKED_INSTRUCTION)
            .putLong(amount)
            .put(decimals.toByte())
            .array()

        return Instruction(
            programId = TOKEN_PROGRAM_ID,
            accounts = listOf(
                AccountMeta(base58Decode(source), isSigner = false, isWritable = true),
                AccountMeta(base58Decode(mint), isSigner = false, isWritable = false),
                AccountMeta(base58Decode(destination), isSigner = false, isWritable = true),
                AccountMeta(base58Decode(owner), isSigner = true, isWritable = false)
            ),
            data = data
        )
    }

    /**
     * Create Memo instruction.
     */
    private fun createMemoInstruction(memo: String): Instruction {
        return Instruction(
            programId = MEMO_PROGRAM_ID,
            accounts = emptyList(),
            data = memo.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Build serialized transaction.
     */
    private fun buildTransaction(
        feePayer: String,
        recentBlockhash: String,
        instructions: List<Instruction>
    ): ByteArray {
        // Collect all unique accounts
        val accountsMap = linkedMapOf<String, AccountMeta>()

        // Fee payer is always first
        val feePayerBytes = base58Decode(feePayer)
        accountsMap[feePayer] = AccountMeta(feePayerBytes, isSigner = true, isWritable = true)

        // Add accounts from instructions
        for (instruction in instructions) {
            for (account in instruction.accounts) {
                val key = base58Encode(account.pubkey)
                val existing = accountsMap[key]
                if (existing != null) {
                    // Merge: if either is signer, result is signer; if either is writable, result is writable
                    accountsMap[key] = AccountMeta(
                        account.pubkey,
                        isSigner = existing.isSigner || account.isSigner,
                        isWritable = existing.isWritable || account.isWritable
                    )
                } else {
                    accountsMap[key] = account
                }
            }
            // Add program ID
            val programKey = base58Encode(instruction.programId)
            if (!accountsMap.containsKey(programKey)) {
                accountsMap[programKey] = AccountMeta(instruction.programId, isSigner = false, isWritable = false)
            }
        }

        // Sort accounts: signers first (writable before read-only), then non-signers (writable before read-only)
        val sortedAccounts = accountsMap.values.sortedWith(
            compareByDescending<AccountMeta> { it.isSigner }
                .thenByDescending { it.isWritable }
        )

        // Build account index map
        val accountIndexMap = sortedAccounts.mapIndexed { index, meta ->
            base58Encode(meta.pubkey) to index
        }.toMap()

        // Count signers and writable accounts
        val numRequiredSignatures = sortedAccounts.count { it.isSigner }
        val numReadonlySignedAccounts = sortedAccounts.count { it.isSigner && !it.isWritable }
        val numReadonlyUnsignedAccounts = sortedAccounts.count { !it.isSigner && !it.isWritable }

        // Serialize message
        val buffer = ByteArrayOutputStream()

        // Header
        buffer.write(numRequiredSignatures)
        buffer.write(numReadonlySignedAccounts)
        buffer.write(numReadonlyUnsignedAccounts)

        // Account addresses
        writeCompactU16(buffer, sortedAccounts.size)
        for (account in sortedAccounts) {
            buffer.write(account.pubkey)
        }

        // Recent blockhash
        buffer.write(base58Decode(recentBlockhash))

        // Instructions
        writeCompactU16(buffer, instructions.size)
        for (instruction in instructions) {
            val programIndex = accountIndexMap[base58Encode(instruction.programId)]!!
            buffer.write(programIndex)

            // Account indices
            writeCompactU16(buffer, instruction.accounts.size)
            for (account in instruction.accounts) {
                val index = accountIndexMap[base58Encode(account.pubkey)]!!
                buffer.write(index)
            }

            // Data
            writeCompactU16(buffer, instruction.data.size)
            buffer.write(instruction.data)
        }

        val message = buffer.toByteArray()

        // Return unsigned transaction (1 empty signature slot + message)
        val txBuffer = ByteArrayOutputStream()
        writeCompactU16(txBuffer, numRequiredSignatures) // Number of signatures
        repeat(numRequiredSignatures) {
            txBuffer.write(ByteArray(64)) // Empty signature placeholder
        }
        txBuffer.write(message)

        return txBuffer.toByteArray()
    }

    /**
     * Write compact u16 encoding.
     */
    private fun writeCompactU16(buffer: ByteArrayOutputStream, value: Int) {
        var remaining = value
        while (true) {
            var elem = remaining and 0x7f
            remaining = remaining shr 7
            if (remaining == 0) {
                buffer.write(elem)
                break
            } else {
                elem = elem or 0x80
                buffer.write(elem)
            }
        }
    }

    /**
     * Execute RPC call.
     */
    private fun executeRpc(body: JSONObject): JSONObject {
        val url = URL(rpcUrl)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }

            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Instruction data class.
     */
    private data class Instruction(
        val programId: ByteArray,
        val accounts: List<AccountMeta>,
        val data: ByteArray
    )

    /**
     * Account metadata.
     */
    private data class AccountMeta(
        val pubkey: ByteArray,
        val isSigner: Boolean,
        val isWritable: Boolean
    )
}

/**
 * Base58 alphabet.
 */
private const val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

/**
 * Decode Base58 string to bytes.
 */
private fun base58Decode(input: String): ByteArray {
    var num = java.math.BigInteger.ZERO
    val base = java.math.BigInteger.valueOf(58)

    for (c in input) {
        val digit = BASE58_ALPHABET.indexOf(c)
        if (digit < 0) throw IllegalArgumentException("Invalid Base58 character: $c")
        num = num.multiply(base).add(java.math.BigInteger.valueOf(digit.toLong()))
    }

    var bytes = num.toByteArray()

    // Remove leading zero if present (from BigInteger sign)
    if (bytes.isNotEmpty() && bytes[0] == 0.toByte() && bytes.size > 1) {
        bytes = bytes.copyOfRange(1, bytes.size)
    }

    // Count leading '1's and add leading zeros
    var leadingZeros = 0
    for (c in input) {
        if (c == '1') leadingZeros++ else break
    }

    // Ensure result is 32 bytes for public keys
    val result = ByteArray(leadingZeros + bytes.size)
    System.arraycopy(bytes, 0, result, leadingZeros, bytes.size)

    return result
}

/**
 * Encode bytes to Base58 string.
 */
private fun base58Encode(bytes: ByteArray): String {
    var num = java.math.BigInteger(1, bytes)
    val sb = StringBuilder()
    val base = java.math.BigInteger.valueOf(58)

    while (num > java.math.BigInteger.ZERO) {
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
