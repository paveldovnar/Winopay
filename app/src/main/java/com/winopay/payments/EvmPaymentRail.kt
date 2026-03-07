package com.winopay.payments

import com.winopay.data.local.InvoiceEntity

/**
 * STUB: EVM (Ethereum/Polygon/BSC) implementation of PaymentRail.
 *
 * This is a compile-only stub to prove the interface is chain-agnostic.
 * All methods return "unsupported" or throw UnsupportedOperationException.
 *
 * FUTURE IMPLEMENTATION NOTES:
 * - buildPaymentRequest: Generate EIP-681 payment URL or WalletConnect deep link
 * - deriveTokenAddress: Return wallet address (ERC-20 balances tracked by contract)
 * - listCandidates: Use etherscan/infura API to list recent token transfers
 * - validateCandidate: Parse ERC-20 Transfer event logs
 * - checkConfirmation: Check block confirmations (12 for ETH, 15 for BSC)
 */
class EvmPaymentRail private constructor(
    override val railId: String,
    private val chainId: String
) : PaymentRail {

    override val networkId: String = chainId

    override val tokenPolicy: TokenPolicyProvider = EvmTokenPolicyStub

    override fun getSupportedTokens(): List<SupportedToken> {
        // EVM not implemented - return empty list
        return emptyList()
    }

    companion object {
        const val RAIL_ID = "evm"

        // Supported EVM networks (chainId -> networkName)
        val SUPPORTED_NETWORKS = setOf(
            "1",      // Ethereum Mainnet
            "5",      // Goerli Testnet
            "137",    // Polygon Mainnet
            "80001",  // Polygon Mumbai
            "56",     // BSC Mainnet
            "97"      // BSC Testnet
        )

        fun supportsNetwork(networkId: String): Boolean {
            return networkId in SUPPORTED_NETWORKS
        }

        /**
         * Get an EVM rail instance for the given chain.
         * @throws UnsupportedOperationException Always - EVM not yet implemented
         */
        fun forChain(chainId: String): EvmPaymentRail {
            require(chainId in SUPPORTED_NETWORKS) { "Unsupported EVM chain: $chainId" }
            return EvmPaymentRail(RAIL_ID, chainId)
        }
    }

    override fun buildPaymentRequest(invoice: InvoiceEntity): String {
        // TODO: Generate EIP-681 payment URL
        // Format: ethereum:<address>@<chainId>/transfer?address=<token>&uint256=<amount>
        throw UnsupportedOperationException("EVM payment requests not yet implemented")
    }

    override fun generateReference(): String {
        // TODO: Generate unique nonce or use invoice ID
        throw UnsupportedOperationException("EVM reference generation not yet implemented")
    }

    override fun deriveTokenAddress(ownerWallet: String, tokenMint: String): String {
        // EVM: wallet address IS the token receiving address
        // ERC-20 balances are tracked by the token contract, not separate accounts
        return ownerWallet
    }

    override fun derivePollingTargets(invoice: InvoiceEntity): List<PollingTarget> {
        // TODO: Return wallet address as polling target for ERC-20 transfers
        throw UnsupportedOperationException("EVM polling targets not yet implemented")
    }

    override suspend fun discoverTokenAccountTargets(invoice: InvoiceEntity): List<PollingTarget> {
        // EVM chains don't have "token accounts" like Solana ATAs
        // ERC-20 balances are tracked by the token contract, not separate accounts
        // This method always returns empty for EVM - no fallback discovery needed
        return emptyList()
    }

    override suspend fun listCandidates(target: PollingTarget, limit: Int): CandidateListResult {
        // TODO: Use Etherscan/Infura API to list recent token transfers to address
        throw UnsupportedOperationException("EVM candidate listing not yet implemented")
    }

    override suspend fun validateCandidate(params: ValidationParams): ValidationResult {
        // TODO: Parse ERC-20 Transfer event logs and validate amount/sender
        throw UnsupportedOperationException("EVM validation not yet implemented")
    }

    override suspend fun checkConfirmation(transactionId: String, providerHint: Any?): Boolean {
        // TODO: Check block confirmations (12 for ETH mainnet, varies by chain)
        throw UnsupportedOperationException("EVM confirmation check not yet implemented")
    }

    override fun startForegroundDetection(
        invoice: InvoiceEntity,
        timeoutMs: Long,
        onDetected: OnPaymentDetected
    ) {
        // TODO: Poll for ERC-20 Transfer events
        throw UnsupportedOperationException("EVM foreground detection not yet implemented")
    }

    override fun cancelForegroundDetection(invoiceId: String) {
        // No-op for stub
    }

    override fun cancelAllForegroundDetections() {
        // No-op for stub
    }

    override fun isDetecting(invoiceId: String): Boolean {
        return false
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REFUND METHODS (STUB)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    override fun canRefund(invoice: InvoiceEntity): Boolean {
        // EVM refunds not yet implemented
        return false
    }

    override suspend fun buildRefundTransaction(invoice: InvoiceEntity): RefundTransactionResult {
        return RefundTransactionResult.NotEligible("EVM refunds not yet implemented")
    }

    override suspend fun sendRefundTransaction(signedTransaction: ByteArray): RefundSendResult {
        return RefundSendResult.Failure("EVM refunds not yet implemented")
    }
}

/**
 * Stub token policy for EVM chains.
 * Future: implement per-chain stablecoin acceptance.
 */
private object EvmTokenPolicyStub : TokenPolicyProvider {
    override val isDualAcceptanceEnabled: Boolean = false
    override fun getAllowedTokens(requestedToken: String?): Set<String> =
        if (requestedToken != null) setOf(requestedToken) else emptySet()
    override fun isStablecoin(token: String?): Boolean = false
    override fun getWarningCode(expected: String?, actual: String?): String? = null
    override fun getTokenSymbol(token: String?): String = token ?: "ETH"
}
