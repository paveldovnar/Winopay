package com.winopay.solana.rpc

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Solana RPC provider interface for multi-provider failover.
 *
 * DESIGN PRINCIPLES:
 * 1. NO crashes - all errors captured in RpcCallResult
 * 2. 3-second timeout per call (fast failover)
 * 3. Structured logging for debugging
 */
interface SolanaRpcProvider {
    val name: String
    val url: String

    /**
     * Get signatures for a reference address.
     * Returns (signature, blockTimeMs) pairs.
     */
    suspend fun getSignaturesForAddress(
        address: String,
        limit: Int = 10
    ): RpcCallResult<List<SignatureInfo>>

    /**
     * Get full transaction data for validation.
     */
    suspend fun getTransaction(signature: String): RpcCallResult<JSONObject?>

    /**
     * Check if transaction is confirmed.
     */
    suspend fun getSignatureStatus(signature: String): RpcCallResult<ConfirmationStatus>

    /**
     * Get token accounts by owner for a specific mint.
     * Used for fallback detection when ATA derivation fails.
     *
     * @param owner Owner wallet address
     * @param mint Token mint address (null for all SPL tokens)
     * @return List of discovered token accounts
     */
    suspend fun getTokenAccountsByOwner(
        owner: String,
        mint: String?
    ): RpcCallResult<List<DiscoveredTokenAccount>>
}

/**
 * Signature info from getSignaturesForAddress.
 */
data class SignatureInfo(
    val signature: String,
    val blockTimeMs: Long,  // Converted to milliseconds
    val hasError: Boolean
)

/**
 * Discovered token account from getTokenAccountsByOwner.
 * Used for fallback detection strategy.
 */
data class DiscoveredTokenAccount(
    val address: String,      // Token account address (ATA or non-standard)
    val mint: String,         // Token mint address
    val owner: String,        // Owner wallet address
    val balance: Long         // Balance in minor units
)

/**
 * Transaction confirmation status.
 */
enum class ConfirmationStatus {
    NOT_FOUND,
    PROCESSING,
    CONFIRMED,
    FINALIZED
}

/**
 * Result of an RPC call - NEVER throws.
 */
sealed class RpcCallResult<out T> {
    data class Success<T>(val data: T, val providerName: String) : RpcCallResult<T>()
    data class Failure(
        val errorType: RpcErrorType,
        val httpCode: Int? = null,
        val message: String,
        val providerName: String
    ) : RpcCallResult<Nothing>()

