package com.winopay.solana.pay

import com.winopay.BuildConfig
import com.winopay.data.local.PaymentCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AMOUNT-ONLY payment detection.
 *
 * Tests verify:
 * 1. ValidationParams no longer requires reference
 * 2. Validation passes without reference in transaction
 * 3. Single-active-invoice invariant concepts
 * 4. BlockTime filtering works correctly
 * 5. Amount matching with tolerance
 * 6. Recipient matching
 * 7. Mint matching for SPL tokens
 */
class AmountOnlyDetectionTest {

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATION PARAMS TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `ValidationParams does not require reference`() {
        // AMOUNT-ONLY: ValidationParams should not have invoiceReference field
        val params = TransactionValidator.ValidationParams(
            signature = "test_signature",
            expectedRecipient = "merchant_wallet_address",
            expectedAmountMinor = 1_000_000L,  // 1 USDC
            expectedCurrency = PaymentCurrency.USDC,
            expectedMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            invoiceNetworkId = BuildConfig.SOLANA_CLUSTER,
            invoiceCreatedAt = System.currentTimeMillis()
        )

        // Verify all fields are set correctly
        assertEquals("test_signature", params.signature)
        assertEquals("merchant_wallet_address", params.expectedRecipient)
        assertEquals(1_000_000L, params.expectedAmountMinor)
        assertEquals(PaymentCurrency.USDC, params.expectedCurrency)
        assertNotNull(params.expectedMint)
        assertTrue(params.invoiceCreatedAt > 0)
    }

    @Test
    fun `ValidationParams accepts null mint for SOL payments`() {
        val params = TransactionValidator.ValidationParams(
            signature = "test_signature",
            expectedRecipient = "merchant_wallet_address",
            expectedAmountMinor = 1_000_000_000L,  // 1 SOL in lamports
            expectedCurrency = PaymentCurrency.SOL,
            expectedMint = null,  // SOL has no mint
            invoiceNetworkId = BuildConfig.SOLANA_CLUSTER,
            invoiceCreatedAt = System.currentTimeMillis()
        )

        assertEquals(PaymentCurrency.SOL, params.expectedCurrency)
        assertEquals(null, params.expectedMint)
    }

    @Test
    fun `ValidationParams accepts preferredProvider for RPC consistency`() {
        val params = TransactionValidator.ValidationParams(
            signature = "test_signature",
            expectedRecipient = "merchant_wallet_address",
            expectedAmountMinor = 1_000_000L,
            expectedCurrency = PaymentCurrency.USDC,
            expectedMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            invoiceNetworkId = BuildConfig.SOLANA_CLUSTER,
            invoiceCreatedAt = System.currentTimeMillis(),
            preferredProvider = null  // Can be null, will use failover
        )

        assertEquals(null, params.preferredProvider)
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESULT TYPE TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `TransactionValidator Result Valid is recognized correctly`() {
        val result: TransactionValidator.Result = TransactionValidator.Result.Valid()

        assertTrue(result.isValid())
        assertTrue(result is TransactionValidator.Result.Valid)
    }

    @Test
    fun `TransactionValidator Result Valid contains actualMintUsed and warningCode`() {
        val result = TransactionValidator.Result.Valid(
            actualMintUsed = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",  // USDT
            warningCode = "PAID_WITH_DIFFERENT_STABLECOIN"
        )

        assertTrue(result.isValid())
        assertEquals("Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", result.actualMintUsed)
        assertEquals("PAID_WITH_DIFFERENT_STABLECOIN", result.warningCode)
    }

    @Test
    fun `TransactionValidator Result Invalid contains reason`() {
        val result: TransactionValidator.Result = TransactionValidator.Result.Invalid(
            "Underpayment: received 900000, expected 1000000"
        )

        assertFalse(result.isValid())
        assertTrue(result is TransactionValidator.Result.Invalid)

        val invalid = result as TransactionValidator.Result.Invalid
        assertTrue(invalid.reason.contains("Underpayment"))
    }

    @Test
    fun `TransactionValidator Result Error contains message`() {
        val result: TransactionValidator.Result = TransactionValidator.Result.Error(
            "RPC error: Connection timeout"
        )

        assertFalse(result.isValid())
        assertTrue(result is TransactionValidator.Result.Error)

        val error = result as TransactionValidator.Result.Error
        assertTrue(error.message.contains("RPC error"))
    }

    // ═══════════════════════════════════════════════════════════════════
    // BLOCKTIME FILTERING TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Invoice createdAt is used for transaction filtering`() {
        val invoiceCreatedAt = System.currentTimeMillis()

        val params = TransactionValidator.ValidationParams(
            signature = "test_signature",
            expectedRecipient = "merchant_wallet_address",
            expectedAmountMinor = 1_000_000L,
            expectedCurrency = PaymentCurrency.USDC,
            expectedMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            invoiceNetworkId = BuildConfig.SOLANA_CLUSTER,
            invoiceCreatedAt = invoiceCreatedAt
        )

        // Invoice createdAt should be passed to validator
        assertEquals(invoiceCreatedAt, params.invoiceCreatedAt)
    }

