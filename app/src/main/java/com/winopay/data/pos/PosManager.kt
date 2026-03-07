package com.winopay.data.pos

import android.content.Context
import android.util.Log
import com.winopay.BuildConfig
import com.winopay.WinoPayApplication
import com.winopay.data.CurrencyConverter
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import com.winopay.data.local.PaymentCurrency
import com.winopay.data.profile.MerchantProfileStore
import com.winopay.data.worker.DetectionScheduler
import com.winopay.payments.DetectionResult
import com.winopay.payments.PaymentRail
import com.winopay.payments.PaymentRailFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Result of the critical section in selectPaymentMethod().
 * Used to pass data out of the mutex-protected block.
 */
private sealed class CriticalSectionResult {
    data class Success(val invoice: InvoiceEntity, val usdcDisplayAmount: Double) : CriticalSectionResult()
    data class Blocked(val error: String) : CriticalSectionResult()
    data class Error(val error: String) : CriticalSectionResult()
}

/**
 * Centralized POS flow manager for Solana Pay.
 *
 * AMOUNT-ONLY DETECTION:
 * Since some wallets don't include the reference/memo from QR,
 * detection is based on AMOUNT + MINT + RECIPIENT only.
 *
 * CRITICAL: Only ONE invoice can be active at a time.
 * This ensures amount-based detection doesn't match wrong payments.
 *
 * Flow:
 * 1. Merchant enters amount -> createInvoice()
 * 2. Ensure no active invoice exists (single-invoice rule)
 * 3. Merchant selects payment method -> selectPaymentMethod()
 * 4. QR displayed with Solana Pay URL
 * 5. Customer scans and pays from their wallet
 * 6. PosManager detects payment via MERCHANT ADDRESS polling (not reference)
 * 7. Validate by AMOUNT + MINT + createdAt timestamp
 * 8. Invoice confirmed/failed/expired
 */
class PosManager(private val context: Context) {

    companion object {
        private const val TAG = "PosManager"
    }

    // Use central timeout constant
    private val invoiceTimeoutMs: Long = InvoiceTimeouts.DEFAULT_TIMEOUT_MS

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val profileStore = MerchantProfileStore(context)

    /**
     * Payment rail abstraction. Dynamically selects based on active rail in profile store.
     * Falls back to Solana if no active rail is configured.
     *
     * NOTE: Uses runBlocking to synchronously get the active rail. This is acceptable
     * because the rail lookup is very fast (DataStore cached read).
     */
    private val paymentRail: PaymentRail
        get() = getActivePaymentRail()

