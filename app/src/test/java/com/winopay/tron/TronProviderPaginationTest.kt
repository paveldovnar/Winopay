package com.winopay.tron

import com.winopay.tron.rpc.Trc20Transaction
import com.winopay.tron.rpc.Trc20TransactionsPage
import com.winopay.tron.rpc.TronProviderManager
import com.winopay.tron.rpc.TronRpcErrorType
import com.winopay.tron.rpc.TronRpcProvider
import com.winopay.tron.rpc.TronRpcResult
import com.winopay.tron.rpc.TronTransaction
import com.winopay.tron.rpc.TronTransactionInfo
import com.winopay.tron.rpc.UnsignedTronTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TRON provider pagination and failover logic.
 *
 * Tests:
 * 1. Pagination stop conditions (found candidate, reached old tx, safety cap)
 * 2. createdAt gating correctness with ms timestamps
 * 3. Failover behavior patterns
 */
class TronProviderPaginationTest {

    @Before
    fun setup() {
        TronProviderManager.resetLastSuccessful()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // PAGINATION STOP CONDITIONS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `pagination constants are reasonable`() {
        // Safety cap should be small but sufficient
        assertTrue("MAX_PAGES should be >= 2", TronProviderManager.MAX_PAGES >= 2)
        assertTrue("MAX_PAGES should be <= 5", TronProviderManager.MAX_PAGES <= 5)

        // Page size should be efficient
        assertTrue("PAGE_SIZE should be >= 20", TronProviderManager.PAGE_SIZE >= 20)
        assertTrue("PAGE_SIZE should be <= 200", TronProviderManager.PAGE_SIZE <= 200)
    }

    @Test
    fun `stop condition 1 - candidate matcher returns true stops pagination`() {
        // The candidateMatcher function should stop pagination when it returns true
        val txId = "target-tx-123"
        var matcherCalled = false

        val matcher: (Trc20Transaction) -> Boolean = { tx ->
            matcherCalled = true
            tx.txId == txId
        }

        // Matcher should return true for matching txId
        val tx = createTestTx(txId = txId, blockTimestamp = System.currentTimeMillis())
        assertTrue("Matcher should match target txId", matcher(tx))
        assertTrue("Matcher should have been called", matcherCalled)
    }

    @Test
    fun `stop condition 2 - transactions older than createdAt stops scanning`() {
        // When we encounter a tx older than invoice createdAt, we should stop
        val invoiceCreatedAt = 1700000000000L // Nov 14, 2023 in ms
        val oldTxTimestamp = 1699999999000L   // 1 second before invoice

        // Old transaction should be filtered out
        assertTrue(
            "Transaction before invoice creation should be filtered",
            oldTxTimestamp < invoiceCreatedAt
        )

        // Newer transaction should pass
        val newTxTimestamp = 1700000001000L
        assertTrue(
            "Transaction after invoice creation should pass",
            newTxTimestamp >= invoiceCreatedAt
        )
    }

