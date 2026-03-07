package com.winopay.solana.pay

import android.util.Log
import com.winopay.BuildConfig
import com.winopay.data.local.PaymentCurrency
import com.winopay.payments.SolanaTokenPolicy
import com.winopay.payments.TokenPolicyProvider
import com.winopay.solana.rpc.ConfirmationStatus
import com.winopay.solana.rpc.RpcCallResult
import com.winopay.solana.rpc.RpcProviderManager
import com.winopay.solana.rpc.SignatureInfo
import com.winopay.solana.rpc.SolanaRpcProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * STRICT transaction validation for Solana Pay payments.
 *
 * AMOUNT-ONLY DETECTION (no reference required):
 * Many wallets do NOT include reference/memo from Solana Pay QR codes.
 * We detect payments by polling the MERCHANT ADDRESS and validating:
 * 1. Recipient MUST receive tokens/SOL (found in balance changes)
 * 2. Mint MUST match expected mint (for SPL)
 * 3. Amount received MUST be >= expected amount (merchant protection)
 * 4. Transaction MUST be after invoice creation (blockTime >= createdAt)
 * 5. NEVER return Valid by default - always require proof
 *
 * SINGLE-ACTIVE-INVOICE INVARIANT:
 * Since we no longer use reference for uniqueness, only ONE invoice
 * can be active at a time. This is enforced at the DB/PosManager level.
 *
 * MULTI-RPC FAILOVER:
 * Uses RpcProviderManager for production-grade reliability.
 * If primary RPC is blocked, automatically fails over to backups.
 *
 * Used by both foreground detector and background worker (single source of truth).
 */
object TransactionValidator {

    private const val TAG = "TransactionValidator"

    /**
     * Validation result with detailed reason.
     *
     * For Valid results with SPL tokens, includes:
     * - actualMintUsed: The mint that was actually paid (may differ from expected for stablecoins)
     * - warningCode: Warning if paid with different stablecoin (USDT instead of USDC or vice versa)
     * - payerAddress: Wallet address of the payer (owner for SPL transfers)
     * - payerTokenAccount: Token account that sent the payment (source ATA for SPL)
     */
    sealed class Result {
        data class Valid(
            val actualMintUsed: String? = null,
            val warningCode: String? = null,
            val payerAddress: String? = null,
            val payerTokenAccount: String? = null
        ) : Result()
        data class Invalid(val reason: String) : Result()
        data class Error(val message: String) : Result()

        fun isValid() = this is Valid
    }

    /**
     * Parameters for validation.
     *
     * AMOUNT-ONLY DETECTION:
     * - No reference required (wallets often omit it)
     * - Validates: recipient, mint, amount, blockTime
     * - Single-active-invoice ensures uniqueness
     */
    data class ValidationParams(
        val signature: String,
        val expectedRecipient: String,
        val expectedAmountMinor: Long,
        val expectedCurrency: PaymentCurrency,
        val expectedMint: String?,
        val invoiceNetworkId: String,
        val invoiceCreatedAt: Long,
        /** Provider that found this signature (for cross-call consistency) */
        val preferredProvider: SolanaRpcProvider? = null
    )

