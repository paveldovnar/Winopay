package com.winopay.tron.rpc

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * TRON RPC provider interface for multi-provider failover.
 *
 * DESIGN PRINCIPLES:
 * 1. NO crashes - all errors captured in TronRpcResult
 * 2. 3-second timeout per call (fast failover)
 * 3. Structured logging for debugging
 *
 * TRON API:
 * - Uses HTTP JSON-RPC (similar to Ethereum)
 * - TronGrid is primary provider
 * - Backup providers: Tron public nodes
 */
interface TronRpcProvider {
    val name: String
    val baseUrl: String

    /**
     * Get TRC20 transfer transactions for an address.
     * Uses TronGrid's /v1/accounts/{address}/transactions/trc20 endpoint.
     *
     * @param address Wallet address (base58check)
     * @param contractAddress TRC20 contract to filter (null for all)
     * @param limit Max transactions to return
     * @param minTimestamp Only return transactions after this timestamp (ms)
     * @param fingerprint Pagination cursor for next page (from previous response)
     * @return Paginated list of TRC20 transactions
     */
    suspend fun getTrc20Transactions(
        address: String,
        contractAddress: String?,
        limit: Int = 20,
        minTimestamp: Long? = null,
        fingerprint: String? = null
    ): TronRpcResult<Trc20TransactionsPage>

    /**
     * Get transaction by ID (hash).
     *
     * @param txId Transaction hash
     * @return Transaction details or null if not found
     */
    suspend fun getTransactionById(txId: String): TronRpcResult<TronTransaction?>

    /**
     * Get transaction info (confirmation status, fee, etc.).
     *
     * @param txId Transaction hash
     * @return Transaction info or null if not found
     */
    suspend fun getTransactionInfo(txId: String): TronRpcResult<TronTransactionInfo?>

    /**
     * Create unsigned TRC20 transfer transaction.
     * Uses triggerSmartContract to build the transaction.
     *
     * @param ownerAddressHex Sender address (hex with 41 prefix)
     * @param contractAddressHex Token contract address (hex with 41 prefix)
     * @param functionSelector Method selector (e.g., "transfer(address,uint256)")
     * @param parameter ABI-encoded parameters (hex string)
     * @param feeLimit Maximum fee in SUN
     * @return Unsigned transaction data
     */
    suspend fun createTrc20Transaction(
        ownerAddressHex: String,
        contractAddressHex: String,
        functionSelector: String,
        parameter: String,
        feeLimit: Long
    ): TronRpcResult<UnsignedTronTransaction>

    /**
     * Broadcast a signed transaction to the network.
     *
     * @param signedTxHex Signed transaction in hex format
     * @return Transaction ID (hash) or error
     */
    suspend fun broadcastTransaction(signedTxHex: String): TronRpcResult<String>
}

/**
 * TRC20 transfer transaction from TronGrid API.
 */
data class Trc20Transaction(
    /** Transaction hash */
    val txId: String,
    /** From address (base58check) */
    val from: String,
    /** To address (base58check) */
    val to: String,
    /** Transfer amount (raw, use decimals to convert) */
    val value: String,
    /** Token contract address */
    val contractAddress: String,
    /** Token symbol (e.g., "USDT") */
    val tokenSymbol: String,
    /** Token decimals */
    val tokenDecimals: Int,
    /** Block timestamp in milliseconds */
    val blockTimestamp: Long,
    /** Transaction type (e.g., "Transfer") */
    val type: String
)

/**
 * Paginated response from TRC20 transactions endpoint.
 */
data class Trc20TransactionsPage(
    /** Transactions in this page */
    val transactions: List<Trc20Transaction>,
    /** Pagination cursor for next page (null if no more pages) */
    val fingerprint: String?,
    /** Provider that returned this data */
    val providerName: String
)

/**
 * Full TRON transaction details.
 */
data class TronTransaction(
    val txId: String,
    val blockNumber: Long,
    val blockTimestamp: Long,
    val contractType: String, // "TransferContract", "TriggerSmartContract"
    val ownerAddress: String,
    val toAddress: String?,
    val contractAddress: String?,
    val contractData: String?, // For smart contract calls
    val result: String // "SUCCESS" or error
)

/**
 * TRON transaction info (for confirmation status).
 */
data class TronTransactionInfo(
    val txId: String,
    val blockNumber: Long,
    val blockTimestamp: Long,
    /** Receipt status: "SUCCESS" or error */
    val result: String,
    /** Fee in SUN (1 TRX = 1,000,000 SUN) */
    val fee: Long,
    /** Number of confirmations (estimated from block) */
    val confirmations: Int
)

/**
 * Unsigned TRON transaction ready for signing.
 */