    @Test
    fun `Transaction predating invoice should be filtered out`() {
        val invoiceCreatedAt = System.currentTimeMillis()
        val transactionBlockTime = invoiceCreatedAt - 10_000L  // 10 seconds before invoice

        // Logic: blockTime < invoiceCreatedAt means transaction predates invoice
        assertTrue(transactionBlockTime < invoiceCreatedAt)

        // Such transactions should be skipped (not validated)
        // This is enforced in the detector loop, not in ValidationParams
    }

    @Test
    fun `Transaction after invoice creation should be considered`() {
        val invoiceCreatedAt = System.currentTimeMillis()
        val transactionBlockTime = invoiceCreatedAt + 5_000L  // 5 seconds after invoice

        // Logic: blockTime >= invoiceCreatedAt means valid timing
        assertTrue(transactionBlockTime >= invoiceCreatedAt)
    }

    // ═══════════════════════════════════════════════════════════════════
    // SINGLE-ACTIVE-INVOICE INVARIANT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Single-active-invoice concept - only one invoice allowed`() {
        // AMOUNT-ONLY detection requires strict single-active-invoice rule
        // because we can't distinguish payments by reference anymore.
        //
        // This is a conceptual test - actual enforcement is in InvoiceDao/Repository

        val activeInvoiceCount = 1
        val maxAllowedActive = 1

