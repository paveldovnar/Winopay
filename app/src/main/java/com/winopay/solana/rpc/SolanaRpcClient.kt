package com.winopay.solana.rpc

import android.util.Log
import com.winopay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Low-level Solana JSON-RPC client.
 */
class SolanaRpcClient(
    private val rpcUrl: String = BuildConfig.SOLANA_RPC_URL
) {

    companion object {
        private const val TAG = "SolanaRpcClient"
        private const val TIMEOUT_MS = 30_000
    }

    /**
     * Get native SOL balance in lamports.
     * RPC method: getBalance
     */
    suspend fun getBalance(publicKey: String): RpcResult<Long> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getBalance")
                put("params", JSONArray().apply {
                    put(publicKey)
                })
            }

            val response = executeRpc(body)

            if (response.has("error")) {
                val error = response.getJSONObject("error")
                val message = error.optString("message", "Unknown error")
                Log.e(TAG, "getBalance error: $message")
                return@withContext RpcResult.Error(message)
            }

            val result = response.getJSONObject("result")
            val lamports = result.getLong("value")
            Log.d(TAG, "getBalance for $publicKey: $lamports lamports")
            RpcResult.Success(lamports)
        } catch (e: Exception) {
            Log.e(TAG, "getBalance failed: ${e.message}", e)
            RpcResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Get SPL token accounts by owner for a specific mint.
     * RPC method: getTokenAccountsByOwner
     * Returns raw token amount (before decimal conversion).
     */
    suspend fun getTokenBalance(
        ownerPublicKey: String,
        mintAddress: String
    ): RpcResult<Long> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getTokenAccountsByOwner")
                put("params", JSONArray().apply {
                    put(ownerPublicKey)
                    put(JSONObject().apply {
                        put("mint", mintAddress)
                    })
                    put(JSONObject().apply {
                        put("encoding", "jsonParsed")
                    })
                })
            }

            val response = executeRpc(body)

            if (response.has("error")) {
                val error = response.getJSONObject("error")
                val message = error.optString("message", "Unknown error")
                Log.e(TAG, "getTokenAccountsByOwner error: $message")
                return@withContext RpcResult.Error(message)
            }

            val result = response.getJSONObject("result")
            val valueArray = result.getJSONArray("value")

            if (valueArray.length() == 0) {
                // No token account found - balance is 0
                Log.d(TAG, "No token account found for $ownerPublicKey, mint: $mintAddress")
                return@withContext RpcResult.Success(0L)
            }

            // Get first token account (usually only one per mint per owner)
            val tokenAccount = valueArray.getJSONObject(0)
            val accountData = tokenAccount.getJSONObject("account")
                .getJSONObject("data")
                .getJSONObject("parsed")
                .getJSONObject("info")
                .getJSONObject("tokenAmount")

            val amount = accountData.getString("amount").toLongOrNull() ?: 0L
            Log.d(TAG, "Token balance for $ownerPublicKey: $amount (raw)")
            RpcResult.Success(amount)
        } catch (e: Exception) {
            Log.e(TAG, "getTokenAccountsByOwner failed: ${e.message}", e)
            RpcResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Get ALL SPL token accounts for an owner (no mint filter).
     * Used for diagnostic purposes to detect mint mismatches.
     * RPC method: getTokenAccountsByOwner with programId filter.
     */
    suspend fun getAllTokenAccounts(ownerPublicKey: String): RpcResult<List<TokenAccountInfo>> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getTokenAccountsByOwner")
                put("params", JSONArray().apply {
                    put(ownerPublicKey)
                    put(JSONObject().apply {
                        // Filter by SPL Token program instead of mint
                        put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
                    })
                    put(JSONObject().apply {
                        put("encoding", "jsonParsed")
                    })
                })
            }

            val response = executeRpc(body)

            if (response.has("error")) {
                val error = response.getJSONObject("error")
                val message = error.optString("message", "Unknown error")
                Log.e(TAG, "getAllTokenAccounts error: $message")
                return@withContext RpcResult.Error(message)
            }

            val result = response.getJSONObject("result")
            val valueArray = result.getJSONArray("value")
            val accounts = mutableListOf<TokenAccountInfo>()

            for (i in 0 until valueArray.length()) {
                try {
                    val tokenAccount = valueArray.getJSONObject(i)
                    val pubkey = tokenAccount.getString("pubkey")
                    val accountData = tokenAccount.getJSONObject("account")
                        .getJSONObject("data")
                        .getJSONObject("parsed")
                        .getJSONObject("info")

                    val mint = accountData.getString("mint")
                    val tokenAmount = accountData.getJSONObject("tokenAmount")
                    val amount = tokenAmount.getString("amount")
                    val decimals = tokenAmount.getInt("decimals")
                    val uiAmountString = tokenAmount.optString("uiAmountString", "0")

                    accounts.add(TokenAccountInfo(
                        pubkey = pubkey,
                        mint = mint,
                        amountRaw = amount,
                        decimals = decimals,
                        uiAmountString = uiAmountString
                    ))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse token account at index $i: ${e.message}")
                }
            }

            Log.d(TAG, "getAllTokenAccounts for $ownerPublicKey: found ${accounts.size} accounts")
            RpcResult.Success(accounts)
        } catch (e: Exception) {
            Log.e(TAG, "getAllTokenAccounts failed: ${e.message}", e)
            RpcResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Get signature status for a transaction.
     * RPC method: getSignatureStatuses
     */
    suspend fun getSignatureStatus(signature: String): RpcResult<SignatureStatus> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getSignatureStatuses")
                put("params", JSONArray().apply {
                    put(JSONArray().apply {
                        put(signature)
                    })
                    put(JSONObject().apply {
                        put("searchTransactionHistory", true)
                    })
                })
            }

            val response = executeRpc(body)

            if (response.has("error")) {
                val error = response.getJSONObject("error")
                val message = error.optString("message", "Unknown error")
                Log.e(TAG, "getSignatureStatuses error: $message")
                return@withContext RpcResult.Error(message)
            }

            val result = response.getJSONObject("result")
            val valueArray = result.getJSONArray("value")

            if (valueArray.length() == 0 || valueArray.isNull(0)) {
                Log.d(TAG, "Signature not found: $signature")
                return@withContext RpcResult.Success(SignatureStatus.NotFound)
            }

            val statusObj = valueArray.getJSONObject(0)
            val err = statusObj.opt("err")
            val confirmationStatus = statusObj.optString("confirmationStatus", null)
            val slot = statusObj.optLong("slot", 0)
            val confirmations = statusObj.opt("confirmations")

            val status = when {
                err != null && err != JSONObject.NULL -> {
                    Log.d(TAG, "Transaction failed: $signature, error: $err")
                    SignatureStatus.Failed(err.toString())
                }
                confirmationStatus == "finalized" -> {
                    Log.d(TAG, "Transaction finalized: $signature")
                    SignatureStatus.Confirmed(slot, "finalized")
                }
                confirmationStatus == "confirmed" -> {
                    Log.d(TAG, "Transaction confirmed: $signature")
                    SignatureStatus.Confirmed(slot, "confirmed")
                }
                confirmationStatus == "processed" -> {
                    Log.d(TAG, "Transaction processed: $signature")
                    SignatureStatus.Processing(slot)
                }
                else -> {
                    Log.d(TAG, "Transaction pending: $signature")
                    SignatureStatus.Processing(slot)
                }
            }

            RpcResult.Success(status)
        } catch (e: Exception) {
            Log.e(TAG, "getSignatureStatuses failed: ${e.message}", e)
            RpcResult.Error(e.message ?: "Unknown error", e)
        }
    }

    /**
     * Execute raw RPC call.
     */
    private fun executeRpc(body: JSONObject): JSONObject {
        val url = URL(rpcUrl)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
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

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RpcException("HTTP $responseCode: $responseText")
            }

            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Result wrapper for RPC calls.
 */
sealed class RpcResult<out T> {
    data class Success<T>(val data: T) : RpcResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : RpcResult<Nothing>()
}

class RpcException(message: String) : Exception(message)

/**
 * Signature status result.
 */
sealed class SignatureStatus {
    data object NotFound : SignatureStatus()
    data class Processing(val slot: Long) : SignatureStatus()
    data class Confirmed(val slot: Long, val confirmationStatus: String) : SignatureStatus()
    data class Failed(val error: String) : SignatureStatus()
}

/**
 * Token account info for diagnostic purposes.
 */
data class TokenAccountInfo(
    val pubkey: String,
    val mint: String,
    val amountRaw: String,
    val decimals: Int,
    val uiAmountString: String
) {
    /**
     * Parse raw amount to Long (minor units).
     */
    fun amountAsLong(): Long = amountRaw.toLongOrNull() ?: 0L

    /**
     * Check if this account has non-zero balance.
     */
    fun hasBalance(): Boolean = amountAsLong() > 0L
}