data class UnsignedTronTransaction(
    /** Transaction ID (hash of raw_data) */
    val txId: String,
    /** Raw transaction data for signing (hex) */
    val rawDataHex: String,
    /** Full transaction JSON for wallet signing */
    val transactionJson: String
)

/**
 * Result of a TRON RPC call - NEVER throws.
 */
sealed class TronRpcResult<out T> {
    data class Success<T>(val data: T, val providerName: String) : TronRpcResult<T>()
    data class Failure(
        val errorType: TronRpcErrorType,
        val httpCode: Int? = null,
        val message: String,
        val providerName: String
    ) : TronRpcResult<Nothing>()

    fun isSuccess() = this is Success
    fun isFailure() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

/**
 * Categorized TRON RPC error types.
 */
enum class TronRpcErrorType {
    TIMEOUT,           // Connection or read timeout
    NETWORK_ERROR,     // DNS, connection refused, etc.
    HTTP_ERROR,        // Non-200 response
    API_ERROR,         // TRON API error in response
    PARSE_ERROR,       // Invalid JSON response
    RATE_LIMITED,      // 429 Too Many Requests
    UNKNOWN
}

/**
 * Standard TRON RPC provider implementation using TronGrid API.
 *
 * TronGrid Endpoints:
 * - Mainnet: https://api.trongrid.io
 * - Nile: https://nile.trongrid.io
 * - Shasta: https://api.shasta.trongrid.io
 *
 * API Documentation: https://developers.tron.network/reference
 */
class StandardTronRpcProvider(
    override val name: String,
    override val baseUrl: String,
    private val apiKey: String? = null,
    private val timeoutMs: Int = 3000
) : TronRpcProvider {

    companion object {
        private const val TAG = "TronRpcProvider"
    }

    override suspend fun getTrc20Transactions(
        address: String,
        contractAddress: String?,
        limit: Int,
        minTimestamp: Long?,
        fingerprint: String?
    ): TronRpcResult<Trc20TransactionsPage> {
        return try {
            // Build URL: /v1/accounts/{address}/transactions/trc20
            val urlBuilder = StringBuilder("$baseUrl/v1/accounts/$address/transactions/trc20")
            urlBuilder.append("?limit=$limit")
            if (contractAddress != null) {
                urlBuilder.append("&contract_address=$contractAddress")
            }
            if (minTimestamp != null) {
                urlBuilder.append("&min_timestamp=$minTimestamp")
            }
            if (fingerprint != null) {
                urlBuilder.append("&fingerprint=$fingerprint")
            }
            urlBuilder.append("&only_to=true") // Only incoming transfers

            val response = executeGet(urlBuilder.toString())

            when (response) {
                is TronRpcResult.Failure -> response
                is TronRpcResult.Success -> {
                    val json = response.data

                    // Check for API error
                    if (!json.optBoolean("success", true)) {
                        val error = json.optString("error", "Unknown API error")
                        return TronRpcResult.Failure(
                            errorType = TronRpcErrorType.API_ERROR,
                            message = error,
                            providerName = name
                        )
                    }

                    val data = json.optJSONArray("data") ?: JSONArray()
                    val transactions = mutableListOf<Trc20Transaction>()

                    for (i in 0 until data.length()) {
                        try {
                            val tx = data.getJSONObject(i)
                            val tokenInfo = tx.optJSONObject("token_info")

                            transactions.add(Trc20Transaction(
                                txId = tx.getString("transaction_id"),
                                from = tx.getString("from"),
                                to = tx.getString("to"),
                                value = tx.getString("value"),
                                contractAddress = tokenInfo?.optString("address") ?: "",
                                tokenSymbol = tokenInfo?.optString("symbol") ?: "TRC20",
                                tokenDecimals = tokenInfo?.optInt("decimals", 6) ?: 6,
                                blockTimestamp = tx.getLong("block_timestamp"),
                                type = tx.optString("type", "Transfer")
                            ))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to parse TRC20 tx at index $i: ${e.message}")
                        }
                    }

                    // Extract pagination cursor from meta
                    val meta = json.optJSONObject("meta")
                    val nextFingerprint = meta?.optString("fingerprint")?.takeIf { it.isNotBlank() }

                    TronRpcResult.Success(
                        Trc20TransactionsPage(
                            transactions = transactions,
                            fingerprint = nextFingerprint,
                            providerName = name
                        ),
                        name
                    )
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun getTransactionById(txId: String): TronRpcResult<TronTransaction?> {
        return try {
            // TronGrid: POST /wallet/gettransactionbyid
            val body = JSONObject().apply {
                put("value", txId)
            }

            val response = executePost("$baseUrl/wallet/gettransactionbyid", body)

            when (response) {
                is TronRpcResult.Failure -> response
                is TronRpcResult.Success -> {
                    val json = response.data

                    // Empty response means not found
                    if (!json.has("txID") && !json.has("raw_data")) {
                        return TronRpcResult.Success(null, name)
                    }

                    val rawData = json.optJSONObject("raw_data")
                    val contract = rawData?.optJSONArray("contract")?.optJSONObject(0)
                    val parameter = contract?.optJSONObject("parameter")?.optJSONObject("value")

                    val tx = TronTransaction(
                        txId = json.optString("txID", txId),
                        blockNumber = rawData?.optLong("ref_block_num") ?: 0L,
                        blockTimestamp = rawData?.optLong("timestamp") ?: 0L,
                        contractType = contract?.optString("type") ?: "Unknown",
                        ownerAddress = parameter?.optString("owner_address") ?: "",
                        toAddress = parameter?.optString("to_address"),
                        contractAddress = parameter?.optString("contract_address"),
                        contractData = parameter?.optString("data"),
                        result = if (json.has("ret")) {
                            json.optJSONArray("ret")?.optJSONObject(0)?.optString("contractRet") ?: "UNKNOWN"
                        } else "PENDING"
                    )

                    TronRpcResult.Success(tx, name)
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun getTransactionInfo(txId: String): TronRpcResult<TronTransactionInfo?> {
        return try {
            val body = JSONObject().apply {
                put("value", txId)
            }

            val response = executePost("$baseUrl/wallet/gettransactioninfobyid", body)

            when (response) {
                is TronRpcResult.Failure -> response
                is TronRpcResult.Success -> {
                    val json = response.data

                    // Empty response means not found
                    if (!json.has("id") && !json.has("blockNumber")) {
                        return TronRpcResult.Success(null, name)
                    }

                    // Estimate confirmations (TRON produces blocks every 3 seconds)
                    // For real confirmation count, would need current block height
                    val blockNumber = json.optLong("blockNumber", 0)
                    val confirmations = if (blockNumber > 0) 1 else 0

                    val info = TronTransactionInfo(
                        txId = json.optString("id", txId),
                        blockNumber = blockNumber,
                        blockTimestamp = json.optLong("blockTimeStamp", 0),
                        result = json.optString("receipt")
                            ?.let { JSONObject(it).optString("result") }
                            ?: if (json.has("contractResult")) "SUCCESS" else "PENDING",
                        fee = json.optLong("fee", 0),
                        confirmations = confirmations
                    )

                    TronRpcResult.Success(info, name)
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun createTrc20Transaction(
        ownerAddressHex: String,
        contractAddressHex: String,
        functionSelector: String,
        parameter: String,
        feeLimit: Long
    ): TronRpcResult<UnsignedTronTransaction> {
        return try {
            // TronGrid: POST /wallet/triggersmartcontract
            val body = JSONObject().apply {
                put("owner_address", ownerAddressHex)
                put("contract_address", contractAddressHex)
                put("function_selector", functionSelector)
                put("parameter", parameter)
                put("fee_limit", feeLimit)
                put("call_value", 0) // No TRX sent with TRC20 transfer
            }

            Log.d(TAG, "CREATE_TX|ownerHex=${ownerAddressHex.take(12)}...|contract=${contractAddressHex.take(12)}...")

            val response = executePost("$baseUrl/wallet/triggersmartcontract", body)

            when (response) {
                is TronRpcResult.Failure -> response
                is TronRpcResult.Success -> {
                    val json = response.data

                    // Check for API error
                    val result = json.optJSONObject("result")
                    if (result != null && !result.optBoolean("result", true)) {
                        val message = result.optString("message", "Unknown error")
                        // Decode hex message if present
                        val decodedMessage = try {
                            String(message.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
                        } catch (e: Exception) {
                            message
                        }
                        return TronRpcResult.Failure(
                            errorType = TronRpcErrorType.API_ERROR,
                            message = decodedMessage,
                            providerName = name
                        )
                    }

                    val transaction = json.optJSONObject("transaction")
                        ?: return TronRpcResult.Failure(
                            errorType = TronRpcErrorType.API_ERROR,
                            message = "No transaction in response",
                            providerName = name
                        )

                    val txId = transaction.optString("txID")
                    val rawDataHex = transaction.optJSONObject("raw_data_hex")?.toString()
                        ?: transaction.optString("raw_data_hex", "")

                    if (txId.isBlank()) {
                        return TronRpcResult.Failure(
                            errorType = TronRpcErrorType.API_ERROR,
                            message = "No txID in response",
                            providerName = name
                        )
                    }

                    Log.d(TAG, "CREATE_TX|SUCCESS|txId=${txId.take(16)}...")

                    TronRpcResult.Success(
                        UnsignedTronTransaction(
                            txId = txId,
                            rawDataHex = rawDataHex,
                            transactionJson = transaction.toString()
                        ),
                        name
                    )
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    override suspend fun broadcastTransaction(signedTxHex: String): TronRpcResult<String> {
        return try {
            // Parse signed transaction JSON
            val signedTx = try {
                JSONObject(signedTxHex)
            } catch (e: Exception) {
                // If not JSON, assume it's hex-encoded raw transaction
                // Build minimal transaction structure
                JSONObject().apply {
                    put("raw_data_hex", signedTxHex)
                }
            }

            Log.d(TAG, "BROADCAST|txId=${signedTx.optString("txID", "").take(16)}...")

            val response = executePost("$baseUrl/wallet/broadcasttransaction", signedTx)

            when (response) {
                is TronRpcResult.Failure -> response
                is TronRpcResult.Success -> {
                    val json = response.data

                    val success = json.optBoolean("result", false)
                    if (!success) {
                        val code = json.optString("code", "UNKNOWN")
                        val message = json.optString("message", "Broadcast failed")
                        // Decode hex message if present
                        val decodedMessage = try {
                            String(message.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
                        } catch (e: Exception) {
                            message
                        }
                        return TronRpcResult.Failure(
                            errorType = TronRpcErrorType.API_ERROR,
                            message = "$code: $decodedMessage",
                            providerName = name
                        )
                    }

                    val txId = json.optString("txid", signedTx.optString("txID", ""))

                    Log.i(TAG, "BROADCAST|SUCCESS|txId=${txId.take(16)}...")
                    TronRpcResult.Success(txId, name)
                }
            }
        } catch (e: Exception) {
            categorizeException(e)
        }
    }

    private fun executeGet(url: String): TronRpcResult<JSONObject> {
        val urlObj = URL(url)
        val connection = urlObj.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            if (apiKey != null) {
                connection.setRequestProperty("TRON-PRO-API-KEY", apiKey)
            }
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs

            val responseCode = connection.responseCode
            val responseText = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }

            if (responseCode == 429) {
                return TronRpcResult.Failure(
                    errorType = TronRpcErrorType.RATE_LIMITED,
                    httpCode = responseCode,
                    message = "Rate limited",
                    providerName = name
                )
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return TronRpcResult.Failure(
                    errorType = TronRpcErrorType.HTTP_ERROR,
                    httpCode = responseCode,
                    message = "HTTP $responseCode: ${responseText.take(100)}",
                    providerName = name
                )
            }

            TronRpcResult.Success(JSONObject(responseText), name)
        } catch (e: SocketTimeoutException) {
            TronRpcResult.Failure(
                errorType = TronRpcErrorType.TIMEOUT,
                message = "Timeout after ${timeoutMs}ms",
                providerName = name
            )
        } catch (e: Exception) {
            categorizeException(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun executePost(url: String, body: JSONObject): TronRpcResult<JSONObject> {
        val urlObj = URL(url)
        val connection = urlObj.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (apiKey != null) {
                connection.setRequestProperty("TRON-PRO-API-KEY", apiKey)
            }
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

            if (responseCode == 429) {
                return TronRpcResult.Failure(
                    errorType = TronRpcErrorType.RATE_LIMITED,
                    httpCode = responseCode,
                    message = "Rate limited",
                    providerName = name
                )
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return TronRpcResult.Failure(
                    errorType = TronRpcErrorType.HTTP_ERROR,
                    httpCode = responseCode,
                    message = "HTTP $responseCode: ${responseText.take(100)}",
                    providerName = name
                )
            }

            TronRpcResult.Success(JSONObject(responseText), name)
        } catch (e: SocketTimeoutException) {
            TronRpcResult.Failure(
                errorType = TronRpcErrorType.TIMEOUT,
                message = "Timeout after ${timeoutMs}ms",
                providerName = name
            )
        } catch (e: Exception) {
            categorizeException(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun <T> categorizeException(e: Exception): TronRpcResult<T> {
        val errorType = when {
            e is SocketTimeoutException -> TronRpcErrorType.TIMEOUT
            e.message?.contains("timeout", ignoreCase = true) == true -> TronRpcErrorType.TIMEOUT
            e.message?.contains("refused", ignoreCase = true) == true -> TronRpcErrorType.NETWORK_ERROR
            e.message?.contains("DNS", ignoreCase = true) == true -> TronRpcErrorType.NETWORK_ERROR
            e.message?.contains("network", ignoreCase = true) == true -> TronRpcErrorType.NETWORK_ERROR
            e is org.json.JSONException -> TronRpcErrorType.PARSE_ERROR
            else -> TronRpcErrorType.UNKNOWN
        }

        return TronRpcResult.Failure(
            errorType = errorType,
            message = "${e.javaClass.simpleName}: ${e.message}",
            providerName = name
        )
    }
}
