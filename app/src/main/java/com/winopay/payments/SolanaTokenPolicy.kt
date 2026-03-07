package com.winopay.payments

import android.util.Log
import com.winopay.BuildConfig

/**
 * Solana-specific token acceptance policy.
 *
 * Implements dual stablecoin acceptance (USDC + USDT) for Solana.
 * Mints are configured via BuildConfig (devnet/mainnet flavors).
 *
 * NOTE: This class does NOT derive ATAs - that's done by SolanaPaymentRail.
 * This class only manages the token allowlist policy.
 */
object SolanaTokenPolicy : TokenPolicyProvider {

    private const val TAG = "SolanaTokenPolicy"

    /** USDC mint address from BuildConfig */
    val USDC_MINT: String = BuildConfig.USDC_MINT

    /** USDT mint address from BuildConfig (may be empty on devnet) */
    val USDT_MINT: String = BuildConfig.USDT_MINT

    /**
     * Check if a mint address looks valid (basic length check).
     * Full validation is done at the Solana layer.
     */
    private fun isValidMint(mint: String?): Boolean {
        if (mint.isNullOrBlank()) return false
        return mint.length in 32..44
    }

    override val isDualAcceptanceEnabled: Boolean by lazy {
        val usdcValid = isValidMint(USDC_MINT)
        val usdtValid = isValidMint(USDT_MINT)
        val enabled = usdcValid && usdtValid

        Log.d(TAG, "━━━━━ DUAL STABLECOIN CONFIG ━━━━━")
        Log.d(TAG, "  USDC_MINT: $USDC_MINT (valid=$usdcValid)")
        Log.d(TAG, "  USDT_MINT: ${USDT_MINT.ifBlank { "(empty)" }} (valid=$usdtValid)")
        Log.d(TAG, "  Dual acceptance: ${if (enabled) "ENABLED" else "DISABLED"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        enabled
    }

    /** Set of all valid stablecoin mints */
    val allStablecoinMints: Set<String> by lazy {
        buildSet {
            if (isValidMint(USDC_MINT)) add(USDC_MINT)
            if (isValidMint(USDT_MINT)) add(USDT_MINT)
        }
    }

    override fun isStablecoin(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return token in allStablecoinMints
    }

    override fun getAllowedTokens(requestedToken: String?): Set<String> {
        if (requestedToken.isNullOrBlank()) {
            return emptySet()
        }

        // If dual acceptance enabled AND requestedToken is a stablecoin
        if (isDualAcceptanceEnabled && isStablecoin(requestedToken)) {
            return allStablecoinMints
        }

        // Strict: only accept exact token
        return setOf(requestedToken)
    }

    override fun getWarningCode(expected: String?, actual: String?): String? {
        if (expected == null || actual == null) return null
        if (expected == actual) return null

        // Warning only if both are stablecoins but different
        if (isStablecoin(expected) && isStablecoin(actual)) {
            Log.w(TAG, "⚠ STABLECOIN MISMATCH: expected=${getTokenSymbol(expected)}, paid=${getTokenSymbol(actual)}")
            return TokenPolicyProvider.WARNING_PAID_WITH_DIFFERENT_STABLECOIN
        }

        return null
    }

    override fun getTokenSymbol(token: String?): String {
        return when (token) {
            USDC_MINT -> "USDC"
            USDT_MINT -> "USDT"
            null -> "SOL"
            else -> "UNKNOWN"
        }
    }
}
