package com.winopay.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.winopay.data.local.AppDatabase
import com.winopay.data.local.InvoiceEntity
import com.winopay.data.local.InvoiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
     */
    suspend fun saveInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.insert(invoice)
        Log.d(TAG, "Invoice saved: ${invoice.id}, status: ${invoice.status}")
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
     * Mark refund as sent for an invoice.
     *
     * @param invoiceId Invoice identifier
     * @param refundTxId Refund transaction ID
     */
    suspend fun markRefundSent(invoiceId: String, refundTxId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.markRefundSent(refundTxId))
            Log.d(TAG, "Invoice refund sent: $invoiceId, refundTxId: $refundTxId")
        }
    }

    /**
     * Mark invoice as failed with optional reason.
     */
    suspend fun failInvoice(invoiceId: String, reason: String? = null) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.markFailed(reason))
            Log.d(TAG, "Invoice marked failed: $invoiceId, reason: $reason")
        }
    }

    /**
     * Mark invoice as expired.
     */
    suspend fun expireInvoice(invoiceId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.markExpired())
            Log.d(TAG, "Invoice marked expired: $invoiceId")
        }
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
     */
    suspend fun cancelInvoice(invoiceId: String) = withContext(Dispatchers.IO) {
        val invoice = invoiceDao.getById(invoiceId)
        if (invoice != null) {
            invoiceDao.update(invoice.markCanceled())
            Log.d(TAG, "Invoice canceled by merchant: $invoiceId")
        }
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
}