    /**
     * Validate a transaction strictly (AMOUNT-ONLY detection).
     *
     * Returns Valid ONLY if ALL conditions are met:
     * - Recipient received the expected token/SOL
     * - Amount received >= expected amount
     * - Mint matches (for SPL transfers)
     * - Transaction blockTime >= invoiceCreatedAt
     *
     * NOTE: Reference is NOT checked - many wallets omit it.
     * Single-active-invoice rule ensures payment uniqueness.
     *
     * Uses RpcProviderManager for multi-RPC failover.
     */
    fun validateTransaction(params: ValidationParams): Result {
        Log.d(TAG, "━━━━━ AMOUNT-ONLY VALIDATION START ━━━━━")
        Log.d(TAG, "  Signature: ${params.signature}")
        Log.d(TAG, "  Expected recipient: ${params.expectedRecipient}")
        Log.d(TAG, "  Expected mint: ${params.expectedMint ?: "SOL (native)"}")
        Log.d(TAG, "  Expected amount (minor): ${params.expectedAmountMinor}")
        Log.d(TAG, "  Invoice networkId: ${params.invoiceNetworkId}")
        Log.d(TAG, "  Invoice created at: ${params.invoiceCreatedAt}")
        Log.d(TAG, "  Preferred provider: ${params.preferredProvider?.name ?: "none"}")

        return try {
            // Fetch transaction using RpcProviderManager with failover
            val txResult = runBlocking {
                RpcProviderManager.getTransaction(params.signature, params.preferredProvider)
            }

            val result = when (txResult) {
                is RpcCallResult.Failure -> {
                    Log.e(TAG, "  ✗ FAIL: RPC error - ${txResult.errorType}: ${txResult.message}")
                    return Result.Error("RPC error: ${txResult.message}")
                }
                is RpcCallResult.Success -> txResult.data
            }

            if (result == null) {
                Log.e(TAG, "  ✗ FAIL: Transaction not found")
                return Result.Invalid("Transaction not found")
            }
            val validationResult = validateTransactionJson(result, params)

            when (validationResult) {
                is Result.Valid -> Log.d(TAG, "  ✓ VALIDATION PASSED")
                is Result.Invalid -> Log.e(TAG, "  ✗ VALIDATION FAILED: ${validationResult.reason}")
                is Result.Error -> Log.e(TAG, "  ✗ VALIDATION ERROR: ${validationResult.message}")
            }
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            validationResult
        } catch (e: Exception) {
            Log.e(TAG, "  ✗ EXCEPTION: ${e.message}", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    internal fun validateTransactionJson(
        txJson: JSONObject,
        params: ValidationParams
    ): Result {
        if (params.invoiceCreatedAt <= 0L) {
            Log.e(TAG, "  ✗ FAIL: Invalid invoiceCreatedAt: ${params.invoiceCreatedAt}")
            return Result.Invalid("Invalid invoice creation time")
        }

        val txNetworkId = RpcProviderManager.getCluster()
        if (txNetworkId != params.invoiceNetworkId) {
            Log.e(TAG, "  ✗ FAIL: Network mismatch (txNetworkId=$txNetworkId != invoiceNetworkId=${params.invoiceNetworkId})")
            return Result.Invalid("Network mismatch")
        }

        // Check if transaction failed on-chain
        val meta = txJson.optJSONObject("meta")
        if (meta?.opt("err") != null && meta.opt("err") != JSONObject.NULL) {
            Log.e(TAG, "  ✗ FAIL: Transaction failed on-chain: ${meta.opt("err")}")
            return Result.Invalid("Transaction failed on-chain")
        }

        // Check block time vs invoice creation
        val blockTime = txJson.optLong("blockTime", 0L) * 1000 // Convert to milliseconds
        if (blockTime <= 0L) {
            Log.e(TAG, "  ✗ FAIL: Missing/invalid blockTime")
            return Result.Invalid("Missing blockTime")
        }
        if (blockTime < params.invoiceCreatedAt) {
            Log.e(TAG, "  ✗ FAIL: Transaction predates invoice (blockTime=$blockTime < invoiceCreatedAt=${params.invoiceCreatedAt})")
            return Result.Invalid("Transaction predates invoice creation")
        }
        Log.d(TAG, "  ✓ Block time check passed: $blockTime >= ${params.invoiceCreatedAt}")

        val transaction = txJson.optJSONObject("transaction")
        val message = transaction?.optJSONObject("message")
        val accountKeys = message?.optJSONArray("accountKeys")

        return if (params.expectedMint != null) {
            validateSplTransferStrict(
                meta = meta,
                expectedRecipient = params.expectedRecipient,
                expectedMint = params.expectedMint,
                expectedAmountMinor = params.expectedAmountMinor,
                accountKeys = accountKeys
            )
        } else {
            validateSolTransferStrict(
                message = message,
                meta = meta,
                expectedRecipient = params.expectedRecipient,
                expectedAmountMinor = params.expectedAmountMinor
            )
        }
    }

    /**
     * STRICT SPL token transfer validation with DUAL STABLECOIN support.
     *
     * Requirements:
     * 1. Find recipient in post-token balances with ALLOWED mint (USDC/USDT interchangeable)
     * 2. Calculate balance increase (post - pre)
     * 3. Balance increase MUST be >= expected amount
     *
     * DUAL STABLECOIN ACCEPTANCE:
     * - If expectedMint is USDC or USDT, BOTH are accepted at 1:1
     * - Returns actualMintUsed and warningCode if different stablecoin used
     * - For non-stablecoin tokens, strict mint matching applies
     *
     * PAYER EXTRACTION:
     * - Identifies sender by finding account with balance DECREASE for same mint
     * - Returns payerAddress (owner wallet) and payerTokenAccount (source ATA)
     */
    private fun validateSplTransferStrict(
        meta: JSONObject?,
        expectedRecipient: String,
        expectedMint: String,
        expectedAmountMinor: Long,
        accountKeys: JSONArray?
    ): Result {
        if (meta == null) {
            return Result.Invalid("Transaction metadata missing")
        }

        // Get allowed mints for this invoice (USDC+USDT for stablecoins, or strict single mint)
        val allowedMints = SolanaTokenPolicy.getAllowedTokens(expectedMint)
        Log.d(TAG, "  SPL validation: expectedMint=$expectedMint, allowedMints=$allowedMints")

        val preBalances = meta.optJSONArray("preTokenBalances") ?: JSONArray()
        val postBalances = meta.optJSONArray("postTokenBalances") ?: JSONArray()

        Log.d(TAG, "  SPL validation: preBalances=${preBalances.length()}, postBalances=${postBalances.length()}")

        // Build map of accountIndex -> (owner, mint, preAmount, postAmount)
        data class TokenBalance(
            val owner: String,
            val mint: String,
            val accountIndex: Int,
            var preAmount: Long = 0L,
            var postAmount: Long = 0L
        )

        val balanceMap = mutableMapOf<Int, TokenBalance>()

        // Process pre-balances
        for (i in 0 until preBalances.length()) {
            val balance = preBalances.getJSONObject(i)
            val mint = balance.optString("mint", "")
            val owner = extractOwner(balance)
            val accountIndex = balance.optInt("accountIndex", -1)
            val amount = extractTokenAmount(balance)

            if (accountIndex >= 0) {
                balanceMap[accountIndex] = TokenBalance(owner, mint, accountIndex, preAmount = amount)
                Log.d(TAG, "    Pre[$accountIndex]: owner=$owner, mint=$mint, amount=$amount")
            }
        }

        // Process post-balances
        for (i in 0 until postBalances.length()) {
            val balance = postBalances.getJSONObject(i)
            val mint = balance.optString("mint", "")
            val owner = extractOwner(balance)
            val accountIndex = balance.optInt("accountIndex", -1)
            val amount = extractTokenAmount(balance)

            if (accountIndex >= 0) {
                val existing = balanceMap[accountIndex]
                if (existing != null) {
                    balanceMap[accountIndex] = existing.copy(postAmount = amount)
                } else {
                    // New account (ATA created in this tx)
                    balanceMap[accountIndex] = TokenBalance(owner, mint, accountIndex, preAmount = 0L, postAmount = amount)
                }
                Log.d(TAG, "    Post[$accountIndex]: owner=$owner, mint=$mint, amount=$amount")
            }
        }

        // Find recipient's balance increase for any ALLOWED mint
        var recipientFound = false
        var amountReceived = 0L
        var mintMatched = false
        var actualMintUsed: String? = null

        // Payer extraction: find account with balance DECREASE
        var payerAddress: String? = null
        var payerTokenAccount: String? = null

        for ((_, balance) in balanceMap) {
            // DUAL STABLECOIN: Check if mint is in allowedMints set (not strict equality)
            if (balance.mint !in allowedMints) {
                continue
            }
            mintMatched = true

            val delta = balance.postAmount - balance.preAmount
            Log.d(TAG, "    Balance delta: owner=${balance.owner}, mint=${balance.mint}, delta=$delta")

            if (balance.owner == expectedRecipient && delta > 0) {
                recipientFound = true
                amountReceived = delta
                actualMintUsed = balance.mint
                Log.d(TAG, "    ✓ Found recipient balance increase: $amountReceived (mint=${balance.mint})")
            } else if (delta < 0 && payerAddress == null) {
                // Found sender - balance decreased
                payerAddress = balance.owner
                // Resolve token account address from accountKeys using accountIndex
                payerTokenAccount = accountKeys?.optString(balance.accountIndex)
                Log.d(TAG, "    ✓ Found payer: owner=${balance.owner}, tokenAccount=$payerTokenAccount, delta=$delta")
            }
        }

        // STRICT: At least one allowed mint must be found
        if (!mintMatched) {
            val allowedStr = allowedMints.joinToString(", ") { SolanaTokenPolicy.getTokenSymbol(it) }
            Log.e(TAG, "  ✗ REJECTION: MINT_NOT_FOUND")
            Log.e(TAG, "    allowedMints: ${allowedMints.joinToString(", ")}")
            Log.e(TAG, "    foundMints in tx: ${balanceMap.values.map { it.mint }.distinct().joinToString(", ")}")
            return Result.Invalid("No allowed mint ($allowedStr) found in token balances")
        }

        // STRICT: Recipient must be found
        if (!recipientFound) {
            Log.e(TAG, "  ✗ REJECTION: RECIPIENT_NOT_FOUND")
            Log.e(TAG, "    expectedRecipient: $expectedRecipient")
            Log.e(TAG, "    foundOwners: ${balanceMap.values.map { it.owner }.distinct().joinToString(", ")}")
            Log.e(TAG, "    NOTE: For SPL transfers, owner is the WALLET, not the ATA")
            return Result.Invalid("Recipient $expectedRecipient did not receive tokens")
        }

        // STRICT: Amount must be >= expected (merchant protection)
        // Allow +1 tolerance for rounding only
        if (amountReceived < expectedAmountMinor) {
            val shortfall = expectedAmountMinor - amountReceived
            if (shortfall > 1) {
                Log.e(TAG, "    ✗ UNDERPAY: received=$amountReceived, expected=$expectedAmountMinor, shortfall=$shortfall")
                return Result.Invalid("Underpayment: received $amountReceived, expected $expectedAmountMinor (short by $shortfall)")
            }
        }

        // Determine if payment was made with a different stablecoin
        val warningCode = SolanaTokenPolicy.getWarningCode(expectedMint, actualMintUsed)

        Log.d(TAG, "    ✓ SPL transfer valid: received=$amountReceived, expected=$expectedAmountMinor, mint=${SolanaTokenPolicy.getTokenSymbol(actualMintUsed)}")
        Log.d(TAG, "    ✓ Payer info: address=$payerAddress, tokenAccount=$payerTokenAccount")
        return Result.Valid(
            actualMintUsed = actualMintUsed,
            warningCode = warningCode,
            payerAddress = payerAddress,
            payerTokenAccount = payerTokenAccount
        )
    }

    /**
     * STRICT SOL transfer validation.
     *
     * Requirements:
     * 1. Find system transfer instruction to recipient
     * 2. Transfer amount MUST be >= expected amount
     *
     * PAYER EXTRACTION:
     * - Returns sender address from system transfer instruction
     */
    private fun validateSolTransferStrict(
        message: JSONObject?,
        meta: JSONObject?,
        expectedRecipient: String,
        expectedAmountMinor: Long
    ): Result {
        val instructions = message?.optJSONArray("instructions")
        if (instructions == null) {
            return Result.Invalid("No instructions in transaction")
        }

        Log.d(TAG, "  SOL validation: instructions=${instructions.length()}")

        // Also check inner instructions (for complex transactions)
        val innerInstructions = meta?.optJSONArray("innerInstructions") ?: JSONArray()

        // Check main instructions
        val mainResult = findSolTransfer(instructions, expectedRecipient, expectedAmountMinor, "main")
        if (mainResult is Result.Valid) {
            return mainResult
        }

        // Check inner instructions
        for (i in 0 until innerInstructions.length()) {
            val innerGroup = innerInstructions.optJSONObject(i)
            val innerIxs = innerGroup?.optJSONArray("instructions") ?: continue
            val innerResult = findSolTransfer(innerIxs, expectedRecipient, expectedAmountMinor, "inner[$i]")
            if (innerResult is Result.Valid) {
                return innerResult
            }
        }

        return Result.Invalid("No valid SOL transfer to recipient $expectedRecipient found")
    }

    /**
     * Find SOL transfer instruction to recipient.
     * Returns Valid result with payerAddress set to sender.
     */
    private fun findSolTransfer(
        instructions: JSONArray,
        expectedRecipient: String,
        expectedAmountMinor: Long,
        source: String
    ): Result {
        for (i in 0 until instructions.length()) {
            val instruction = instructions.optJSONObject(i) ?: continue
            val program = instruction.optString("program", "")
            val parsed = instruction.optJSONObject("parsed")

            if (program == "system" && parsed != null) {
                val type = parsed.optString("type", "")
                if (type == "transfer") {
                    val info = parsed.optJSONObject("info")
                    if (info != null) {
                        val destination = info.optString("destination", "")
                        val sender = info.optString("source", "")
                        val lamports = info.optLong("lamports", 0L)

                        Log.d(TAG, "    $source[$i]: type=transfer, from=$sender, to=$destination, lamports=$lamports")

                        if (destination == expectedRecipient) {
                            // STRICT: Amount must be >= expected
                            if (lamports < expectedAmountMinor) {
                                val shortfall = expectedAmountMinor - lamports
                                if (shortfall > 1) {
                                    Log.e(TAG, "    ✗ UNDERPAY: received=$lamports, expected=$expectedAmountMinor")
                                    return Result.Invalid("Underpayment: received $lamports, expected $expectedAmountMinor")
                                }
                            }
                            Log.d(TAG, "    ✓ SOL transfer valid: received=$lamports, expected=$expectedAmountMinor")
                            Log.d(TAG, "    ✓ Payer (sender): $sender")
                            return Result.Valid(
                                payerAddress = sender,
                                payerTokenAccount = null  // Native SOL - no token account
                            )
                        }
                    }
                }
            }
        }

        return Result.Invalid("Transfer to recipient not found in $source instructions")
    }

    /**
     * Extract owner from token balance object.
     * Handles both string and object formats.
     */
    private fun extractOwner(balance: JSONObject): String {
        val ownerObj = balance.opt("owner")
        return when (ownerObj) {
            is String -> ownerObj
            is JSONObject -> ownerObj.optString("pubkey", ownerObj.toString())
            else -> ""
        }
    }

    /**
     * Extract token amount from balance object.
     */
    private fun extractTokenAmount(balance: JSONObject): Long {
        val uiTokenAmount = balance.optJSONObject("uiTokenAmount")
        return uiTokenAmount?.optString("amount", "0")?.toLongOrNull() ?: 0L
    }

    /**
     * Get signatures for address with multi-RPC failover.
     *
     * Returns SignaturesResult containing:
     * - List of (signature, blockTime) pairs, newest first
     * - The provider that succeeded (for consistency in subsequent calls)
     *
     * @param address Reference pubkey to query
     * @param limit Max signatures to return
     * @return SignaturesResult with data and provider info
     */
    data class SignaturesResult(
        val signatures: List<Pair<String, Long>>,
        val provider: SolanaRpcProvider?,
        val providerName: String
    )

    fun getSignaturesForAddressWithProvider(
        address: String,
        limit: Int = 10
    ): SignaturesResult {
        return try {
            val result = runBlocking {
                RpcProviderManager.getSignaturesForAddress(address, limit)
            }

            when (result) {
                is RpcCallResult.Failure -> {
                    Log.e(TAG, "getSignaturesForAddress failed: ${result.message}")
                    SignaturesResult(emptyList(), null, "FAILED")
                }
                is RpcCallResult.Success -> {
                    val signatures = result.data.map { it.signature to it.blockTimeMs }
                    val provider = RpcProviderManager.findProviderByName(result.providerName)
                    SignaturesResult(signatures, provider, result.providerName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSignaturesForAddress exception: ${e.message}", e)
            SignaturesResult(emptyList(), null, "EXCEPTION")
        }
    }

    /**
     * Legacy method for backwards compatibility.
     * Use getSignaturesForAddressWithProvider for new code.
     */
    @Deprecated("Use getSignaturesForAddressWithProvider for provider tracking")
    fun getSignaturesForAddress(
        address: String,
        rpcUrl: String = BuildConfig.SOLANA_RPC_URL,
        limit: Int = 10
    ): List<Pair<String, Long>> {
        return getSignaturesForAddressWithProvider(address, limit).signatures
    }

    /**
     * Check if transaction is confirmed with multi-RPC failover.
     *
     * @param preferredProvider Provider to try first (for consistency)
     */
    fun checkConfirmation(
        signature: String,
        preferredProvider: SolanaRpcProvider? = null
    ): Boolean {
        return try {
            val result = runBlocking {
                RpcProviderManager.getSignatureStatus(signature, preferredProvider)
            }

            when (result) {
                is RpcCallResult.Failure -> {
                    Log.e(TAG, "checkConfirmation failed: ${result.message}")
                    false
                }
                is RpcCallResult.Success -> {
                    result.data == ConfirmationStatus.CONFIRMED ||
                    result.data == ConfirmationStatus.FINALIZED
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkConfirmation exception: ${e.message}", e)
            false
        }
    }

    /**
     * Legacy overload for backwards compatibility.
     */
    @Deprecated("Use checkConfirmation with preferredProvider")
    fun checkConfirmation(
        signature: String,
        rpcUrl: String
    ): Boolean {
        return checkConfirmation(signature, null)
    }
}
