package com.winopay.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.winopay.WinoPayApplication
import com.winopay.data.local.AppDatabase
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Terminal states that MUST NOT be overwritten by expire/cancel/fail transitions. */
private val PROTECTED_TERMINAL_STATES = setOf(
    InvoiceStatus.CONFIRMED,
    InvoiceStatus.REFUND_SENT
)

/**
 * Repository for managing invoices in Room database.
 *
 * In Solana Pay POS flow:
 * - Invoices are created when merchant generates QR code
 * - Payment detection updates invoice status via reference polling
 * - NO MWA signing happens here - customer pays from their wallet
 */
class InvoiceRepository(
    private val database: AppDatabase
) {

    companion object {
        private const val TAG = "InvoiceRepository"
    }

    private val invoiceDao = database.invoiceDao()

    /**
     * Save a new invoice to database.
     *
     * Uses ABORT conflict strategy — will throw if invoice with same ID already exists.
     * This prevents silent overwrite of confirmed payment records.
     *
     * @return true if saved successfully, false if duplicate ID detected
     */
    suspend fun saveInvoice(invoice: InvoiceEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            invoiceDao.insert(invoice)
            Log.d(TAG, "Invoice saved: ${invoice.id}, status: ${invoice.status}")
            true
        } catch (e: SQLiteConstraintException) {
            Log.e(TAG, "saveInvoice: DUPLICATE ID PREVENTED — invoice ${invoice.id} already exists. " +
                "This protects against silent overwrite of existing records.", e)
            false
        }
    }

    /**
     * Update existing invoice.
     */
    suspend fun updateInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.update(invoice)
        Log.d(TAG, "Invoice updated: ${invoice.id}, status: ${invoice.status}")
    }

    /**
     * Mark invoice as pending (transaction found, awaiting confirmation).
     *
     * IDEMPOTENT & CRASH-SAFE:
     * - Early exit if invoice not found
     * - Early exit if already CONFIRMED (never downgrade)
     * - Early exit if already PENDING with same signature (no-op)
     * - Checks if signature belongs to another invoice (UNIQUE constraint protection)
     * - Catches SQLiteConstraintException to prevent crash on race condition
     *
     * @param invoiceId Invoice identifier
     * @param signature Transaction signature
     * @param actualMintUsed Actual mint used for payment (null if same as expected or SOL)
     * @param warningCode Warning code if paid with different stablecoin
     * @param payerAddress Wallet address of payer (owner for SPL, sender for native)
     * @param payerTokenAccount Token account that sent payment (source ATA for SPL)
     * @return true if update was performed, false if skipped or constraint violated
     */
    suspend fun markPending(
        invoiceId: String,
        signature: String,
        actualMintUsed: String? = null,
        warningCode: String? = null,
        payerAddress: String? = null,
        payerTokenAccount: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Early exit if invoice not found
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice == null) {
            Log.w(TAG, "markPending: invoice not found: $invoiceId")
            return@withContext false
        }

        // 2. Early exit if already CONFIRMED (never downgrade confirmed invoice)
        if (invoice.status == InvoiceStatus.CONFIRMED) {
            Log.d(TAG, "markPending: invoice already CONFIRMED, skipping: $invoiceId")
            return@withContext false
        }

        // 2b. Refuse to mark pending if invoice is in a terminal state (EXPIRED/CANCELED/FAILED).
        // Prevents race where worker marks pending after UI already expired/canceled.
        if (invoice.status in setOf(InvoiceStatus.EXPIRED, InvoiceStatus.CANCELED, InvoiceStatus.FAILED)) {
            Log.w(TAG, "markPending: BLOCKED — invoice $invoiceId is ${invoice.status}, " +
                "cannot transition to PENDING (likely race with UI expire/cancel)")
            return@withContext false
        }

        // 3. Early exit if already PENDING with same signature (idempotent)
        if (invoice.status == InvoiceStatus.PENDING && invoice.foundSignature == signature) {
            Log.d(TAG, "markPending: already PENDING with same signature, no-op: $invoiceId")
            return@withContext true  // Success - already in desired state
        }

        // 4. Check if signature already belongs to ANOTHER invoice (UNIQUE constraint protection)
        val existingWithSig = invoiceDao.getBySignature(signature)
        if (existingWithSig != null && existingWithSig.id != invoiceId) {
            Log.w(TAG, "markPending: signature already belongs to another invoice " +
                "(sig=$signature, existingInvoice=${existingWithSig.id}, requestedInvoice=$invoiceId)")
            return@withContext false
        }

        // 5. Perform update with constraint exception handling
        try {
            invoiceDao.update(invoice.markPending(signature, actualMintUsed, warningCode, payerAddress, payerTokenAccount))
            Log.d(TAG, "Invoice marked pending: $invoiceId, signature: $signature" +
                (if (actualMintUsed != null) ", actualMint: $actualMintUsed" else "") +
                (if (warningCode != null) ", warning: $warningCode" else "") +
                (if (payerAddress != null) ", payer: $payerAddress" else ""))
            true
        } catch (e: SQLiteConstraintException) {
            // Race condition: another thread already assigned this signature
            Log.w(TAG, "markPending: SQLiteConstraintException (race condition handled): " +
                "invoiceId=$invoiceId, signature=$signature", e)
            false
        }
    }

    /**
     * Mark invoice as confirmed.
     *
     * IDEMPOTENT & CRASH-SAFE:
     * - Early exit if invoice not found
     * - Early exit if already CONFIRMED with same signature (no-op)
     * - Early exit if already CONFIRMED with any signature (never overwrite)
     * - Checks if signature belongs to another invoice (UNIQUE constraint protection)
     * - Catches SQLiteConstraintException to prevent crash on race condition
     *
     * @param invoiceId Invoice identifier
     * @param signature Transaction signature
     * @param actualMintUsed Actual mint used for payment (null if same as expected or SOL)
     * @param warningCode Warning code if paid with different stablecoin
     * @param payerAddress Wallet address of payer (owner for SPL, sender for native)
     * @param payerTokenAccount Token account that sent payment (source ATA for SPL)
     * @return true if update was performed, false if skipped or constraint violated
     */
    suspend fun confirmInvoice(
        invoiceId: String,
        signature: String,
        actualMintUsed: String? = null,
        warningCode: String? = null,
        payerAddress: String? = null,
        payerTokenAccount: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        // 1. Early exit if invoice not found
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice == null) {
            Log.w(TAG, "confirmInvoice: invoice not found: $invoiceId")
            return@withContext false
        }

        // 2. Early exit if already CONFIRMED (never overwrite confirmed invoice)
        if (invoice.status == InvoiceStatus.CONFIRMED) {
            if (invoice.foundSignature == signature) {
                Log.d(TAG, "confirmInvoice: already CONFIRMED with same signature, no-op: $invoiceId")
                return@withContext true  // Success - already in desired state
            } else {
                Log.w(TAG, "confirmInvoice: already CONFIRMED with different signature, skipping: $invoiceId " +
                    "(existing=${invoice.foundSignature}, requested=$signature)")
                return@withContext false
            }
        }

        // 2b. CRITICAL GUARD: Refuse to confirm invoices in terminal states.
        // Prevents TOCTOU race where worker confirms after UI already expired/canceled.
        if (invoice.status in setOf(InvoiceStatus.EXPIRED, InvoiceStatus.CANCELED, InvoiceStatus.FAILED)) {
            Log.w(TAG, "confirmInvoice: BLOCKED — invoice $invoiceId is ${invoice.status}, " +
                "cannot transition to CONFIRMED (likely race with UI expire/cancel)")
            return@withContext false
        }

        // 3. Check if signature already belongs to ANOTHER invoice (UNIQUE constraint protection)
        val existingWithSig = invoiceDao.getBySignature(signature)
        if (existingWithSig != null && existingWithSig.id != invoiceId) {
            Log.w(TAG, "confirmInvoice: signature already belongs to another invoice " +
                "(sig=$signature, existingInvoice=${existingWithSig.id}, requestedInvoice=$invoiceId)")
            return@withContext false
        }

        // 4. Perform update with constraint exception handling
        try {
            invoiceDao.update(invoice.markConfirmed(signature, actualMintUsed, warningCode, payerAddress, payerTokenAccount))
            Log.d(TAG, "Invoice confirmed: $invoiceId, signature: $signature" +
                (if (actualMintUsed != null) ", actualMint: $actualMintUsed" else "") +
                (if (warningCode != null) ", warning: $warningCode" else "") +
                (if (payerAddress != null) ", payer: $payerAddress" else ""))
            true
        } catch (e: SQLiteConstraintException) {
            // Race condition: another thread already assigned this signature
            Log.w(TAG, "confirmInvoice: SQLiteConstraintException (race condition handled): " +
                "invoiceId=$invoiceId, signature=$signature", e)
            false
        }
    }

    /**
     * Mark refund as sent for an invoice (DEPRECATED - use createRefundInvoice instead).
     *
     * @param invoiceId Invoice identifier
     * @param refundTxId Refund transaction ID
     */
    @Deprecated("Use createRefundInvoice to create a separate refund transaction")
    suspend fun markRefundSent(invoiceId: String, refundTxId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.markRefundSent(refundTxId))
            Log.d(TAG, "Invoice refund sent: $invoiceId, refundTxId: $refundTxId")
        }
    }

    /**
     * Create a new refund invoice linked to the original payment.
     *
     * This creates a SEPARATE transaction record for the refund,
     * keeping the original payment invoice unchanged.
     *
     * @param refundId New invoice ID for the refund (format: A00001-Z99999)
     * @param originalInvoiceId ID of the original payment invoice
     * @param refundTxSignature Transaction signature of the refund
     * @return The created refund invoice, or null if original not found
     */
    suspend fun createRefundInvoice(
        refundId: String,
        originalInvoiceId: String,
        refundTxSignature: String
    ): InvoiceEntity? = withContext(Dispatchers.IO) {
        val originalInvoice = invoiceDao.getById(originalInvoiceId)
        if (originalInvoice == null) {
            Log.e(TAG, "Cannot create refund: original invoice not found: $originalInvoiceId")
            return@withContext null
        }

        if (originalInvoice.payerAddress == null) {
            Log.e(TAG, "Cannot create refund: no payer address on invoice: $originalInvoiceId")
            return@withContext null
        }

        // Create new invoice for the refund
        val refundInvoice = InvoiceEntity.createRefund(
            id = refundId,
            originalInvoice = originalInvoice,
            refundTxSignature = refundTxSignature,
            refundRecipient = originalInvoice.payerAddress!!
        )

        try {
            invoiceDao.insert(refundInvoice)
        } catch (e: SQLiteConstraintException) {
            Log.e(TAG, "createRefundInvoice: DUPLICATE ID — refund $refundId already exists", e)
            return@withContext null
        }
        Log.i(TAG, "REFUND|CREATED|id=$refundId|originalId=$originalInvoiceId|txId=$refundTxSignature")

        // Also update original invoice to mark it has a linked refund
        invoiceDao.update(originalInvoice.copy(
            linkedInvoiceId = refundId,
            updatedAt = System.currentTimeMillis()
        ))

        return@withContext refundInvoice
    }

    /**
     * Create a failed refund invoice linked to the original payment.
     *
     * This creates a SEPARATE transaction record for the failed refund attempt,
     * keeping the original payment invoice unchanged.
     *
     * @param refundId New invoice ID for the failed refund (format: A00001-Z99999)
     * @param originalInvoiceId ID of the original payment invoice
     * @param failureReason Reason why the refund failed
     * @return The created failed refund invoice, or null if original not found
     */
    suspend fun createRefundFailedInvoice(
        refundId: String,
        originalInvoiceId: String,
        failureReason: String
    ): InvoiceEntity? = withContext(Dispatchers.IO) {
        val originalInvoice = invoiceDao.getById(originalInvoiceId)
        if (originalInvoice == null) {
            Log.e(TAG, "Cannot create failed refund: original invoice not found: $originalInvoiceId")
            return@withContext null
        }

        if (originalInvoice.payerAddress == null) {
            Log.e(TAG, "Cannot create failed refund: no payer address on invoice: $originalInvoiceId")
            return@withContext null
        }

        // Create new invoice for the failed refund
        val refundInvoice = InvoiceEntity.createRefundFailed(
            id = refundId,
            originalInvoice = originalInvoice,
            failureReason = failureReason,
            refundRecipient = originalInvoice.payerAddress!!
        )

        try {
            invoiceDao.insert(refundInvoice)
        } catch (e: SQLiteConstraintException) {
            Log.e(TAG, "createRefundFailedInvoice: DUPLICATE ID — refund $refundId already exists", e)
            return@withContext null
        }
        Log.i(TAG, "REFUND|FAILED_CREATED|id=$refundId|originalId=$originalInvoiceId|reason=$failureReason")

        return@withContext refundInvoice
    }

    /**
     * Mark invoice as refund sent (manual refund flow for TRON).
     *
     * Creates a refund invoice record for the original payment.
     * Used when user manually sends a refund from their wallet app.
     *
     * @param invoiceId ID of the original payment invoice
     * @param refundTxId Transaction ID (can be "manual_refund_xxx" for manual refunds)
     * @return true if successful
     */
    suspend fun markAsRefundSent(invoiceId: String, refundTxId: String): Boolean = withContext(Dispatchers.IO) {
        val refundId = WinoPayApplication.instance.invoiceIdGenerator.nextId()
        val result = createRefundInvoice(
            refundId = refundId,
            originalInvoiceId = invoiceId,
            refundTxSignature = refundTxId
        )
        Log.i(TAG, "REFUND|MANUAL_SENT|originalId=$invoiceId|refundId=$refundId|txId=$refundTxId|success=${result != null}")
        return@withContext result != null
    }

    /**
     * Mark invoice as failed with optional reason.
     *
     * STATE MACHINE GUARD: Refuses to transition CONFIRMED or REFUND_SENT invoices.
     * These are protected terminal states whose records must not be destroyed.
     */
    suspend fun failInvoice(invoiceId: String, reason: String? = null) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice == null) return@withContext
        if (invoice.status in PROTECTED_TERMINAL_STATES) {
            Log.w(TAG, "failInvoice: BLOCKED — invoice $invoiceId is ${invoice.status}, cannot transition to FAILED")
            return@withContext
        }
        invoiceDao.update(invoice.markFailed(reason))
        Log.d(TAG, "Invoice marked failed: $invoiceId, reason: $reason")
    }

    /**
     * Mark invoice as expired.
     *
     * STATE MACHINE GUARD: Refuses to transition CONFIRMED or REFUND_SENT invoices.
     * These are protected terminal states whose records must not be destroyed.
     */
    suspend fun expireInvoice(invoiceId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice == null) return@withContext
        if (invoice.status in PROTECTED_TERMINAL_STATES) {
            Log.w(TAG, "expireInvoice: BLOCKED — invoice $invoiceId is ${invoice.status}, cannot transition to EXPIRED")
            return@withContext
        }
        invoiceDao.update(invoice.markExpired())
        Log.d(TAG, "Invoice marked expired: $invoiceId")
    }

    /**
     * Extend invoice deadline for soft-expire support.
     * Called when merchant clicks "Wait 5 more minutes".
     *
     * SOFT-EXPIRE FLOW:
     * 1. Initial deadline reached -> show dialog
     * 2. "Wait 5 more minutes" -> extendDeadline(newDeadline)
     * 3. Worker reads new deadline from DB -> continues polling
     * 4. "Revoke invoice" -> expireInvoice() marks as EXPIRED
     */
    suspend fun extendDeadline(invoiceId: String, newDeadlineAt: Long) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.extendDeadline(newDeadlineAt))
            Log.d(TAG, "Invoice deadline extended: $invoiceId, newDeadline=$newDeadlineAt")
        }
    }

    /**
     * Get current deadline for an invoice.
     */
    suspend fun getDeadline(invoiceId: String): Long = withContext(Dispatchers.IO) {
        invoiceDao.getById(invoiceId)?.deadlineAt ?: 0L
    }

    /**
     * Mark invoice as canceled by merchant.
     * Used when merchant explicitly cancels a payment in progress.
     *
     * STATE MACHINE GUARD: Refuses to transition CONFIRMED or REFUND_SENT invoices.
     * These are protected terminal states whose records must not be destroyed.
     */
    suspend fun cancelInvoice(invoiceId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice == null) return@withContext
        if (invoice.status in PROTECTED_TERMINAL_STATES) {
            Log.w(TAG, "cancelInvoice: BLOCKED — invoice $invoiceId is ${invoice.status}, cannot transition to CANCELED")
            return@withContext
        }
        invoiceDao.update(invoice.markCanceled())
        Log.d(TAG, "Invoice canceled by merchant: $invoiceId")
    }

    /**
     * Get invoice by ID.
     */
    suspend fun getInvoice(invoiceId: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getById(invoiceId)
    }

    /**
     * Get invoice by reference.
     */
    suspend fun getInvoiceByReference(reference: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getByReference(reference)
    }

    /**
     * Observe invoice by ID.
     */
    fun observeInvoice(invoiceId: String): Flow<InvoiceEntity?> {
        return invoiceDao.observeById(invoiceId)
    }

    /**
     * Observe invoice by reference.
     */
    fun observeInvoiceByReference(reference: String): Flow<InvoiceEntity?> {
        return invoiceDao.observeByReference(reference)
    }

    /**
     * Observe all invoices.
     */
    fun observeAllInvoices(): Flow<List<InvoiceEntity>> {
        return invoiceDao.observeAll()
    }

    /**
     * Observe recent invoices.
     */
    fun observeRecentInvoices(limit: Int = 20): Flow<List<InvoiceEntity>> {
        return invoiceDao.observeRecent(limit)
    }

    /**
     * Observe invoices by status.
     */
    fun observeByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> {
        return invoiceDao.observeByStatus(status)
    }

    /**
     * Get pending/created invoices (for resuming detection on app restart).
     */
    suspend fun getPendingInvoices(): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getPendingInvoices()
    }

    /**
     * Check if there's an active invoice (CREATED or PENDING).
     * Used to enforce single-active-invoice rule.
     *
     * CRITICAL: Amount-only detection requires sequential payments.
     * Only ONE invoice can be active at a time.
     */
    suspend fun hasActiveInvoice(): Boolean = withContext(Dispatchers.IO) {
        invoiceDao.getActiveInvoiceCount() > 0
    }

    /**
     * Get the current active invoice (if any).
     */
    suspend fun getActiveInvoice(): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getActiveInvoice()
    }

    /**
     * Expire all active invoices.
     * Called before creating a new invoice to enforce single-active rule.
     */
    suspend fun expireAllActiveInvoices() = withContext(Dispatchers.IO) {
        invoiceDao.expireAllActiveInvoices()
        Log.d(TAG, "All active invoices expired")
    }

    /**
     * Delete invoice by ID.
     */
    suspend fun deleteInvoice(invoiceId: String) = withContext(Dispatchers.IO) {
        invoiceDao.deleteById(invoiceId)
        Log.d(TAG, "Invoice deleted: $invoiceId")
    }

    /**
     * Delete all invoices.
     * Used during logout to clear all transaction history.
     */
    suspend fun deleteAllInvoices() = withContext(Dispatchers.IO) {
        invoiceDao.deleteAll()
        Log.i(TAG, "All invoices deleted")
    }

    // ==================== Wallet-Filtered Methods ====================
    // These methods filter invoices by recipientAddress (merchant wallet).
    // Use these to show only invoices for the currently connected wallet.

    /**
     * Observe all invoices for a specific wallet.
     */
    fun observeAllInvoicesForWallet(walletAddress: String): Flow<List<InvoiceEntity>> {
        return invoiceDao.observeAllForWallet(walletAddress)
    }

    /**
     * Observe recent invoices for a specific wallet.
     */
    fun observeRecentInvoicesForWallet(walletAddress: String, limit: Int = 20): Flow<List<InvoiceEntity>> {
        return invoiceDao.observeRecentForWallet(walletAddress, limit)
    }

    /**
     * Get pending invoices for a specific wallet.
     */
    suspend fun getPendingInvoicesForWallet(walletAddress: String): List<InvoiceEntity> = withContext(Dispatchers.IO) {
        invoiceDao.getPendingInvoicesForWallet(walletAddress)
    }

    /**
     * Check if there's an active invoice for a specific wallet.
     */
    suspend fun hasActiveInvoiceForWallet(walletAddress: String): Boolean = withContext(Dispatchers.IO) {
        invoiceDao.getActiveInvoiceCountForWallet(walletAddress) > 0
    }

    /**
     * Get the current active invoice for a specific wallet.
     */
    suspend fun getActiveInvoiceForWallet(walletAddress: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getActiveInvoiceForWallet(walletAddress)
    }

    /**
     * Observe total revenue for a specific wallet.
     */
    fun observeTotalRevenueForWallet(walletAddress: String): Flow<Long> {
        return invoiceDao.observeTotalRevenueForWallet(walletAddress)
    }

    /**
     * Observe confirmed invoice count for a specific wallet.
     */
    fun observeConfirmedCountForWallet(walletAddress: String): Flow<Int> {
        return invoiceDao.observeConfirmedCountForWallet(walletAddress)
    }

    /**
     * Observe recent invoices for multiple wallets (multichain support).
     * Used when merchant has multiple rails connected (Solana + TRON + EVM).
     */
    fun observeRecentInvoicesForWallets(walletAddresses: List<String>, limit: Int = 20): Flow<List<InvoiceEntity>> {
        if (walletAddresses.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return invoiceDao.observeRecentForWallets(walletAddresses, limit)
    }
}