    fun isSuccess() = this is Success
    fun isFailure() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

/**
 * Categorized RPC error types for better handling.
 */
enum class RpcErrorType {
    TIMEOUT,           // Connection or read timeout
    NETWORK_ERROR,     // DNS, connection refused, etc.
    HTTP_ERROR,        // Non-200 response
    RPC_ERROR,         // JSON-RPC error in response
    PARSE_ERROR,       // Invalid JSON response
    UNKNOWN
}

/**
 * Standard Solana RPC provider implementation.
 */
class StandardRpcProvider(
    override val name: String,
    override val url: String,
    private val timeoutMs: Int = 3000  // 3 seconds for fast failover
) : SolanaRpcProvider {

    companion object {
        private const val TAG = "RpcProvider"
    }

    override suspend fun getSignaturesForAddress(
        address: String,
        limit: Int
    ): RpcCallResult<List<SignatureInfo>> {
        return try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getSignaturesForAddress")
                put("params", JSONArray().apply {
                    put(address)
                    put(JSONObject().apply {
                        put("limit", limit)
                    })
                })
            }

            val response = executeRpc(body)

            when (response) {
                is RpcCallResult.Failure -> response
                is RpcCallResult.Success -> {
                    val json = response.data
                    if (json.has("error")) {
                        val error = json.getJSONObject("error")
                        RpcCallResult.Failure(
                            errorType = RpcErrorType.RPC_ERROR,
                            message = error.optString("message", "Unknown RPC error"),
                            providerName = name
                        )
                    } else {
                        val result = json.optJSONArray("result") ?: JSONArray()
                        val signatures = mutableListOf<SignatureInfo>()

                        for (i in 0 until result.length()) {
                            val item = result.getJSONObject(i)
                            val sig = item.getString("signature")
                            val blockTime = item.optLong("blockTime", 0L) * 1000 // to ms
                            val err = item.opt("err")
                            val hasError = err != null && err != JSONObject.NULL

                            if (!hasError) {
                                signatures.add(SignatureInfo(sig, blockTime, hasError))
                            }
                        }

                        RpcCallResult.Success(signatures, name)
                    }
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun getTransaction(signature: String): RpcCallResult<JSONObject?> {
        return try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getTransaction")
                put("params", JSONArray().apply {
                    put(signature)
                    put(JSONObject().apply {
                        put("encoding", "jsonParsed")
                        put("maxSupportedTransactionVersion", 0)
                    })
                })
            }

            val response = executeRpc(body)

            when (response) {
                is RpcCallResult.Failure -> response
                is RpcCallResult.Success -> {
                    val json = response.data
                    if (json.has("error")) {
                        val error = json.getJSONObject("error")
                        RpcCallResult.Failure(
                            errorType = RpcErrorType.RPC_ERROR,
                            message = error.optString("message", "Unknown RPC error"),
                            providerName = name
                        )
                    } else {
                        val result = json.optJSONObject("result")
                        RpcCallResult.Success(result, name)
                    }
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun getSignatureStatus(signature: String): RpcCallResult<ConfirmationStatus> {
        return try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getSignatureStatuses")
                put("params", JSONArray().apply {
                    put(JSONArray().apply { put(signature) })
                    put(JSONObject().apply {
                        put("searchTransactionHistory", true)
                    })
                })
            }

            val response = executeRpc(body)

            when (response) {
                is RpcCallResult.Failure -> response
                is RpcCallResult.Success -> {
                    val json = response.data
                    if (json.has("error")) {
                        val error = json.getJSONObject("error")
                        RpcCallResult.Failure(
                            errorType = RpcErrorType.RPC_ERROR,
                            message = error.optString("message", "Unknown RPC error"),
                            providerName = name
                        )
                    } else {
                        val result = json.optJSONObject("result")
                        val value = result?.optJSONArray("value")

                        if (value == null || value.length() == 0 || value.isNull(0)) {
                            RpcCallResult.Success(ConfirmationStatus.NOT_FOUND, name)
                        } else {
                            val status = value.getJSONObject(0)
                            val confirmationStatus = status.optString("confirmationStatus", "")

                            val level = when (confirmationStatus) {
                                "finalized" -> ConfirmationStatus.FINALIZED
                                "confirmed" -> ConfirmationStatus.CONFIRMED
                                "processed" -> ConfirmationStatus.PROCESSING
                                else -> ConfirmationStatus.PROCESSING
                            }
                            RpcCallResult.Success(level, name)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun getTokenAccountsByOwner(
        owner: String,
        mint: String?
    ): RpcCallResult<List<DiscoveredTokenAccount>> {
        return try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getTokenAccountsByOwner")
                put("params", JSONArray().apply {
                    put(owner)
                    put(JSONObject().apply {
                        if (mint != null) {
                            // Filter by specific mint
                            put("mint", mint)
                        } else {
                            // Get all SPL token accounts
                            put("programId", "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
                        }
                    })
                    put(JSONObject().apply {
                        put("encoding", "jsonParsed")
                    })
                })
            }

            val response = executeRpc(body)

            when (response) {
                is RpcCallResult.Failure -> response
                is RpcCallResult.Success -> {
                    val json = response.data
                    if (json.has("error")) {
                        val error = json.getJSONObject("error")
                        RpcCallResult.Failure(
                            errorType = RpcErrorType.RPC_ERROR,
                            message = error.optString("message", "Unknown RPC error"),
                            providerName = name
                        )
                    } else {
                        val result = json.optJSONObject("result")
                        val value = result?.optJSONArray("value") ?: JSONArray()
                        val accounts = mutableListOf<DiscoveredTokenAccount>()

                        for (i in 0 until value.length()) {
                            try {
                                val item = value.getJSONObject(i)
                                val pubkey = item.getString("pubkey")
                                val accountData = item.getJSONObject("account")
                                    .getJSONObject("data")
                                    .getJSONObject("parsed")
                                    .getJSONObject("info")

                                val tokenMint = accountData.getString("mint")
                                val tokenOwner = accountData.getString("owner")
                                val tokenAmount = accountData.getJSONObject("tokenAmount")
                                val amount = tokenAmount.getString("amount").toLongOrNull() ?: 0L

                                accounts.add(DiscoveredTokenAccount(
                                    address = pubkey,
                                    mint = tokenMint,
                                    owner = tokenOwner,
                                    balance = amount
                                ))
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse token account at index $i: ${e.message}")
                            }
                        }

                        RpcCallResult.Success(accounts, name)
                    }
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    private fun executeRpc(body: JSONObject): RpcCallResult<JSONObject> {
        val urlObj = URL(url)
        val connection = urlObj.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
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
                return RpcCallResult.Failure(
                    errorType = RpcErrorType.HTTP_ERROR,
                    httpCode = responseCode,
                    message = "HTTP $responseCode: ${responseText.take(100)}",
                    providerName = name
                )
            }

            RpcCallResult.Success(JSONObject(responseText), name)
        } catch (e: SocketTimeoutException) {
            RpcCallResult.Failure(
                errorType = RpcErrorType.TIMEOUT,
                message = "Timeout after ${timeoutMs}ms",
                providerName = name
            )
        } catch (e: Exception) {
            categorizeException(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun <T> categorizeException(e: Exception): RpcCallResult<T> {
        val errorType = when {
            e is SocketTimeoutException -> RpcErrorType.TIMEOUT
            e.message?.contains("timeout", ignoreCase = true) == true -> RpcErrorType.TIMEOUT
            e.message?.contains("refused", ignoreCase = true) == true -> RpcErrorType.NETWORK_ERROR
            e.message?.contains("DNS", ignoreCase = true) == true -> RpcErrorType.NETWORK_ERROR
            e.message?.contains("network", ignoreCase = true) == true -> RpcErrorType.NETWORK_ERROR
            e is org.json.JSONException -> RpcErrorType.PARSE_ERROR
            else -> RpcErrorType.UNKNOWN
        }

        return RpcCallResult.Failure(
            errorType = errorType,
            message = "${e.javaClass.simpleName}: ${e.message}",
            providerName = name
        )
    }
}
