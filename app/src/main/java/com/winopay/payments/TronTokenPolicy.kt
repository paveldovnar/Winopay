package com.winopay.payments

import android.util.Log
import com.winopay.tron.TronConstants

/**
 * TRON-specific token acceptance policy.
 *
 * TRON SUPPORTS ONLY USDT (no USDC).
 * Unlike Solana which has dual stablecoin acceptance, TRON accepts only USDT TRC20.
 *
 * MAINNET CONTRACT:
 * - USDT: TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t
 *
 * NOTE: This class only manages the token allowlist policy.
 * Actual balance/transfer queries are done via TronProviderManager.
 */
class TronTokenPolicy(
    private val networkId: String = TronConstants.NETWORK_MAINNET
) : TokenPolicyProvider {

    companion object {
        private const val TAG = "TronTokenPolicy"
    }

    /** USDT contract for current network */
    val usdtContract: String = TronConstants.getUsdtContract(networkId)

    /**
     * Check if a contract address looks valid (basic length check).
     */
    private fun isValidContract(contract: String?): Boolean {
        if (contract.isNullOrBlank()) return false
        return contract.length == 34 && contract.startsWith("T")
    }

    /**
     * TRON does NOT support dual stablecoin acceptance.
     * Only USDT is supported on TRON.
     */
    override val isDualAcceptanceEnabled: Boolean = false

    /** Set of supported stablecoin contracts (USDT only) */
    val allStablecoinContracts: Set<String> by lazy {
        buildSet {
            if (isValidContract(usdtContract)) add(usdtContract)
        }.also {
            Log.d(TAG, "TRON|STABLECOIN_POLICY|network=$networkId|usdt=$usdtContract|dual_acceptance=DISABLED")
        }
    }

    override fun isStablecoin(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return token in allStablecoinContracts
    }

    override fun getAllowedTokens(requestedToken: String?): Set<String> {
        if (requestedToken.isNullOrBlank()) {
            return emptySet()
        }

        // TRON: strict mode only - accept exact token requested
        // No dual acceptance on TRON (only USDT is supported)
        return setOf(requestedToken)
    }

    override fun getWarningCode(expected: String?, actual: String?): String? {
        // TRON: no dual acceptance, so no stablecoin mismatch warnings
        // Payment must match expected token exactly
        return null
    }

    override fun getTokenSymbol(token: String?): String {
        return TronConstants.getTokenSymbol(token, networkId)
    }
}
