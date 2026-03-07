package com.winopay.payments

/**
 * Chain-agnostic interface for token acceptance policy.
 *
 * Defines which tokens are accepted for payment and how to handle
 * interchangeable tokens (e.g., USDC/USDT on Solana, USDC/USDT on EVM).
 *
 * IMPLEMENTATION:
 * - Solana: SolanaTokenPolicy (USDC+USDT dual acceptance)
 * - EVM: EvmTokenPolicy (stub, future)
 */
interface TokenPolicyProvider {

    /**
     * Get the set of allowed token addresses for a requested token.
     *
     * For interchangeable stablecoins (USDC/USDT):
     * - Returns {USDC, USDT} if dual acceptance enabled
     *
     * For other tokens:
     * - Returns {requestedToken} (strict matching)
     *
     * @param requestedToken Token address from invoice (null for native currency)
     * @return Set of token addresses to accept
     */
    fun getAllowedTokens(requestedToken: String?): Set<String>

    /**
     * Check if a token is a known stablecoin eligible for dual acceptance.
     */
    fun isStablecoin(token: String?): Boolean

    /**
     * Get warning code when payment uses different but acceptable token.
     *
     * @param expected Expected token from invoice
     * @param actual Actual token used in payment
     * @return Warning code or null if no warning needed
     */
    fun getWarningCode(expected: String?, actual: String?): String?

    /**
     * Get human-readable symbol for a token (for logging/display).
     */
    fun getTokenSymbol(token: String?): String

    /**
     * Whether dual stablecoin acceptance is enabled.
     */
    val isDualAcceptanceEnabled: Boolean

    companion object {
        /** Warning code for payment with different but accepted stablecoin */
        const val WARNING_PAID_WITH_DIFFERENT_STABLECOIN = "PAID_WITH_DIFFERENT_STABLECOIN"
    }
}