    /**
     * Get the active payment rail based on stored profile.
     * Falls back to default Solana rail if no active rail is set.
     */
    private fun getActivePaymentRail(): PaymentRail {
        return try {
            val activeRail = runBlocking { profileStore.getActiveRail() }
            if (activeRail != null) {
                Log.i(TAG, "RAIL|ACTIVE|railId=${activeRail.railId}|networkId=${activeRail.networkId}|account=${activeRail.accountId}")
                PaymentRailFactory.getRailByRailId(activeRail.railId, activeRail.networkId)
            } else {
                Log.d(TAG, "RAIL|DEFAULT|solana")
                PaymentRailFactory.getDefaultRail()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RAIL|ERROR|fallback=solana|error=${e.message}")
            PaymentRailFactory.getDefaultRail()
        }
    }

    /**
     * Mutex for atomic invoice creation.
     * Ensures only ONE invoice can be created at a time (check + insert + schedule).
     * Prevents race condition where two rapid selectPaymentMethod() calls
     * could both pass the activeInvoice check and create duplicate invoices.
     */
    private val invoiceCreationMutex = Mutex()

    private val _state = MutableStateFlow<PosState>(PosState.EnterAmount)
    val state: StateFlow<PosState> = _state.asStateFlow()

    private var timeoutJob: Job? = null

    // Current invoice data
    private var currentInvoice: InvoiceEntity? = null
    private var currentQrData: String? = null
    private var expiresAt: Long = 0L

    /**
     * Reset to initial state.
     */
    fun reset() {
        cancelJobs()
        currentInvoice = null
        currentQrData = null
        expiresAt = 0L
        _state.value = PosState.EnterAmount
        Log.d(TAG, "Reset to EnterAmount")
    }

    /**
     * Create invoice and transition to SelectPayment.
     * Does NOT create database record yet - that happens in selectPaymentMethod.
     *
     * NOTE: This doesn't check for active invoices (that's done in selectPaymentMethod).
     * If an active invoice exists, selectPaymentMethod will block and transition
     * to BlockedByActiveInvoice state.
     */
    fun createInvoice(amount: Double): String {
        Log.d(TAG, "createInvoice() called with amount: $amount")
        cancelJobs()

        val invoiceId = UUID.randomUUID().toString().take(8)
        Log.d(TAG, "Generated invoiceId: $invoiceId")

        val newState = PosState.SelectPayment(
            amount = amount,
            invoiceId = invoiceId
        )
        Log.d(TAG, "Transitioning state: ${_state.value} -> $newState")
        _state.value = newState

        Log.d(TAG, "Created invoice: $invoiceId for amount: $amount, current state: ${_state.value}")
        return invoiceId
    }

    /**
     * Select payment method and generate QR code.
     *
     * This creates the invoice in Room, generates Solana Pay URL,
     * and starts payment detection.
     *
     * CRITICAL: Blocks invoice creation if exchange rates are unavailable.
     * CRITICAL: Only ONE invoice can be active at a time (amount-only detection).
     *
     * MUTEX SCOPE: The mutex ONLY covers the atomic section:
     * - Re-check active invoice
     * - Save invoice to Room
     * - Schedule background detection
     *
     * FX conversion and other prep work happens OUTSIDE the mutex to avoid
     * blocking concurrent calls during slow operations.
     *
     * @param method Payment method (USDC, etc.)
     * @param recipientPublicKey Merchant wallet address
     * @param businessName Business name for label
     * @return Error message if failed, null if success
     */
    suspend fun selectPaymentMethod(
        method: PaymentMethod,
        recipientPublicKey: String,
        businessName: String
    ): String? {
        Log.d(TAG, "selectPaymentMethod() called: method=${method.displayName}, recipient=$recipientPublicKey, business=$businessName")

        val currentState = _state.value
        Log.d(TAG, "Current state: $currentState")

        if (currentState !is PosState.SelectPayment) {
            Log.e(TAG, "Invalid state for selectPaymentMethod: $currentState")
            return "Invalid state"
        }

        val app = WinoPayApplication.instance
        val invoiceId = currentState.invoiceId
        val inputAmount = currentState.amount
        Log.d(TAG, "Processing invoice: id=$invoiceId, inputAmount=$inputAmount")

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // PHASE 1: PREPARATION (OUTSIDE MUTEX)
        // FX conversion, ATA derivation, and other prep work.
        // These operations may involve disk I/O or computation but don't
        // need atomicity with the database insert.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        // Get merchant's currency setting (may involve disk I/O)
        val merchantCurrency = app.dataStoreManager.currency.first()

        // ━━━━━━━━━━ FX DIAGNOSTIC LOG ━━━━━━━━━━
        val snapshotInfo = CurrencyConverter.getSnapshotInfo()
        Log.d(TAG, "━━━━━━━━ FX DIAGNOSTIC ━━━━━━━━")
        Log.d(TAG, "  merchantCurrency: $merchantCurrency")
        Log.d(TAG, "  requiresRates(currency): ${CurrencyConverter.requiresRates(merchantCurrency)}")
        Log.d(TAG, "  hasValidRates(): ${CurrencyConverter.hasValidRates()}")
        Log.d(TAG, "  snapshot: ${snapshotInfo?.let { "${it.provider}, ${it.date}, ${it.getAgeString()}" } ?: "NULL"}")
        Log.d(TAG, "  validateRates(): ${CurrencyConverter.validateRates() ?: "OK"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ━━━━━━━━━━ USD BYPASS: NEVER REQUIRE FX ━━━━━━━━━━
        // CRITICAL: USD merchants can ALWAYS create invoices
        // 1 USD = 1 USDC exactly, no FX conversion needed
        val requiresRates = CurrencyConverter.requiresRates(merchantCurrency)

        if (requiresRates) {
            // ━━━━━━━━━━ STRICT RATE VALIDATION (NON-USD ONLY) ━━━━━━━━━━
            // CRITICAL: Block invoice creation if rates unavailable
            val rateValidation = CurrencyConverter.validateRates()
            if (rateValidation != null) {
                Log.e(TAG, "━━━━━ FX_BLOCK_001: INVOICE BLOCKED ━━━━━")
                Log.e(TAG, "  Error code: FX_BLOCK_001")
                Log.e(TAG, "  Reason: $rateValidation")
                Log.e(TAG, "  Currency: $merchantCurrency (requires FX)")
                Log.e(TAG, "  Action: Show retry button, check network")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                _state.value = PosState.RateError(rateValidation)
                return rateValidation
            }
        } else {
            Log.d(TAG, "━━━━━ USD BYPASS ACTIVE ━━━━━")
            Log.d(TAG, "  Currency: $merchantCurrency")
            Log.d(TAG, "  1 USD = 1 USDC (no FX needed)")
            Log.d(TAG, "  Invoice creation: ALLOWED (even offline)")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        // ━━━━━━━━━━ CONVERSION PIPELINE (BigDecimal) ━━━━━━━━━━
        // Convert display amount to minor units
        // All currencies use 2 decimal places for now (cents, kopeks, etc.)
        val fiatDecimals = 2
        val fiatAmountMinor = (inputAmount * 100).toLong()
        Log.d(TAG, "Converted $inputAmount $merchantCurrency -> $fiatAmountMinor minor units")

        // Get conversion result with full metadata (immutable FX snapshot)
        val conversionResult = CurrencyConverter.convertToUsdcMinorWithMetadata(
            fiatAmountMinor = fiatAmountMinor,
            fiatCurrency = merchantCurrency,
            fiatDecimals = fiatDecimals
        )

        if (conversionResult == null) {
            val error = "No exchange rate available for $merchantCurrency"
            Log.e(TAG, "━━━━━ FX_BLOCK_002: CONVERSION FAILED ━━━━━")
            Log.e(TAG, "  Error code: FX_BLOCK_002")
            Log.e(TAG, "  Reason: $error")
            Log.e(TAG, "  Currency: $merchantCurrency")
            Log.e(TAG, "  This should NOT happen for USD!")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            _state.value = PosState.RateError(error)
            return error
        }

        // Generate unique reference for payment detection
        val reference = paymentRail.generateReference()
        Log.d(TAG, "Generated reference: $reference")

        // MULTICHAIN: Use railId and tokenId from the selected PaymentMethod
        val railId = method.railId
        val tokenId = method.tokenId
        val tokenSymbol = method.symbol

        // Get the payment rail for this method
        val methodRail = try {
            val connection = runBlocking { profileStore.getRailConnection(railId) }
            if (connection != null) {
                PaymentRailFactory.getRailByRailId(railId, connection.networkId)
            } else {
                paymentRail // Fallback to active rail
            }
        } catch (e: Exception) {
            Log.w(TAG, "INVOICE|RAIL_FALLBACK|railId=$railId|error=${e.message}")
            paymentRail
        }
        val networkId = methodRail.networkId

        // Verify the token is supported on this rail
        val supportedTokens = methodRail.getSupportedTokens()
        val selectedToken = supportedTokens.find { it.tokenId == tokenId }
        if (selectedToken == null || !selectedToken.isEnabled) {
            Log.e(TAG, "INVOICE|CREATE|BLOCKED|method=$tokenSymbol not available on rail=$railId")
            return "Payment method $tokenSymbol is not available on this network"
        }

        val currency: PaymentCurrency = when (tokenSymbol) {
            "USDC" -> PaymentCurrency.USDC
            "USDT" -> PaymentCurrency.USDT
            else -> PaymentCurrency.USDC // Fallback
        }
        val splTokenMint = tokenId // Token contract/mint address from the method
        val amountRaw: Long = conversionResult.usdcMinor

        // Derive token address (ATA for Solana, wallet for TRON)
        var recipientTokenAccount: String? = null
        recipientTokenAccount = paymentRail.deriveTokenAddress(
            ownerWallet = recipientPublicKey,
            tokenMint = splTokenMint
        )

        Log.d(TAG, "━━━━━ TOKEN ADDRESS DERIVED ━━━━━")
        Log.d(TAG, "  Rail: $railId")
        Log.d(TAG, "  Network: $networkId")
        Log.d(TAG, "  Token: $tokenSymbol")
        Log.d(TAG, "  Token contract: $splTokenMint")
        Log.d(TAG, "  Owner wallet: $recipientPublicKey")
        Log.d(TAG, "  Recipient address: $recipientTokenAccount")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Calculate decimal amount string for Solana Pay URL
        val amountDecimalString = String.format(java.util.Locale.US, "%.6f", amountRaw.toDouble() / 1_000_000.0)
        val usdcDisplayAmount = amountRaw.toDouble() / 1_000_000.0

        // Sanity check: if USD merchant with $1.00 input, must produce "1.000000"
        if (merchantCurrency == "USD" && inputAmount == 1.0) {
            if (amountDecimalString != "1.000000") {
                Log.e(TAG, "SANITY CHECK FAILED: USD $1.00 input produced '$amountDecimalString' instead of '1.000000'")
            } else {
                Log.d(TAG, "SANITY CHECK PASSED: USD $1.00 -> $amountDecimalString")
            }
        }

        Log.d(TAG, "━━━━━ INVOICE PREPARED ━━━━━")
        Log.d(TAG, "  CLUSTER: ${BuildConfig.SOLANA_CLUSTER}")
        Log.d(TAG, "  FIAT_INPUT: $inputAmount $merchantCurrency")
        Log.d(TAG, "  FIAT_MINOR: ${conversionResult.fiatAmountMinor} ($fiatDecimals decimals)")
        Log.d(TAG, "  RATE_USED: ${conversionResult.rateUsed} (${conversionResult.rateDirection})")
        Log.d(TAG, "  RATE_PROVIDER: ${conversionResult.provider}")
        Log.d(TAG, "  RATE_DATE: ${conversionResult.rateDate}")
        Log.d(TAG, "  USD_PRECISE: ${conversionResult.usdPrecise}")
        Log.d(TAG, "  SLIPPAGE_APPLIED: ${conversionResult.slippageApplied}")
        Log.d(TAG, "  USDC_MINOR: $amountRaw")
        Log.d(TAG, "  ROUNDING_MODE: ${conversionResult.roundingMode}")
        Log.d(TAG, "  TIMESTAMP: ${System.currentTimeMillis()}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Create invoice entity with IMMUTABLE FX rate snapshot
        Log.d(TAG, "Creating InvoiceEntity with immutable FX snapshot...")
        val preparedInvoice = InvoiceEntity.create(
            id = invoiceId,
            reference = reference,
            recipientAddress = recipientPublicKey,
            recipientTokenAccount = recipientTokenAccount,  // ATA for SPL, null for SOL
            amount = amountRaw,
            currency = currency,
            splTokenMint = splTokenMint,
            railId = railId,       // MULTICHAIN: Use active rail
            networkId = networkId, // MULTICHAIN: Use active network
            label = businessName,
            message = "Payment to $businessName",
            // IMMUTABLE FX snapshot (frozen at invoice creation, NEVER recomputed)
            fiatAmountMinor = conversionResult.fiatAmountMinor,
            fiatCurrency = conversionResult.fiatCurrency,
            fiatDecimals = conversionResult.fiatDecimals,
            rateUsed = conversionResult.rateUsed,  // Exact BigDecimal string
            rateDirection = conversionResult.rateDirection,
            rateProvider = conversionResult.provider,
            rateDate = conversionResult.rateDate,
            rateFetchedAt = conversionResult.rateFetchedAt
        )

        // ━━━━━ INVOICE|CREATE DIAGNOSTIC LOG ━━━━━
        Log.i(TAG, "INVOICE|CREATE|railId=$railId|networkId=$networkId|tokenId=$splTokenMint|amountMinor=$amountRaw|recipient=$recipientPublicKey")
        Log.d(TAG, "Invoice prepared with immutable FX snapshot")
        Log.d(TAG, "━━━━━ INVOICE DETECTION CONFIG ━━━━━")
        Log.d(TAG, "  Invoice ID: ${preparedInvoice.id}")
        Log.d(TAG, "  Currency: ${preparedInvoice.currency}")
        Log.d(TAG, "  Expected mint: ${preparedInvoice.splTokenMint ?: "SOL (native)"}")
        Log.d(TAG, "  Owner wallet: ${preparedInvoice.recipientAddress}")
        Log.d(TAG, "  Polling address: ${preparedInvoice.getPollingAddress()}")
        Log.d(TAG, "  Polling type: ${if (preparedInvoice.requiresAtaPolling()) "ATA (token account)" else "WALLET (direct)"}")
        if (preparedInvoice.splTokenMint != null) {
            Log.d(TAG, "  ATA derivation: owner=${preparedInvoice.recipientAddress.take(8)}... + mint=${preparedInvoice.splTokenMint.take(8)}...")
        }
        Log.d(TAG, "  Amount (minor units): ${preparedInvoice.amount}")
        Log.d(TAG, "  Amount (display): ${preparedInvoice.getDisplayAmount()}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // PHASE 2: CRITICAL SECTION (INSIDE MUTEX)
        // ONLY the atomic check + insert + schedule operations.
        // This is the minimal section that MUST be atomic to prevent
        // duplicate invoice creation from concurrent calls.
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        val criticalSectionResult = invoiceCreationMutex.withLock {
            Log.d(TAG, "━━━━━ INVOICE CREATION MUTEX ACQUIRED ━━━━━")

            // ━━━━━━━━━━ STRICT SINGLE-INVOICE GUARD ━━━━━━━━━━
            // CRITICAL: Amount-only detection requires sequential payments.
            // BLOCKING MODEL: Do NOT auto-expire - force merchant to handle it.
            val activeInvoice = app.invoiceRepository.getActiveInvoice()
            if (activeInvoice != null) {
                Log.e(TAG, "━━━━━ SINGLE-INVOICE GUARD: BLOCKED ━━━━━")
                Log.e(TAG, "  Found active invoice: ${activeInvoice.id}")
                Log.e(TAG, "  Status: ${activeInvoice.status}")
                Log.e(TAG, "  Amount: ${activeInvoice.getDisplayAmount()}")
                Log.e(TAG, "  Action: BLOCKING new invoice creation")
                Log.e(TAG, "  Merchant must: Resume or Cancel active payment")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Transition to blocked state - UI will show dialog
                _state.value = PosState.BlockedByActiveInvoice(
                    activeInvoiceId = activeInvoice.id,
                    activeAmount = activeInvoice.getDisplayAmount(),
                    activeStatus = activeInvoice.status.name
                )
                Log.d(TAG, "━━━━━ INVOICE CREATION MUTEX RELEASED (BLOCKED) ━━━━━")
                return@withLock CriticalSectionResult.Blocked("Active payment in progress")
            }

            // Set expiry INSIDE mutex to ensure consistency
            expiresAt = System.currentTimeMillis() + invoiceTimeoutMs
            Log.d(TAG, "Set expiry: $expiresAt (${invoiceTimeoutMs}ms from now)")

            // Store as current invoice WITH deadline
            val invoiceWithDeadline = preparedInvoice.withDeadline(expiresAt)
            currentInvoice = invoiceWithDeadline

            // Save to database (atomic with check above)
            Log.d(TAG, "Saving invoice to Room database...")
            app.invoiceRepository.saveInvoice(invoiceWithDeadline)
            Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceWithDeadline.id.take(8)}|INITIAL_DEADLINE=$expiresAt")
            Log.d(TAG, "Invoice saved to database")

            // Schedule background detection with WorkManager (atomic with save)
            DetectionScheduler.scheduleDetection(
                context = app,
                invoice = preparedInvoice,
                timeoutMs = invoiceTimeoutMs
            )

            Log.d(TAG, "━━━━━ INVOICE CREATION MUTEX RELEASED ━━━━━")
            CriticalSectionResult.Success(preparedInvoice, usdcDisplayAmount)
        }
        // ━━━━━━━━━━ END CRITICAL SECTION ━━━━━━━━━━

        // Handle critical section result
        when (criticalSectionResult) {
            is CriticalSectionResult.Blocked -> return criticalSectionResult.error
            is CriticalSectionResult.Error -> return criticalSectionResult.error
            is CriticalSectionResult.Success -> {
                // Continue with QR generation and detection
            }
        }

        // Use the prepared invoice from phase 1 (already saved in phase 2)
        val invoice = preparedInvoice

        // Build Solana Pay URL
        Log.d(TAG, "Building Solana Pay URL...")
        currentQrData = paymentRail.buildPaymentRequest(invoice)

        // ━━━━━ AMOUNT CONSISTENCY ASSERTION (DEV ONLY) ━━━━━
        // CRITICAL: Verify URL amount matches DB amount exactly
        if (BuildConfig.DEBUG) {
            val urlAmountString = invoice.getAmountString()
            val urlAmountBD = java.math.BigDecimal(urlAmountString)
            val urlAmountMinor = urlAmountBD.multiply(java.math.BigDecimal(1_000_000)).longValueExact()

            Log.d(TAG, "━━━━━ AMOUNT CONSISTENCY CHECK ━━━━━")
            Log.d(TAG, "  fiatInput: ${invoice.getDisplayAmount()} ${invoice.fiatCurrency}")
            Log.d(TAG, "  fiatMinor: ${invoice.fiatAmountMinor}")
            Log.d(TAG, "  rateUsed: ${invoice.rateUsed}")
            Log.d(TAG, "  usdcMinor (DB): ${invoice.amount}")
            Log.d(TAG, "  urlAmountString: $urlAmountString")
            Log.d(TAG, "  urlAmountMinor (parsed): $urlAmountMinor")
            Log.d(TAG, "  expectedAmountMinor: ${invoice.amount}")

            if (urlAmountMinor != invoice.amount) {
                Log.e(TAG, "━━━━━ AMOUNT MISMATCH ERROR ━━━━━")
                Log.e(TAG, "  ERROR: URL amount != DB amount!")
                Log.e(TAG, "  urlAmountMinor: $urlAmountMinor")
                Log.e(TAG, "  invoice.amount: ${invoice.amount}")
                Log.e(TAG, "  DIFFERENCE: ${urlAmountMinor - invoice.amount}")
                Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                // In debug builds, this is a critical bug - fail fast
                throw IllegalStateException(
                    "AMOUNT MISMATCH: URL amount ($urlAmountMinor) != DB amount (${invoice.amount})"
                )
            }
            Log.d(TAG, "  ✓ AMOUNTS MATCH: $urlAmountMinor == ${invoice.amount}")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }

        // ━━━━━ SOLANA PAY URL DEBUG ━━━━━
        Log.d(TAG, "━━━━━ SOLANA PAY URL ━━━━━")
        Log.d(TAG, "  FULL URL: $currentQrData")
        Log.d(TAG, "  Copy this URL to test in Solflare/Phantom:")
        Log.d(TAG, "  $currentQrData")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Validate URL structure
        val urlParts = currentQrData?.split("?")
        if (urlParts != null && urlParts.size == 2) {
            val recipient = urlParts[0].removePrefix("solana:")
            val params = urlParts[1]
            Log.d(TAG, "  URL recipient: $recipient")
            Log.d(TAG, "  URL params: $params")
        }

        if (currentQrData.isNullOrEmpty()) {
            Log.e(TAG, "CRITICAL: SolanaPayUrlBuilder returned null/empty URL!")
            return "QR code generation failed"
        }

        // Transition to QR state
        val qrState = PosState.Qr(
            invoiceId = invoiceId,
            amount = inputAmount,
            usdcAmount = usdcDisplayAmount,
            method = method,
            qrData = currentQrData!!,
            expiresAt = expiresAt
        )
        Log.d(TAG, "Transitioning to QR state: $qrState")
        _state.value = qrState
        Log.d(TAG, "State transition complete, new state: ${_state.value}")

        Log.d(TAG, "Selected payment method: ${method.displayName}, starting detection")

        // Start payment detection
        Log.d(TAG, "Starting payment detection for invoice ${invoice.id}...")
        startPaymentDetection(invoice)

        // Start timeout
        Log.d(TAG, "Starting timeout job...")
        startTimeout(invoiceId)
        Log.d(TAG, "selectPaymentMethod() completed successfully")

        return null // Success
    }

    /**
     * Cancel current invoice explicitly by merchant.
     *
     * This is a DESTRUCTIVE action that:
     * 1. Stops foreground detection
     * 2. Cancels background WorkManager job
     * 3. Marks invoice as CANCELED in Room (not EXPIRED)
     * 4. Resets UI state to EnterAmount
     *
     * Call this when merchant explicitly cancels via dialog confirmation.
     */
    fun cancelInvoice() {
        val invoice = currentInvoice
        Log.d(TAG, "━━━━━ MERCHANT CANCEL ━━━━━")
        Log.d(TAG, "  Invoice: ${invoice?.id}")
        Log.d(TAG, "  Action: Explicit cancel by merchant")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Mark as CANCELED (not EXPIRED) in database
        invoice?.let {
            // Stop foreground detection
            paymentRail.cancelForegroundDetection(it.id)

            // Cancel background worker
            DetectionScheduler.cancelDetection(WinoPayApplication.instance, it.id)

            // Mark as CANCELED in Room
            scope.launch(Dispatchers.IO) {
                WinoPayApplication.instance.invoiceRepository.cancelInvoice(it.id)
            }
        }

        reset()
    }

    /**
     * Cancel a blocked invoice and reset to EnterAmount.
     *
     * Called from BlockedByActiveInvoice state when merchant chooses
     * to cancel the blocking invoice and start fresh.
     *
     * @param invoiceId The invoice ID from BlockedByActiveInvoice state
     */
    fun cancelBlockedInvoice(invoiceId: String) {
        Log.d(TAG, "━━━━━ CANCEL BLOCKED INVOICE ━━━━━")
        Log.d(TAG, "  Invoice: $invoiceId")
        Log.d(TAG, "  Action: Cancel blocked invoice, allow new payment")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val app = WinoPayApplication.instance

        // Stop any active detection
        paymentRail.cancelForegroundDetection(invoiceId)
        DetectionScheduler.cancelDetection(app, invoiceId)

        // Mark as CANCELED in Room
        scope.launch(Dispatchers.IO) {
            app.invoiceRepository.cancelInvoice(invoiceId)
        }

        // Reset to allow new payment
        reset()
    }

    /**
     * Resume a blocked invoice - go back to its active state.
     *
     * Called from BlockedByActiveInvoice state when merchant chooses
     * to continue with the existing payment instead of creating new.
     *
     * @param invoiceId The invoice ID from BlockedByActiveInvoice state
     * @return true if successfully resumed, false if invoice not found
     */
    suspend fun resumeBlockedInvoice(invoiceId: String): Boolean {
        Log.d(TAG, "━━━━━ RESUME BLOCKED INVOICE ━━━━━")
        Log.d(TAG, "  Invoice: $invoiceId")
        Log.d(TAG, "  Action: Resume blocked invoice")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val app = WinoPayApplication.instance
        val invoice = app.invoiceRepository.getInvoice(invoiceId)

        if (invoice == null) {
            Log.e(TAG, "Cannot resume: invoice not found")
            reset()
            return false
        }

        // Use existing restore logic
        currentInvoice = invoice

        // Calculate remaining time
        val elapsedMs = System.currentTimeMillis() - invoice.createdAt
        val remainingMs = invoiceTimeoutMs - elapsedMs

        if (remainingMs <= 0) {
            // Timeout already passed - show soft-expire dialog
            Log.d(TAG, "Invoice timeout passed during resume - showing soft-expire")
            currentInvoice = invoice
            currentQrData = paymentRail.buildPaymentRequest(invoice)

            val elapsedSeconds = (elapsedMs / 1000).toInt()
            _state.value = PosState.ExpiredAwaitingDecision(
                invoiceId = invoice.id,
                amount = invoice.getDisplayAmount(),
                elapsedSeconds = elapsedSeconds,
                qrData = currentQrData ?: ""
            )
            return true
        }

        expiresAt = System.currentTimeMillis() + remainingMs

        // Restore UI state based on invoice status
        when (invoice.status) {
            InvoiceStatus.CREATED -> {
                currentQrData = paymentRail.buildPaymentRequest(invoice)

                _state.value = PosState.Qr(
                    invoiceId = invoice.id,
                    amount = invoice.getDisplayAmount(),
                    usdcAmount = invoice.getDisplayAmount(),
                    method = PaymentMethod.fromCurrency(invoice.currency, invoice.railId, invoice.splTokenMint ?: ""),
                    qrData = currentQrData!!,
                    expiresAt = expiresAt
                )

                startPaymentDetection(invoice, remainingMs)
                startTimeout(invoice.id, remainingMs)
            }
            InvoiceStatus.PENDING -> {
                _state.value = PosState.Pending(
                    invoiceId = invoice.id,
                    amount = invoice.getDisplayAmount(),
                    signature = invoice.foundSignature ?: ""
                )

                startPaymentDetection(invoice, remainingMs)
                startTimeout(invoice.id, remainingMs)
            }
            else -> {
                Log.w(TAG, "Cannot resume invoice with status: ${invoice.status}")
                reset()
                return false
            }
        }

        Log.d(TAG, "Blocked invoice resumed successfully")
        return true
    }

    /**
     * Check if there's an active payment in progress.
     * Used to prevent navigation away without explicit cancel.
     */
    fun hasActivePayment(): Boolean {
        return when (_state.value) {
            is PosState.Qr, is PosState.Pending -> true
            else -> false
        }
    }

    /**
     * Get current invoice ID if payment is active.
     */
    fun getActiveInvoiceId(): String? {
        return when (val state = _state.value) {
            is PosState.Qr -> state.invoiceId
            is PosState.Pending -> state.invoiceId
            else -> null
        }
    }

    /**
     * Restore UI state from an active invoice in Room.
     *
     * Called when entering POS flow to check if there's an active payment
     * that should be resumed (e.g., after app restart).
     *
     * @return true if state was restored, false if no active invoice
     */
    suspend fun restoreActiveInvoice(): Boolean {
        val app = WinoPayApplication.instance
        val activeInvoice = app.invoiceRepository.getActiveInvoice()

        if (activeInvoice == null) {
            Log.d(TAG, "No active invoice to restore")
            return false
        }

        Log.d(TAG, "━━━━━ RESTORING ACTIVE INVOICE ━━━━━")
        Log.d(TAG, "  Invoice ID: ${activeInvoice.id}")
        Log.d(TAG, "  Status: ${activeInvoice.status}")
        Log.d(TAG, "  Amount: ${activeInvoice.getDisplayAmount()}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        // Store current invoice
        currentInvoice = activeInvoice

        // Calculate remaining time
        val elapsedMs = System.currentTimeMillis() - activeInvoice.createdAt
        val remainingMs = invoiceTimeoutMs - elapsedMs

        // Rebuild QR data (needed for soft-expire state too)
        currentQrData = paymentRail.buildPaymentRequest(activeInvoice)

        if (remainingMs <= 0) {
            // Timeout already passed - show soft-expire dialog (don't auto-expire!)
            Log.d(TAG, "Active invoice timeout passed - showing soft-expire")
            val elapsedSeconds = (elapsedMs / 1000).toInt()
            _state.value = PosState.ExpiredAwaitingDecision(
                invoiceId = activeInvoice.id,
                amount = activeInvoice.getDisplayAmount(),
                elapsedSeconds = elapsedSeconds,
                qrData = currentQrData ?: ""
            )
            return true
        }

        expiresAt = System.currentTimeMillis() + remainingMs

        // Restore UI state based on invoice status
        when (activeInvoice.status) {
            InvoiceStatus.CREATED -> {
                _state.value = PosState.Qr(
                    invoiceId = activeInvoice.id,
                    amount = activeInvoice.getDisplayAmount(),
                    usdcAmount = activeInvoice.getDisplayAmount(),
                    method = PaymentMethod.fromCurrency(activeInvoice.currency, activeInvoice.railId, activeInvoice.splTokenMint ?: ""),
                    qrData = currentQrData!!,
                    expiresAt = expiresAt
                )

                // Restart foreground detection
                startPaymentDetection(activeInvoice, remainingMs)
                startTimeout(activeInvoice.id, remainingMs)
            }
            InvoiceStatus.PENDING -> {
                _state.value = PosState.Pending(
                    invoiceId = activeInvoice.id,
                    amount = activeInvoice.getDisplayAmount(),
                    signature = activeInvoice.foundSignature ?: ""
                )

                // Restart foreground detection (in case confirmation is pending)
                startPaymentDetection(activeInvoice, remainingMs)
                startTimeout(activeInvoice.id, remainingMs)
            }
            else -> {
                // CONFIRMED, FAILED, EXPIRED, CANCELED - shouldn't be "active"
                Log.w(TAG, "Unexpected active invoice status: ${activeInvoice.status}")
                return false
            }
        }

        Log.d(TAG, "Invoice state restored successfully")
        return true
    }

    /**
     * Go back to SelectPayment from QR (to choose different method).
     */
    fun changePaymentMethod() {
        cancelJobs()
        val invoice = currentInvoice ?: return

        _state.value = PosState.SelectPayment(
            amount = invoice.getDisplayAmount(),
            invoiceId = invoice.id
        )

        Log.d(TAG, "Changed payment method, back to SelectPayment")
    }

    /**
     * Retry after failure.
     */
    fun retry() {
        reset()
    }

    /**
     * Manually trigger failure (for testing).
     */
    fun triggerFailure(reason: String = "Manual failure") {
        val invoice = currentInvoice ?: return
        cancelJobs()

        _state.value = PosState.Failed(
            invoiceId = invoice.id,
            amount = invoice.getDisplayAmount(),
            reason = reason
        )

        // Mark as failed in database
        scope.launch(Dispatchers.IO) {
            WinoPayApplication.instance.invoiceRepository.failInvoice(invoice.id)
        }

        Log.d(TAG, "Triggered failure: $reason")
    }

    /**
     * Sync UI state from Room invoice status.
     *
     * Called when background worker updates Room while QR screen is visible.
     * This ensures UI reflects the true invoice state from database.
     */
    fun syncFromRoomStatus(invoice: InvoiceEntity) {
        val currentState = _state.value

        // Only sync if we're showing QR or Pending for this invoice
        val activeInvoiceId = when (currentState) {
            is PosState.Qr -> currentState.invoiceId
            is PosState.Pending -> currentState.invoiceId
            else -> null
        }

        if (activeInvoiceId != invoice.id) {
            return
        }

        // Determine if state transition is needed based on Room status
        when (invoice.status) {
            InvoiceStatus.CONFIRMED -> {
                if (currentState !is PosState.Success) {
                    Log.d(TAG, "Room sync: CONFIRMED - transitioning to Success")
                    cancelJobs()
                    _state.value = PosState.Success(
                        invoiceId = invoice.id,
                        amount = invoice.getDisplayAmount(),
                        signature = invoice.foundSignature ?: ""
                    )
                    DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoice.id)
                }
            }
            InvoiceStatus.FAILED -> {
                if (currentState !is PosState.Failed) {
                    Log.d(TAG, "Room sync: FAILED - transitioning to Failed")
                    cancelJobs()
                    _state.value = PosState.Failed(
                        invoiceId = invoice.id,
                        amount = invoice.getDisplayAmount(),
                        reason = invoice.failureReason ?: "Payment failed"
                    )
                    DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoice.id)
                }
            }
            InvoiceStatus.EXPIRED -> {
                if (currentState !is PosState.Expired) {
                    Log.d(TAG, "Room sync: EXPIRED - transitioning to Expired")
                    cancelJobs()
                    _state.value = PosState.Expired(
                        invoiceId = invoice.id,
                        amount = invoice.getDisplayAmount()
                    )
                    DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoice.id)
                }
            }
            InvoiceStatus.PENDING -> {
                if (currentState is PosState.Qr) {
                    Log.d(TAG, "Room sync: PENDING - transitioning to Pending")
                    _state.value = PosState.Pending(
                        invoiceId = invoice.id,
                        amount = invoice.getDisplayAmount(),
                        signature = invoice.foundSignature ?: ""
                    )
                }
            }
            InvoiceStatus.CREATED -> {
                // No action needed - still waiting for payment
            }
            InvoiceStatus.CANCELED -> {
                // Invoice was canceled (likely by merchant from another path)
                // Reset to EnterAmount
                if (currentState is PosState.Qr || currentState is PosState.Pending) {
                    Log.d(TAG, "Room sync: CANCELED - resetting to EnterAmount")
                    cancelJobs()
                    reset()
                }
            }
        }
    }

    /**
     * Get remaining seconds until expiry.
     */
    fun getRemainingSeconds(): Int {
        if (expiresAt == 0L) return 0
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0).toInt()
    }

    /**
     * Start payment detection by polling reference address.
     */
    private fun startPaymentDetection(invoice: InvoiceEntity, timeoutMs: Long = invoiceTimeoutMs) {
        paymentRail.startForegroundDetection(
            invoice = invoice,
            timeoutMs = timeoutMs
        ) { invoiceId, result ->
            handleDetectionResult(invoiceId, result)
        }
    }

    /**
     * Handle payment detection result.
     *
     * PENDING SCREEN UX:
     * - If detection is FAST (< 800ms) → skip Pending, go straight to Success
     * - If detection is SLOW (>= 800ms) → show Pending → delay → Success
     */
    private suspend fun handleDetectionResult(invoiceId: String, result: DetectionResult) {
        val invoice = currentInvoice
        if (invoice == null || invoice.id != invoiceId) {
            Log.w(TAG, "Detection result for unknown/different invoice: $invoiceId")
            return
        }

        Log.d(TAG, "Detection result for $invoiceId: $result")

        // Calculate detection elapsed time
        val detectionElapsedMs = System.currentTimeMillis() - invoice.createdAt
        val shouldShowPending = detectionElapsedMs >= 800

        Log.d(TAG, "Detection elapsed: ${detectionElapsedMs}ms, showPending=$shouldShowPending")

        when (result) {
            is DetectionResult.Found -> {
                // Update database first (with payer info)
                WinoPayApplication.instance.invoiceRepository.markPending(
                    invoiceId = invoiceId,
                    signature = result.transactionId,
                    actualMintUsed = result.actualTokenUsed,
                    warningCode = result.warningCode,
                    payerAddress = result.payerAddress,
                    payerTokenAccount = result.payerTokenAccount
                )

                // PART A: PENDING SCREEN LOGIC
                if (shouldShowPending) {
                    // Slow detection (>= 800ms) → show Pending screen
                    Log.d(TAG, "SLOW detection → showing Pending screen")
                    _state.value = PosState.Pending(
                        invoiceId = invoiceId,
                        amount = invoice.getDisplayAmount(),
                        signature = result.transactionId
                    )

                    // Wait for visual feedback
                    if (!result.isConfirmed) {
                        delay(1500)
                    }
                } else {
                    // Fast detection (< 800ms) → skip Pending, go straight to Success
                    Log.d(TAG, "FAST detection (${detectionElapsedMs}ms) → skipping Pending, straight to Success")
                }

                // Transition to success
                cancelJobs()
                _state.value = PosState.Success(
                    invoiceId = invoiceId,
                    amount = invoice.getDisplayAmount(),
                    signature = result.transactionId
                )

                WinoPayApplication.instance.invoiceRepository.confirmInvoice(
                    invoiceId = invoiceId,
                    signature = result.transactionId,
                    actualMintUsed = result.actualTokenUsed,
                    warningCode = result.warningCode,
                    payerAddress = result.payerAddress,
                    payerTokenAccount = result.payerTokenAccount
                )

                // Cancel background worker since payment confirmed
                DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoiceId)
            }

            is DetectionResult.Confirmed -> {
                // Transaction already confirmed
                // Update database first (with payer info)
                WinoPayApplication.instance.invoiceRepository.confirmInvoice(
                    invoiceId = invoiceId,
                    signature = result.transactionId,
                    actualMintUsed = result.actualTokenUsed,
                    warningCode = result.warningCode,
                    payerAddress = result.payerAddress,
                    payerTokenAccount = result.payerTokenAccount
                )

                // PART A: PENDING SCREEN LOGIC
                if (shouldShowPending) {
                    // Slow detection → show brief Pending before Success
                    Log.d(TAG, "SLOW detection → showing Pending briefly")
                    _state.value = PosState.Pending(
                        invoiceId = invoiceId,
                        amount = invoice.getDisplayAmount(),
                        signature = result.transactionId
                    )
                    delay(800) // Brief visual feedback
                } else {
                    Log.d(TAG, "FAST detection (${detectionElapsedMs}ms) → straight to Success")
                }

                // Transition to success
                cancelJobs()
                _state.value = PosState.Success(
                    invoiceId = invoiceId,
                    amount = invoice.getDisplayAmount(),
                    signature = result.transactionId
                )

                // Cancel background worker since payment confirmed
                DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoiceId)
            }

            is DetectionResult.Invalid -> {
                // Invalid transaction - mark as failed
                cancelJobs()
                _state.value = PosState.Failed(
                    invoiceId = invoiceId,
                    amount = invoice.getDisplayAmount(),
                    reason = result.reason
                )

                WinoPayApplication.instance.invoiceRepository.failInvoice(invoiceId)

                // Cancel background worker since payment failed
                DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoiceId)
            }