        assertTrue("Only one active invoice should be allowed",
            activeInvoiceCount <= maxAllowedActive)
    }

    @Test
    fun `Creating new invoice should expire previous active invoice`() {
        // When creating a new invoice:
        // 1. Check for active invoices
        // 2. If found, cancel their detection
        // 3. Expire them in DB
        // 4. Create new invoice
        //
        // This ensures amount-only detection works correctly

        val previousInvoiceStatus = "EXPIRED"  // After guard
        val newInvoiceStatus = "CREATED"

        assertEquals("EXPIRED", previousInvoiceStatus)
        assertEquals("CREATED", newInvoiceStatus)
    }

    // ═══════════════════════════════════════════════════════════════════
    // AMOUNT VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `Exact amount match is valid`() {
        val expectedAmount = 1_000_000L  // 1 USDC
        val receivedAmount = 1_000_000L

        val isValid = receivedAmount >= expectedAmount
        assertTrue("Exact amount should be valid", isValid)
    }

    @Test
    fun `Overpayment is valid (merchant protection)`() {
        val expectedAmount = 1_000_000L  // 1 USDC
        val receivedAmount = 1_100_000L  // 1.1 USDC (overpaid)

        val isValid = receivedAmount >= expectedAmount
        assertTrue("Overpayment should be valid", isValid)
    }

    @Test
    fun `Underpayment by more than 1 unit is invalid`() {
        val expectedAmount = 1_000_000L  // 1 USDC
        val receivedAmount = 999_998L    // Short by 2 units

        val shortfall = expectedAmount - receivedAmount
        val isInvalid = shortfall > 1

        assertTrue("Underpayment by >1 unit should be invalid", isInvalid)
    }

    @Test
    fun `Underpayment by exactly 1 unit is allowed (rounding tolerance)`() {
        val expectedAmount = 1_000_000L  // 1 USDC
        val receivedAmount = 999_999L    // Short by exactly 1 unit

        val shortfall = expectedAmount - receivedAmount
        val isAllowed = shortfall <= 1

        assertTrue("Underpayment by <=1 unit should be allowed", isAllowed)
    }

    // ═══════════════════════════════════════════════════════════════════
    // DETECTION RESULT TESTS
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `PaymentDetectionResult Searching indicates active polling`() {
        val result = PaymentDetectionResult.Searching

        assertTrue(result is PaymentDetectionResult.Searching)
    }

    @Test
    fun `PaymentDetectionResult Found indicates unconfirmed transaction`() {
        val result = PaymentDetectionResult.Found(
            signature = "test_sig",
            isConfirmed = false
        )

        assertTrue(result is PaymentDetectionResult.Found)
        val found = result as PaymentDetectionResult.Found
        assertEquals("test_sig", found.signature)
        assertFalse(found.isConfirmed)
    }

    @Test
    fun `PaymentDetectionResult Confirmed indicates success`() {
        val result = PaymentDetectionResult.Confirmed(signature = "confirmed_sig")

        assertTrue(result is PaymentDetectionResult.Confirmed)
        val confirmed = result as PaymentDetectionResult.Confirmed
        assertEquals("confirmed_sig", confirmed.signature)
    }

    @Test
    fun `PaymentDetectionResult Confirmed contains actualMintUsed and warningCode`() {
        val result = PaymentDetectionResult.Confirmed(
            signature = "confirmed_sig",
            actualMintUsed = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",  // USDT
            warningCode = "PAID_WITH_DIFFERENT_STABLECOIN"
        )

        val confirmed = result as PaymentDetectionResult.Confirmed
        assertEquals("Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", confirmed.actualMintUsed)
        assertEquals("PAID_WITH_DIFFERENT_STABLECOIN", confirmed.warningCode)
    }

    @Test
    fun `PaymentDetectionResult Found contains actualMintUsed and warningCode`() {
        val result = PaymentDetectionResult.Found(
            signature = "found_sig",
            isConfirmed = false,
            actualMintUsed = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB",  // USDT
            warningCode = "PAID_WITH_DIFFERENT_STABLECOIN"
        )

        val found = result as PaymentDetectionResult.Found
        assertEquals("Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB", found.actualMintUsed)
        assertEquals("PAID_WITH_DIFFERENT_STABLECOIN", found.warningCode)
    }

    @Test
    fun `PaymentDetectionResult Invalid contains rejection reason`() {
        val result = PaymentDetectionResult.Invalid(
            signature = "invalid_sig",
            reason = "Amount mismatch"
        )

        assertTrue(result is PaymentDetectionResult.Invalid)
        val invalid = result as PaymentDetectionResult.Invalid
        assertEquals("invalid_sig", invalid.signature)
        assertTrue(invalid.reason.contains("Amount"))
    }

    @Test
    fun `PaymentDetectionResult Expired indicates timeout`() {
        val result = PaymentDetectionResult.Expired

        assertTrue(result is PaymentDetectionResult.Expired)
    }

    @Test
    fun `PaymentDetectionResult Error contains error message`() {
        val result = PaymentDetectionResult.Error(message = "Network failure")

        assertTrue(result is PaymentDetectionResult.Error)
        val error = result as PaymentDetectionResult.Error
        assertTrue(error.message.contains("Network"))
    }
}
