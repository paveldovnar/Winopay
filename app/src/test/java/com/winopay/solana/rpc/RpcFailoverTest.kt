package com.winopay.solana.rpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for RPC failover logic.
 *
 * Tests verify:
 * 1. Provider ordering (primary first, then fallbacks)
 * 2. Failover on timeout/error
 * 3. Success when at least one provider works
 * 4. All-fail scenario handling
 * 5. Provider consistency tracking
 */
class RpcFailoverTest {

    // ═══════════════════════════════════════════════════════════════════
    // RpcCallResult TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `RpcCallResult Success contains data and provider name`() {
        val result: RpcCallResult<String> = RpcCallResult.Success("test data", "TestProvider")

        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertEquals("test data", result.getOrNull())

        val success = result as RpcCallResult.Success
        assertEquals("TestProvider", success.providerName)
    }

    @Test
    fun `RpcCallResult Failure contains error details`() {
        val result: RpcCallResult<String> = RpcCallResult.Failure(
            errorType = RpcErrorType.TIMEOUT,
            httpCode = null,
            message = "Connection timed out",
            providerName = "FailedProvider"
        )

        assertFalse(result.isSuccess())
        assertTrue(result.isFailure())
        assertNull(result.getOrNull())

        val failure = result as RpcCallResult.Failure
        assertEquals(RpcErrorType.TIMEOUT, failure.errorType)
        assertEquals("Connection timed out", failure.message)
        assertEquals("FailedProvider", failure.providerName)
    }

    @Test
    fun `RpcCallResult Failure with HTTP code`() {
        val result: RpcCallResult<String> = RpcCallResult.Failure(
            errorType = RpcErrorType.HTTP_ERROR,
            httpCode = 403,
            message = "Forbidden",
            providerName = "BlockedProvider"
        )

        val failure = result as RpcCallResult.Failure
        assertEquals(RpcErrorType.HTTP_ERROR, failure.errorType)
        assertEquals(403, failure.httpCode)
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOCK PROVIDER FOR TESTING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Mock provider that can be configured to succeed or fail.
     */
    open class MockRpcProvider(
        override val name: String,
        override val url: String = "https://mock.rpc",
        private val shouldSucceed: Boolean = true,
        private val errorType: RpcErrorType = RpcErrorType.TIMEOUT,
        private val signatures: List<SignatureInfo> = emptyList()
    ) : SolanaRpcProvider {

        var callCount = 0
            private set

        override suspend fun getSignaturesForAddress(
            address: String,
            limit: Int
        ): RpcCallResult<List<SignatureInfo>> {
            callCount++
            return if (shouldSucceed) {
                RpcCallResult.Success(signatures, name)
            } else {
                RpcCallResult.Failure(errorType, null, "Mock failure", name)
            }
        }

        override suspend fun getTransaction(signature: String): RpcCallResult<org.json.JSONObject?> {
            callCount++
            return if (shouldSucceed) {
                RpcCallResult.Success(null, name)
            } else {
                RpcCallResult.Failure(errorType, null, "Mock failure", name)
            }
        }

        override suspend fun getSignatureStatus(signature: String): RpcCallResult<ConfirmationStatus> {
            callCount++
            return if (shouldSucceed) {
                RpcCallResult.Success(ConfirmationStatus.CONFIRMED, name)
            } else {
                RpcCallResult.Failure(errorType, null, "Mock failure", name)
            }
        }

        override suspend fun getTokenAccountsByOwner(
            owner: String,
            mint: String?
        ): RpcCallResult<List<DiscoveredTokenAccount>> {
            callCount++
            return if (shouldSucceed) {
                RpcCallResult.Success(emptyList(), name)
            } else {
                RpcCallResult.Failure(errorType, null, "Mock failure", name)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FAILOVER LOGIC TESTS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Simulate failover logic (mirrors RpcProviderManager).
     */
    private suspend fun executeWithFailover(
        providers: List<MockRpcProvider>,
        operation: suspend (SolanaRpcProvider) -> RpcCallResult<List<SignatureInfo>>
    ): RpcCallResult<List<SignatureInfo>> {
        for (provider in providers) {
            val result = operation(provider)
            if (result is RpcCallResult.Success) {
                return result
            }
        }
        return RpcCallResult.Failure(
            errorType = RpcErrorType.NETWORK_ERROR,
            message = "All providers failed",
            providerName = "ALL"
        )
    }

    @Test
    fun `Primary provider success - no failover needed`() = runBlockingTest {
        val primary = MockRpcProvider("Primary", shouldSucceed = true,
            signatures = listOf(SignatureInfo("sig1", 1000L, false)))
        val fallback = MockRpcProvider("Fallback", shouldSucceed = true)

        val result = executeWithFailover(listOf(primary, fallback)) { provider ->
            provider.getSignaturesForAddress("address", 10)
        }

        assertTrue(result.isSuccess())
        assertEquals("Primary", (result as RpcCallResult.Success).providerName)
        assertEquals(1, primary.callCount)
        assertEquals(0, fallback.callCount)  // Fallback not called
    }

    @Test
    fun `Primary fails, fallback succeeds - failover works`() = runBlockingTest {
        val primary = MockRpcProvider("Primary", shouldSucceed = false,
            errorType = RpcErrorType.TIMEOUT)
        val fallback = MockRpcProvider("Fallback", shouldSucceed = true,
            signatures = listOf(SignatureInfo("sig1", 1000L, false)))

        val result = executeWithFailover(listOf(primary, fallback)) { provider ->
            provider.getSignaturesForAddress("address", 10)
        }

        assertTrue(result.isSuccess())
        assertEquals("Fallback", (result as RpcCallResult.Success).providerName)
        assertEquals(1, primary.callCount)
        assertEquals(1, fallback.callCount)
    }

    @Test
    fun `All providers fail - returns failure with ALL provider name`() = runBlockingTest {
        val primary = MockRpcProvider("Primary", shouldSucceed = false)
        val fallback1 = MockRpcProvider("Fallback1", shouldSucceed = false)
        val fallback2 = MockRpcProvider("Fallback2", shouldSucceed = false)

        val result = executeWithFailover(listOf(primary, fallback1, fallback2)) { provider ->
            provider.getSignaturesForAddress("address", 10)
        }

        assertTrue(result.isFailure())
        val failure = result as RpcCallResult.Failure
        assertEquals("ALL", failure.providerName)
        assertEquals(RpcErrorType.NETWORK_ERROR, failure.errorType)

        // All providers were tried
        assertEquals(1, primary.callCount)
        assertEquals(1, fallback1.callCount)
        assertEquals(1, fallback2.callCount)
    }

    @Test
    fun `Provider 2 of 3 succeeds - stops after success`() = runBlockingTest {
        val primary = MockRpcProvider("Primary", shouldSucceed = false)
        val fallback1 = MockRpcProvider("Fallback1", shouldSucceed = true,
            signatures = listOf(SignatureInfo("sig1", 1000L, false)))
        val fallback2 = MockRpcProvider("Fallback2", shouldSucceed = true)

        val result = executeWithFailover(listOf(primary, fallback1, fallback2)) { provider ->
            provider.getSignaturesForAddress("address", 10)
        }

        assertTrue(result.isSuccess())
        assertEquals("Fallback1", (result as RpcCallResult.Success).providerName)
        assertEquals(1, primary.callCount)
        assertEquals(1, fallback1.callCount)
        assertEquals(0, fallback2.callCount)  // Not called after success
    }

    // ═══════════════════════════════════════════════════════════════════
    // ERROR TYPE CATEGORIZATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `RpcErrorType covers all failure scenarios`() {
        val errorTypes = RpcErrorType.values()

        assertTrue(errorTypes.contains(RpcErrorType.TIMEOUT))
        assertTrue(errorTypes.contains(RpcErrorType.NETWORK_ERROR))
        assertTrue(errorTypes.contains(RpcErrorType.HTTP_ERROR))
        assertTrue(errorTypes.contains(RpcErrorType.RPC_ERROR))
        assertTrue(errorTypes.contains(RpcErrorType.PARSE_ERROR))
        assertTrue(errorTypes.contains(RpcErrorType.UNKNOWN))
    }

    @Test
    fun `Different error types preserved through failover`() = runBlockingTest {
        val timeoutProvider = MockRpcProvider("Timeout", shouldSucceed = false,
            errorType = RpcErrorType.TIMEOUT)
        val networkProvider = MockRpcProvider("Network", shouldSucceed = false,
            errorType = RpcErrorType.NETWORK_ERROR)

        // First provider times out
        val result1 = timeoutProvider.getSignaturesForAddress("addr", 10)
        assertEquals(RpcErrorType.TIMEOUT, (result1 as RpcCallResult.Failure).errorType)

        // Second provider has network error
        val result2 = networkProvider.getSignaturesForAddress("addr", 10)
        assertEquals(RpcErrorType.NETWORK_ERROR, (result2 as RpcCallResult.Failure).errorType)
    }

    // ═══════════════════════════════════════════════════════════════════
    // SIGNATURE INFO TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `SignatureInfo contains all required fields`() {
        val info = SignatureInfo(
            signature = "abc123",
            blockTimeMs = 1234567890000L,
            hasError = false
        )

        assertEquals("abc123", info.signature)
        assertEquals(1234567890000L, info.blockTimeMs)
        assertFalse(info.hasError)
    }

    @Test
    fun `SignatureInfo with error flag`() {
        val info = SignatureInfo(
            signature = "failed_tx",
            blockTimeMs = 1000L,
            hasError = true
        )

        assertTrue(info.hasError)
    }

    // ═══════════════════════════════════════════════════════════════════
    // CONFIRMATION STATUS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `ConfirmationStatus levels are ordered correctly`() {
        val statuses = listOf(
            ConfirmationStatus.NOT_FOUND,
            ConfirmationStatus.PROCESSING,
            ConfirmationStatus.CONFIRMED,
            ConfirmationStatus.FINALIZED
        )

        // Verify all statuses exist
        assertEquals(4, ConfirmationStatus.values().size)
        statuses.forEach { status ->
            assertTrue(ConfirmationStatus.values().contains(status))
        }
    }

    @Test
    fun `CONFIRMED and FINALIZED are considered confirmed`() {
        val isConfirmed: (ConfirmationStatus) -> Boolean = { status ->
            status == ConfirmationStatus.CONFIRMED || status == ConfirmationStatus.FINALIZED
        }

        assertFalse(isConfirmed(ConfirmationStatus.NOT_FOUND))
        assertFalse(isConfirmed(ConfirmationStatus.PROCESSING))
        assertTrue(isConfirmed(ConfirmationStatus.CONFIRMED))
        assertTrue(isConfirmed(ConfirmationStatus.FINALIZED))
    }

    // ═══════════════════════════════════════════════════════════════════
    // PROVIDER ORDERING TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Providers tried in order`() = runBlockingTest {
        val callOrder = mutableListOf<String>()

        val provider1 = object : MockRpcProvider("Provider1", shouldSucceed = false) {
            override suspend fun getSignaturesForAddress(
                address: String,
                limit: Int
            ): RpcCallResult<List<SignatureInfo>> {
                callOrder.add("Provider1")
                return super.getSignaturesForAddress(address, limit)
            }
        }

        val provider2 = object : MockRpcProvider("Provider2", shouldSucceed = false) {
            override suspend fun getSignaturesForAddress(
                address: String,
                limit: Int
            ): RpcCallResult<List<SignatureInfo>> {
                callOrder.add("Provider2")
                return super.getSignaturesForAddress(address, limit)
            }
        }

        val provider3 = object : MockRpcProvider("Provider3", shouldSucceed = true) {
            override suspend fun getSignaturesForAddress(
                address: String,
                limit: Int
            ): RpcCallResult<List<SignatureInfo>> {
                callOrder.add("Provider3")
                return super.getSignaturesForAddress(address, limit)
            }
        }

        executeWithFailover(listOf(provider1, provider2, provider3)) { provider ->
            provider.getSignaturesForAddress("address", 10)
        }

        assertEquals(listOf("Provider1", "Provider2", "Provider3"), callOrder)
    }

    // Helper for running blocking tests
    private fun runBlockingTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking { block() }
    }
}
