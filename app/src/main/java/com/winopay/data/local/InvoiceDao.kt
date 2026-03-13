package com.winopay.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for invoices.
 */
@Dao
interface InvoiceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(invoice: InvoiceEntity)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE foundSignature = :signature")
    suspend fun getBySignature(signature: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE reference = :reference")
    suspend fun getByReference(reference: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE reference = :reference")
    fun observeByReference(reference: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC")
    suspend fun getPendingInvoices(): List<InvoiceEntity>

    // ==================== Wallet-Filtered Queries ====================
    // These queries filter invoices by recipientAddress (merchant wallet).
    // Use these to show only invoices for the currently connected wallet.

    /**
     * Observe all invoices for a specific wallet address.
     */
    @Query("SELECT * FROM invoices WHERE recipientAddress = :walletAddress ORDER BY createdAt DESC")
    fun observeAllForWallet(walletAddress: String): Flow<List<InvoiceEntity>>

    /**
     * Observe recent invoices for a specific wallet address.
     */
    @Query("SELECT * FROM invoices WHERE recipientAddress = :walletAddress ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentForWallet(walletAddress: String, limit: Int): Flow<List<InvoiceEntity>>

    /**
     * Get pending invoices for a specific wallet address.
     */
    @Query("SELECT * FROM invoices WHERE recipientAddress = :walletAddress AND status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC")
    suspend fun getPendingInvoicesForWallet(walletAddress: String): List<InvoiceEntity>

    /**
     * Get active invoice count for a specific wallet.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE recipientAddress = :walletAddress AND status IN ('CREATED', 'PENDING')")
    suspend fun getActiveInvoiceCountForWallet(walletAddress: String): Int

    /**
     * Get the single active invoice for a specific wallet (if any).
     */
    @Query("SELECT * FROM invoices WHERE recipientAddress = :walletAddress AND status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveInvoiceForWallet(walletAddress: String): InvoiceEntity?

    /**
     * Observe recent invoices for multiple wallet addresses (multichain support).
     * Use this when merchant has multiple rails connected (Solana + TRON + EVM).
     *
     * Includes:
     * 1. All invoices where merchant is recipient (payments received)
     * 2. All REFUND_SENT invoices whose original invoice belongs to merchant (refunds sent)
     */
    @Query("""
        SELECT * FROM invoices
        WHERE recipientAddress IN (:walletAddresses)
           OR (status = 'REFUND_SENT' AND linkedInvoiceId IN (
               SELECT id FROM invoices WHERE recipientAddress IN (:walletAddresses)
           ))
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun observeRecentForWallets(walletAddresses: List<String>, limit: Int): Flow<List<InvoiceEntity>>

    /**
     * Get count of active invoices (CREATED or PENDING).
     * Used to enforce single-active-invoice rule.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE status IN ('CREATED', 'PENDING')")
    suspend fun getActiveInvoiceCount(): Int

    /**
     * Get the single active invoice (if any).
     * Returns null if no active invoice, or the most recent one if multiple exist.
     */
    @Query("SELECT * FROM invoices WHERE status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveInvoice(): InvoiceEntity?

    /**
     * Cancel all active invoices (mark as EXPIRED).
     * Used when creating a new invoice to ensure single-active rule.
     */
    @Query("UPDATE invoices SET status = 'EXPIRED', updatedAt = :now WHERE status IN ('CREATED', 'PENDING')")
    suspend fun expireAllActiveInvoices(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()

    // ==================== Statistics Queries ====================

    /**
     * Total revenue from confirmed invoices (raw amount).
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE status = 'CONFIRMED'")
    fun observeTotalRevenue(): Flow<Long>

    /**
     * Revenue today from confirmed invoices (raw amount).
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE status = 'CONFIRMED' AND createdAt >= :startOfDay")
    fun observeRevenueToday(startOfDay: Long): Flow<Long>

    /**
     * Total invoice count.
     */
    @Query("SELECT COUNT(*) FROM invoices")
    fun observeInvoiceCount(): Flow<Int>

    /**
     * Confirmed invoice count.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'CONFIRMED'")
    fun observeConfirmedCount(): Flow<Int>

    /**
     * Invoice count today.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE createdAt >= :startOfDay")
    fun observeInvoiceCountToday(startOfDay: Long): Flow<Int>

    /**
     * Confirmed invoice count today.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'CONFIRMED' AND createdAt >= :startOfDay")
    fun observeConfirmedCountToday(startOfDay: Long): Flow<Int>

    // ==================== Wallet-Filtered Statistics ====================

    /**
     * Total revenue for a specific wallet.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE recipientAddress = :walletAddress AND status = 'CONFIRMED'")
    fun observeTotalRevenueForWallet(walletAddress: String): Flow<Long>

    /**
     * Revenue today for a specific wallet.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE recipientAddress = :walletAddress AND status = 'CONFIRMED' AND createdAt >= :startOfDay")
    fun observeRevenueTodayForWallet(walletAddress: String, startOfDay: Long): Flow<Long>

    /**
     * Invoice count for a specific wallet.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE recipientAddress = :walletAddress")
    fun observeInvoiceCountForWallet(walletAddress: String): Flow<Int>

    /**
     * Confirmed invoice count for a specific wallet.
     */
    @Query("SELECT COUNT(*) FROM invoices WHERE recipientAddress = :walletAddress AND status = 'CONFIRMED'")
    fun observeConfirmedCountForWallet(walletAddress: String): Flow<Int>
}
