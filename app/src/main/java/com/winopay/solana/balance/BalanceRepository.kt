package com.winopay.solana.balance

import android.util.Log
import com.winopay.BuildConfig
import com.winopay.solana.rpc.RpcResult
import com.winopay.solana.rpc.SolanaRpcClient
import com.winopay.solana.rpc.TokenAccountInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository for fetching and caching wallet balances.
 *
 * Features:
 * - Fetches USDC and USDT balances using getTokenAccountsByOwner
 * - Diagnostic logging for all token accounts
 * - Mint mismatch detection for devnet scenarios
 *
 * Usage:
 * 1. Set wallet address via setWalletAddress()
 * 2. Call refresh() to fetch latest balances
 * 3. Observe balanceState for updates
 */
class BalanceRepository(
    private val rpcClient: SolanaRpcClient = SolanaRpcClient()
) {

    companion object {
        private const val TAG = "BalanceRepository"

        // Supported mints from BuildConfig
        private val USDC_MINT: String = BuildConfig.USDC_MINT
        private val USDT_MINT: String = BuildConfig.USDT_MINT

        // Minimum decimals to consider as potential stablecoin (usually 6)
        private const val STABLECOIN_MIN_DECIMALS = 5
        private const val STABLECOIN_MAX_DECIMALS = 9
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var walletAddress: String? = null

    private val _balanceState = MutableStateFlow<BalanceResult>(BalanceResult.Loading)
    val balanceState: StateFlow<BalanceResult> = _balanceState.asStateFlow()

    private val _balance = MutableStateFlow(BalanceModel.empty())
    val balance: StateFlow<BalanceModel> = _balance.asStateFlow()

    /**
     * Set the wallet address to fetch balances for.
     */
    fun setWalletAddress(address: String?) {
        if (walletAddress != address) {
            walletAddress = address
            if (address != null) {
                Log.d(TAG, "Wallet address set: $address")
                refresh()
            } else {
                _balance.value = BalanceModel.empty()
                _balanceState.value = BalanceResult.Success(BalanceModel.empty())
            }
        }
    }

    /**
     * Refresh balances from RPC.
     */
    fun refresh() {
        val address = walletAddress
        if (address == null) {
            Log.w(TAG, "Cannot refresh: no wallet address set")
            return
        }

        scope.launch {
            fetchBalances(address)
        }
    }

    /**
     * Refresh balances and suspend until complete.
     */
    suspend fun refreshAsync(): BalanceResult {
        val address = walletAddress
        if (address == null) {
            Log.w(TAG, "Cannot refresh: no wallet address set")
            return BalanceResult.Error("No wallet address set")
        }

        return fetchBalances(address)
    }

    /**
     * Fetch SOL, USDC, and USDT balances with diagnostic logging.
     */
    private suspend fun fetchBalances(address: String): BalanceResult {
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "BALANCE FETCH START")
        Log.i(TAG, "  owner: $address")
        Log.i(TAG, "  cluster: ${BuildConfig.SOLANA_CLUSTER}")
        Log.i(TAG, "  USDC_MINT: $USDC_MINT")
        Log.i(TAG, "  USDT_MINT: ${USDT_MINT.ifEmpty { "(not configured)" }}")
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        _balanceState.value = BalanceResult.Loading

        // 1. Fetch SOL balance
        val solResult = rpcClient.getBalance(address)
        val solLamports = when (solResult) {
            is RpcResult.Success -> {
                Log.d(TAG, "  SOL balance: ${solResult.data} lamports")
                solResult.data
            }
            is RpcResult.Error -> {
                Log.e(TAG, "  SOL balance fetch FAILED: ${solResult.message}")
                val error = BalanceResult.Error("Failed to fetch SOL balance: ${solResult.message}", solResult.cause)
                _balanceState.value = error
                return error
            }
        }

        // 2. Fetch USDC balance (using specific mint)
        val usdcRaw = fetchTokenBalance(address, USDC_MINT, "USDC")

        // 3. Fetch USDT balance (if mint configured)
        val usdtRaw = if (USDT_MINT.isNotEmpty()) {
            fetchTokenBalance(address, USDT_MINT, "USDT")
        } else {
            Log.d(TAG, "  USDT: skipped (no mint configured)")
            0L
        }

        // 4. Diagnostic: fetch ALL token accounts to detect mint mismatch
        var warning: BalanceWarning? = null
        val totalStablecoin = usdcRaw + usdtRaw

        if (totalStablecoin == 0L) {
            Log.w(TAG, "  Supported stablecoin balance is $0 — checking for mint mismatch...")
            warning = checkForMintMismatch(address)
        }

        // 5. Build result
        val balanceModel = BalanceModel.fromRawAmounts(
            solLamports = solLamports,
            usdcRaw = usdcRaw,
            usdtRaw = usdtRaw,
            warning = warning
        )

        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.i(TAG, "BALANCE FETCH COMPLETE")
        Log.i(TAG, "  SOL:  ${balanceModel.formatSol()}")
        Log.i(TAG, "  USDC: ${balanceModel.formatUsdc()} (raw: $usdcRaw)")
        Log.i(TAG, "  USDT: ${balanceModel.formatUsdt()} (raw: $usdtRaw)")
        Log.i(TAG, "  Total stablecoin: ${balanceModel.formatTotalStablecoin()}")
        if (warning != null) {
            Log.w(TAG, "  WARNING: ${(warning as? BalanceWarning.MintMismatch)?.message}")
        }
        Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        _balance.value = balanceModel
        val success = BalanceResult.Success(balanceModel)
        _balanceState.value = success
        return success
    }

    /**
     * Fetch token balance for a specific mint.
     */
    private suspend fun fetchTokenBalance(owner: String, mint: String, label: String): Long {
        if (mint.isEmpty()) {
            Log.d(TAG, "  $label: skipped (empty mint)")
            return 0L
        }

        val result = rpcClient.getTokenBalance(owner, mint)
        return when (result) {
            is RpcResult.Success -> {
                Log.d(TAG, "  $label balance: ${result.data} (raw, mint: ${mint.take(8)}...)")
                result.data
            }
            is RpcResult.Error -> {
                Log.e(TAG, "  $label balance fetch FAILED: ${result.message}")
                0L
            }
        }
    }

    /**
     * Check for mint mismatch by fetching all token accounts.
     * Returns warning if user has tokens with different mints.
     */
    private suspend fun checkForMintMismatch(owner: String): BalanceWarning? {
        val allAccountsResult = rpcClient.getAllTokenAccounts(owner)

        when (allAccountsResult) {
            is RpcResult.Error -> {
                Log.e(TAG, "  getAllTokenAccounts FAILED: ${allAccountsResult.message}")
                return null
            }
            is RpcResult.Success -> {
                val accounts = allAccountsResult.data
                Log.d(TAG, "  Found ${accounts.size} total token accounts")

                // Log ALL token accounts for diagnostics
                logAllTokenAccounts(accounts)

                // Find accounts with non-zero balance that look like stablecoins
                val potentialStablecoins = accounts.filter { account ->
                    account.hasBalance() &&
                    account.decimals in STABLECOIN_MIN_DECIMALS..STABLECOIN_MAX_DECIMALS &&
                    account.mint != USDC_MINT &&
                    account.mint != USDT_MINT
                }

                if (potentialStablecoins.isNotEmpty()) {
                    Log.w(TAG, "━━━━━ MINT MISMATCH DETECTED ━━━━━")
                    Log.w(TAG, "  Expected USDC mint: $USDC_MINT")
                    Log.w(TAG, "  Found ${potentialStablecoins.size} token(s) with different mint(s):")
                    potentialStablecoins.forEach { account ->
                        Log.w(TAG, "    mint: ${account.mint}")
                        Log.w(TAG, "    balance: ${account.uiAmountString} (raw: ${account.amountRaw})")
                        Log.w(TAG, "    decimals: ${account.decimals}")
                    }
                    Log.w(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    return BalanceWarning.MintMismatch(
                        message = "USDC mint mismatch on devnet. Your wallet holds a different USDC token mint than the app supports.",
                        detectedMints = potentialStablecoins.take(10).map { account ->
                            BalanceWarning.DetectedToken(
                                mint = account.mint,
                                balance = account.uiAmountString,
                                decimals = account.decimals
                            )
                        }
                    )
                }

                return null
            }
        }
    }

    /**
     * Log all token accounts for diagnostic purposes.
     */
    private fun logAllTokenAccounts(accounts: List<TokenAccountInfo>) {
        if (accounts.isEmpty()) {
            Log.d(TAG, "  (no token accounts found)")
            return
        }

        Log.d(TAG, "  ┌─────────────────────────────────────────────────────────┐")
        Log.d(TAG, "  │ ALL TOKEN ACCOUNTS (diagnostic dump)                    │")
        Log.d(TAG, "  ├─────────────────────────────────────────────────────────┤")

        accounts.take(10).forEachIndexed { index, account ->
            val mintShort = "${account.mint.take(8)}...${account.mint.takeLast(4)}"
            val isSupported = when (account.mint) {
                USDC_MINT -> " [USDC ✓]"
                USDT_MINT -> " [USDT ✓]"
                else -> ""
            }
            Log.d(TAG, "  │ ${index + 1}. mint: $mintShort$isSupported")
            Log.d(TAG, "  │    pubkey: ${account.pubkey.take(8)}...${account.pubkey.takeLast(4)}")
            Log.d(TAG, "  │    amount: ${account.uiAmountString} (raw: ${account.amountRaw}, decimals: ${account.decimals})")
        }

        if (accounts.size > 10) {
            Log.d(TAG, "  │ ... and ${accounts.size - 10} more accounts")
        }
        Log.d(TAG, "  └─────────────────────────────────────────────────────────┘")
    }

    /**
     * Get current balance synchronously (from cache).
     */
    fun getCurrentBalance(): BalanceModel = _balance.value

    /**
     * Check if balance data is stale.
     */
    fun isStale(thresholdMs: Long = 60_000): Boolean {
        return _balance.value.isStale(thresholdMs)
    }
}