            is DetectionResult.Expired -> {
                // Detection timeout
                cancelJobs()
                _state.value = PosState.Expired(
                    invoiceId = invoiceId,
                    amount = invoice.getDisplayAmount()
                )

                WinoPayApplication.instance.invoiceRepository.expireInvoice(invoiceId)

                // Cancel background worker since invoice expired
                DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoiceId)
            }

            is DetectionResult.Error -> {
                // Error - could retry or fail
                Log.e(TAG, "Detection error: ${result.message}")
                // Keep current state, don't fail immediately
            }

            is DetectionResult.Searching -> {
                // Still searching, state already set
            }
        }
    }

    private fun startTimeout(invoiceId: String, timeoutMs: Long = invoiceTimeoutMs) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeoutMs)

            // Check if still in QR or Pending state
            val currentState = _state.value
            if (currentState is PosState.Qr || currentState is PosState.Pending) {
                val elapsedSeconds = ((System.currentTimeMillis() - (currentInvoice?.createdAt ?: 0L)) / 1000).toInt()

                // PROOF LOG: Timeout reached
                Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|TIMEOUT_REACHED|elapsed=${elapsedSeconds}s")

                // Perform last-chance sweep before showing soft-expire
                Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|LAST_CHANCE_SWEEP_START")
                val paymentFound = performLastChanceSweep(invoiceId)
                Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|LAST_CHANCE_SWEEP_END|found=$paymentFound")

                if (paymentFound) {
                    Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|SWEEP_FOUND_PAYMENT|dialog_skipped")
                    return@launch
                }

                // Transition to soft-expire state (detection continues)
                // NOTE: We do NOT cancel detection or mark as expired yet
                Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|SOFT_EXPIRE_DIALOG_SHOWN")
                _state.value = PosState.ExpiredAwaitingDecision(
                    invoiceId = invoiceId,
                    amount = currentInvoice?.getDisplayAmount() ?: 0.0,
                    elapsedSeconds = elapsedSeconds,
                    qrData = currentQrData ?: ""
                )
            }
        }
    }

    /**
     * Perform a last-chance sweep to catch transactions that arrived at the last moment.
     * Returns true if a valid payment was found.
     */
    private suspend fun performLastChanceSweep(invoiceId: String): Boolean {
        Log.d(TAG, "━━━━━ LAST-CHANCE SWEEP ━━━━━")
        Log.d(TAG, "  invoiceId: $invoiceId")
        Log.d(TAG, "  sweepDuration: ${InvoiceTimeouts.LAST_CHANCE_SWEEP_MS}ms")

        // Give detection a few more seconds to find payment
        delay(InvoiceTimeouts.LAST_CHANCE_SWEEP_MS)

        // Check if state changed during sweep (payment detected)
        val currentState = _state.value
        val paymentFound = currentState is PosState.Success ||
                          currentState is PosState.Pending

        Log.d(TAG, "  result: ${if (paymentFound) "PAYMENT FOUND" else "no payment"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return paymentFound
    }

    /**
     * Extend invoice timeout by 5 more minutes.
     * Called when merchant chooses "Wait 5 more minutes" in soft-expire state.
     *
     * SOFT-EXPIRE FLOW:
     * 1. Updates deadline in DB (worker reads this)
     * 2. Reschedules background detection
     * 3. Restarts foreground detection
     * 4. Returns to QR state
     */
    fun extendTimeout() {
        val currentState = _state.value
        if (currentState !is PosState.ExpiredAwaitingDecision) {
            Log.w(TAG, "Cannot extend: not in ExpiredAwaitingDecision state")
            return
        }

        val invoice = currentInvoice
        if (invoice == null) {
            Log.e(TAG, "Cannot extend: no current invoice")
            return
        }

        val extensionMs = InvoiceTimeouts.EXTENSION_TIMEOUT_MS
        val newDeadline = System.currentTimeMillis() + extensionMs
        expiresAt = newDeadline

        // PROOF LOG: Extension clicked
        Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoice.id.take(8)}|EXTEND_CLICKED|newDeadline=$newDeadline")

        // Update deadline in DB so worker sees it
        scope.launch(Dispatchers.IO) {
            WinoPayApplication.instance.invoiceRepository.extendDeadline(invoice.id, newDeadline)
            Log.i(TAG, "SOFT_EXPIRE|DB|${invoice.id.take(8)}|DEADLINE_UPDATED|newDeadline=$newDeadline")
        }

        // Update current invoice reference
        currentInvoice = invoice.extendDeadline(newDeadline)

        // Reschedule background detection with new deadline
        DetectionScheduler.scheduleDetectionWithDeadline(
            context = WinoPayApplication.instance,
            invoice = currentInvoice!!,
            deadlineAt = newDeadline
        )
        Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoice.id.take(8)}|WORK_RESCHEDULED|deadline=$newDeadline")

        // Restart foreground detection with extension timeout
        startPaymentDetection(currentInvoice!!, extensionMs)

        // Restart timeout job
        startTimeout(invoice.id, extensionMs)

        // Return to QR state
        _state.value = PosState.Qr(
            invoiceId = invoice.id,
            amount = currentState.amount,
            usdcAmount = currentState.amount,
            method = PaymentMethod.fromCurrency(invoice.currency, invoice.railId, invoice.splTokenMint ?: ""),
            qrData = currentState.qrData,
            expiresAt = newDeadline
        )
    }

    /**
     * Revoke invoice (merchant explicitly closes after soft-expire).
     * Marks invoice as EXPIRED and stops all detection.
     *
     * SOFT-EXPIRE FLOW:
     * This is the ONLY place where invoice is marked EXPIRED.
     * Worker does NOT auto-expire - only UI can do this.
     */
    fun revokeInvoice() {
        val currentState = _state.value
        if (currentState !is PosState.ExpiredAwaitingDecision) {
            Log.w(TAG, "Cannot revoke: not in ExpiredAwaitingDecision state")
            return
        }

        val invoiceId = currentState.invoiceId

        // PROOF LOGS: Revoke clicked
        Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|REVOKE_CLICKED")

        cancelJobs()
        Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|CANCEL_FOREGROUND")

        // Mark as EXPIRED in database
        scope.launch(Dispatchers.IO) {
            WinoPayApplication.instance.invoiceRepository.expireInvoice(invoiceId)
            Log.i(TAG, "SOFT_EXPIRE|DB|${invoiceId.take(8)}|MARK_EXPIRED")
        }

        // Cancel background worker
        DetectionScheduler.cancelDetection(WinoPayApplication.instance, invoiceId)
        Log.i(TAG, "SOFT_EXPIRE|FOREGROUND|${invoiceId.take(8)}|CANCEL_WORK")

        // Transition to final Expired state
        _state.value = PosState.Expired(
            invoiceId = invoiceId,
            amount = currentState.amount
        )
    }

    private fun cancelJobs() {
        currentInvoice?.let { paymentRail.cancelForegroundDetection(it.id) }
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
