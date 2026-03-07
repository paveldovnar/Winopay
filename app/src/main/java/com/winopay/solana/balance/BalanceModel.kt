package com.winopay.solana.balance

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Model representing wallet balances.
 * Uses Long for raw amounts (minor units) and BigDecimal for display calculations.
 */
data class BalanceModel(
    val solBalanceLamports: Long,
    val usdcBalanceRaw: Long,
    val usdtBalanceRaw: Long,
    val lastUpdated: Long,
    val warning: BalanceWarning? = null
) {
    companion object {
        const val LAMPORTS_PER_SOL = 1_000_000_000L
        const val STABLECOIN_DECIMALS = 6
        private val STABLECOIN_DIVISOR = BigDecimal("1000000")

        fun empty() = BalanceModel(
            solBalanceLamports = 0L,
            usdcBalanceRaw = 0L,
            usdtBalanceRaw = 0L,
            lastUpdated = 0L,
            warning = null
        )

        fun fromRawAmounts(
            solLamports: Long,
            usdcRaw: Long,
            usdtRaw: Long = 0L,
            timestamp: Long = System.currentTimeMillis(),
            warning: BalanceWarning? = null
        ) = BalanceModel(
            solBalanceLamports = solLamports,
            usdcBalanceRaw = usdcRaw,
            usdtBalanceRaw = usdtRaw,
            lastUpdated = timestamp,
            warning = warning
        )
    }

    /**
     * SOL balance as BigDecimal.
     */
    val solBalance: BigDecimal
        get() = BigDecimal(solBalanceLamports).divide(BigDecimal(LAMPORTS_PER_SOL), 9, RoundingMode.HALF_UP)

    /**
     * USDC balance as BigDecimal.
     */
    val usdcBalance: BigDecimal
        get() = BigDecimal(usdcBalanceRaw).divide(STABLECOIN_DIVISOR, STABLECOIN_DECIMALS, RoundingMode.HALF_UP)

    /**
     * USDT balance as BigDecimal.
     */
    val usdtBalance: BigDecimal
        get() = BigDecimal(usdtBalanceRaw).divide(STABLECOIN_DIVISOR, STABLECOIN_DECIMALS, RoundingMode.HALF_UP)

    /**
     * Total stablecoin balance (USDC + USDT) as BigDecimal.
     */
    val totalStablecoinBalance: BigDecimal
        get() = usdcBalance.add(usdtBalance)

    /**
     * Total stablecoin balance as Double (for UI compatibility).
     */
    val totalStablecoinBalanceDouble: Double
        get() = totalStablecoinBalance.toDouble()

    /**
     * Check if balance data is stale (older than given threshold).
     */
    fun isStale(thresholdMs: Long = 60_000): Boolean {
        return System.currentTimeMillis() - lastUpdated > thresholdMs
    }

    /**
     * Format SOL balance for display.
     */
    fun formatSol(decimals: Int = 4): String {
        return solBalance.setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Format USDC balance for display.
     */
    fun formatUsdc(decimals: Int = 2): String {
        return usdcBalance.setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Format USDT balance for display.
     */
    fun formatUsdt(decimals: Int = 2): String {
        return usdtBalance.setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Format total stablecoin balance for display.
     */
    fun formatTotalStablecoin(decimals: Int = 2): String {
        return totalStablecoinBalance.setScale(decimals, RoundingMode.HALF_UP).toPlainString()
    }

    /**
     * Check if there's a mint mismatch warning.
     */
    fun hasMintMismatchWarning(): Boolean = warning is BalanceWarning.MintMismatch
}

/**
 * Warning types for balance issues.
 */
sealed class BalanceWarning {
    /**
     * User has tokens but with different mint than expected.
     * Common on devnet where multiple "USDC" mints exist.
     */
    data class MintMismatch(
        val message: String,
        val detectedMints: List<DetectedToken>
    ) : BalanceWarning()

    data class DetectedToken(
        val mint: String,
        val balance: String,
        val decimals: Int
    )
}

/**
 * Result wrapper for balance fetching.
 */
sealed class BalanceResult {
    data class Success(val balance: BalanceModel) : BalanceResult()
    data class Error(val message: String, val cause: Throwable? = null) : BalanceResult()
    data object Loading : BalanceResult()
}