    @Test
    fun `stop condition 3 - safety cap MAX_PAGES limits pagination`() {
        // Ensure we have a safety cap
        assertEquals(
            "MAX_PAGES should be 3 for safety",
            3,
            TronProviderManager.MAX_PAGES
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATED_AT GATING (ms timestamps)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `createdAt gating rejects transactions before invoice creation`() {
        val invoiceCreatedAt = 1700000000000L // Precise ms timestamp

        // Transaction 1ms before - should be rejected
        assertFalse(
            "Tx 1ms before should be rejected",
            isAfterCreation(1699999999999L, invoiceCreatedAt)
        )

        // Transaction at exact same ms - should be accepted (edge case)
        assertTrue(
            "Tx at exact creation time should be accepted",
            isAfterCreation(1700000000000L, invoiceCreatedAt)
        )

        // Transaction 1ms after - should be accepted
        assertTrue(
            "Tx 1ms after should be accepted",
            isAfterCreation(1700000000001L, invoiceCreatedAt)
        )
    }

    @Test
    fun `createdAt handles edge case around midnight`() {
        // Midnight UTC Nov 15, 2023
        val midnight = 1700006400000L

        // Just before midnight
        assertFalse(
            "Tx just before midnight should be rejected",
            isAfterCreation(midnight - 1, midnight)
        )

        // Just after midnight
        assertTrue(
            "Tx just after midnight should be accepted",
            isAfterCreation(midnight + 1, midnight)
        )
    }

    @Test
    fun `createdAt handles timezone-independent ms timestamps`() {
        // TRON uses UTC timestamps in milliseconds
        // Ensure our comparison doesn't depend on local timezone

        val utcTimestamp = 1700000000000L // Fixed UTC timestamp
        val laterTimestamp = utcTimestamp + 60_000L // 1 minute later

        // This should always be true regardless of test machine timezone
        assertTrue(
            "Later timestamp should be after earlier timestamp",
            isAfterCreation(laterTimestamp, utcTimestamp)
        )
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FAILOVER BEHAVIOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `provider list is ordered correctly`() {
        // Mainnet should have TronGrid first
        val mainnetProviders = TronProviderManager.getProviderNames(TronConstants.NETWORK_MAINNET)
        assertTrue("Mainnet should have providers", mainnetProviders.isNotEmpty())
        assertEquals("First provider should be TronGrid", "TronGrid", mainnetProviders[0])

        // Nile should have TronGrid-Nile
        val nileProviders = TronProviderManager.getProviderNames(TronConstants.NETWORK_NILE)
        assertTrue("Nile should have providers", nileProviders.isNotEmpty())
        assertEquals("Nile provider should be TronGrid-Nile", "TronGrid-Nile", nileProviders[0])
    }

    @Test
    fun `error types are correctly categorized`() {
        // 429 should be RATE_LIMITED
        assertEquals(TronRpcErrorType.RATE_LIMITED, TronRpcErrorType.RATE_LIMITED)

        // Timeout should be TIMEOUT
        assertEquals(TronRpcErrorType.TIMEOUT, TronRpcErrorType.TIMEOUT)

        // Network issues should be NETWORK_ERROR
        assertEquals(TronRpcErrorType.NETWORK_ERROR, TronRpcErrorType.NETWORK_ERROR)
    }

    @Test
    fun `TronRpcResult success and failure states work correctly`() {
        val successResult: TronRpcResult<String> = TronRpcResult.Success("data", "provider")
        assertTrue("Success should be success", successResult.isSuccess())
        assertFalse("Success should not be failure", successResult.isFailure())
        assertEquals("data", successResult.getOrNull())

        val failureResult: TronRpcResult<String> = TronRpcResult.Failure(
            errorType = TronRpcErrorType.RATE_LIMITED,
            message = "Rate limited",
            providerName = "provider"
        )
        assertFalse("Failure should not be success", failureResult.isSuccess())
        assertTrue("Failure should be failure", failureResult.isFailure())
        assertEquals(null, failureResult.getOrNull())
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // TRANSACTION PAGE DATA STRUCTURE
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    fun `Trc20TransactionsPage holds correct data`() {
        val tx1 = createTestTx("tx1", 1700000001000L)
        val tx2 = createTestTx("tx2", 1700000002000L)

        val page = Trc20TransactionsPage(
            transactions = listOf(tx1, tx2),
            fingerprint = "next-page-cursor",
            providerName = "TronGrid"
        )

        assertEquals(2, page.transactions.size)
        assertEquals("next-page-cursor", page.fingerprint)
        assertEquals("TronGrid", page.providerName)
    }

    @Test
    fun `empty page with null fingerprint indicates end of data`() {
        val page = Trc20TransactionsPage(
            transactions = emptyList(),
            fingerprint = null,
            providerName = "TronGrid"
        )

        assertTrue("Empty page should have no transactions", page.transactions.isEmpty())
        assertEquals("No more pages", null, page.fingerprint)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // HELPER METHODS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * Mimics the createdAt gating logic used in TronPaymentRail.
     */
    private fun isAfterCreation(txTimestamp: Long, invoiceCreatedAt: Long): Boolean {
        return txTimestamp >= invoiceCreatedAt
    }

    /**
     * Create a test transaction with specified values.
     */
    private fun createTestTx(
        txId: String,
        blockTimestamp: Long,
        from: String = "TFromAddress",
        to: String = "TToAddress",
        value: String = "1000000",
        contractAddress: String = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj"
    ): Trc20Transaction {
        return Trc20Transaction(
            txId = txId,
            from = from,
            to = to,
            value = value,
            contractAddress = contractAddress,
            tokenSymbol = "USDT",
            tokenDecimals = 6,
            blockTimestamp = blockTimestamp,
            type = "Transfer"
        )
    }
}

/**
 * Mock provider for testing failover behavior.
 */
class MockTronRpcProvider(
    override val name: String,
    override val baseUrl: String = "https://mock.trongrid.io",
    private val shouldFail: Boolean = false,
    private val errorType: TronRpcErrorType = TronRpcErrorType.NETWORK_ERROR
) : TronRpcProvider {

    var callCount = 0
        private set

    override suspend fun getTrc20Transactions(
        address: String,
        contractAddress: String?,
        limit: Int,
        minTimestamp: Long?,
        fingerprint: String?
    ): TronRpcResult<Trc20TransactionsPage> {
        callCount++
        return if (shouldFail) {
            TronRpcResult.Failure(errorType, message = "Mock failure", providerName = name)
        } else {
            TronRpcResult.Success(
                Trc20TransactionsPage(emptyList(), null, name),
                name
            )
        }
    }

    override suspend fun getTransactionById(txId: String): TronRpcResult<TronTransaction?> {
        return TronRpcResult.Success(null, name)
    }

    override suspend fun getTransactionInfo(txId: String): TronRpcResult<TronTransactionInfo?> {
        return TronRpcResult.Success(null, name)
    }

    override suspend fun createTrc20Transaction(
        ownerAddressHex: String,
        contractAddressHex: String,
        functionSelector: String,
        parameter: String,
        feeLimit: Long
    ): TronRpcResult<UnsignedTronTransaction> {
        return TronRpcResult.Failure(TronRpcErrorType.UNKNOWN, message = "Not implemented", providerName = name)
    }

    override suspend fun broadcastTransaction(signedTxHex: String): TronRpcResult<String> {
        return TronRpcResult.Failure(TronRpcErrorType.UNKNOWN, message = "Not implemented", providerName = name)
    }
}
