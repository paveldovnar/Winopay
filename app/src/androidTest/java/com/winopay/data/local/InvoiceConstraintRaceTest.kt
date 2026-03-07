package com.winopay.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for UNIQUE INDEX constraint handling on invoices.foundSignature.
 *
 * Simulates race conditions where multiple "threads" attempt to assign
 * the same signature to different invoices. Ensures:
 * 1. One update succeeds
 * 2. Other updates get SQLiteConstraintException
 * 3. No crashes occur
 */
@RunWith(AndroidJUnit4::class)
class InvoiceConstraintRaceTest {

    private lateinit var database: AppDatabase
    private lateinit var invoiceDao: InvoiceDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        invoiceDao = database.invoiceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test: Two coroutines race to assign same signature to different invoices.
     * Expected: One succeeds, one gets constraint exception, no crash.
     */
    @Test
    fun raceCondition_sameSignatureToDifferentInvoices_oneSucceedsOneHandled() = runBlocking {
        // Create two invoices
        val invoice1 = createTestInvoice("invoice-1")
        val invoice2 = createTestInvoice("invoice-2")
        invoiceDao.insert(invoice1)
        invoiceDao.insert(invoice2)

        val sharedSignature = "test-signature-${UUID.randomUUID()}"
        val successCount = AtomicInteger(0)
        val constraintExceptionCount = AtomicInteger(0)

        // Launch two coroutines that race to assign same signature
        val results = listOf(
            async(Dispatchers.IO) {
                tryAssignSignature("invoice-1", sharedSignature, successCount, constraintExceptionCount)
            },
            async(Dispatchers.IO) {
                tryAssignSignature("invoice-2", sharedSignature, successCount, constraintExceptionCount)
            }
        ).awaitAll()

        // Verify: exactly one success, one constraint exception
        assertEquals("Exactly one update should succeed", 1, successCount.get())
        assertEquals("Exactly one constraint exception should occur", 1, constraintExceptionCount.get())

        // Verify: signature is assigned to exactly one invoice
        val inv1 = invoiceDao.getById("invoice-1")
        val inv2 = invoiceDao.getById("invoice-2")
        assertNotNull(inv1)
        assertNotNull(inv2)

        val assignedCount = listOf(inv1, inv2).count { it?.foundSignature == sharedSignature }
        assertEquals("Signature should be assigned to exactly one invoice", 1, assignedCount)
    }

    /**
     * Test: Idempotent update - same signature to same invoice twice.
     * Expected: First succeeds, second is no-op (no exception).
     */
    @Test
    fun idempotentUpdate_sameSignatureToSameInvoice_noException() = runBlocking {
        val invoice = createTestInvoice("invoice-1")
        invoiceDao.insert(invoice)

        val signature = "test-signature-idempotent"

        // First update
        val updated1 = invoice.copy(
            status = InvoiceStatus.CONFIRMED,
            foundSignature = signature,
            updatedAt = System.currentTimeMillis()
        )
        invoiceDao.update(updated1)

        // Second update with same signature (simulate idempotent retry)
        val updated2 = updated1.copy(updatedAt = System.currentTimeMillis())
        // Should NOT throw - same entity, same signature
        invoiceDao.update(updated2)

        val result = invoiceDao.getById("invoice-1")
        assertEquals(signature, result?.foundSignature)
        assertEquals(InvoiceStatus.CONFIRMED, result?.status)
    }

    /**
     * Test: Concurrent updates to same invoice with same signature.
     * Expected: No constraint violation, both "succeed" (last wins).
     */
    @Test
    fun concurrentUpdates_sameInvoiceSameSignature_noException() = runBlocking {
        val invoice = createTestInvoice("invoice-1")
        invoiceDao.insert(invoice)

        val signature = "test-signature-concurrent"
        val updateCount = AtomicInteger(0)

        val jobs = (1..5).map { i ->
            async(Dispatchers.IO) {
                val current = invoiceDao.getById("invoice-1")!!
                val updated = current.copy(
                    status = InvoiceStatus.CONFIRMED,
                    foundSignature = signature,
                    updatedAt = System.currentTimeMillis() + i
                )
                invoiceDao.update(updated)
                updateCount.incrementAndGet()
            }
        }

        jobs.awaitAll()

        // All updates should complete without exception
        assertEquals("All updates should complete", 5, updateCount.get())

        val result = invoiceDao.getById("invoice-1")
        assertEquals(signature, result?.foundSignature)
    }

    /**
     * Test: getBySignature returns correct invoice.
     */
    @Test
    fun getBySignature_returnsCorrectInvoice() = runBlocking {
        val invoice1 = createTestInvoice("invoice-1")
        val invoice2 = createTestInvoice("invoice-2")
        invoiceDao.insert(invoice1)
        invoiceDao.insert(invoice2)

        val signature = "unique-signature-123"
        val updated = invoice1.copy(
            foundSignature = signature,
            status = InvoiceStatus.CONFIRMED
        )
        invoiceDao.update(updated)

        val found = invoiceDao.getBySignature(signature)
        assertNotNull(found)
        assertEquals("invoice-1", found?.id)

        val notFound = invoiceDao.getBySignature("nonexistent-signature")
        assertNull(notFound)
    }

    /**
     * Test: Signature null is allowed for multiple invoices (partial unique index).
     */
    @Test
    fun nullSignature_allowedForMultipleInvoices() = runBlocking {
        val invoice1 = createTestInvoice("invoice-1")
        val invoice2 = createTestInvoice("invoice-2")
        val invoice3 = createTestInvoice("invoice-3")

        // All with null signature (CREATED status)
        invoiceDao.insert(invoice1)
        invoiceDao.insert(invoice2)
        invoiceDao.insert(invoice3)

        // Verify all exist with null signature
        val all = listOf(
            invoiceDao.getById("invoice-1"),
            invoiceDao.getById("invoice-2"),
            invoiceDao.getById("invoice-3")
        )
        assertEquals(3, all.filterNotNull().count())
        assertTrue(all.all { it?.foundSignature == null })
    }

    private suspend fun tryAssignSignature(
        invoiceId: String,
        signature: String,
        successCount: AtomicInteger,
        constraintExceptionCount: AtomicInteger
    ): Boolean {
        return try {
            val invoice = invoiceDao.getById(invoiceId)!!
            val updated = invoice.copy(
                status = InvoiceStatus.CONFIRMED,
                foundSignature = signature,
                updatedAt = System.currentTimeMillis()
            )
            invoiceDao.update(updated)
            successCount.incrementAndGet()
            true
        } catch (e: SQLiteConstraintException) {
            constraintExceptionCount.incrementAndGet()
            false
        }
    }

    private fun createTestInvoice(id: String): InvoiceEntity {
        val now = System.currentTimeMillis()
        return InvoiceEntity(
            id = id,
            reference = "ref-$id",
            recipientAddress = "wallet-address",
            recipientTokenAccount = "ata-address",
            amount = 1000000L,
            currency = PaymentCurrency.USDC,
            splTokenMint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            networkId = "devnet",
            status = InvoiceStatus.CREATED,
            foundSignature = null,
            memo = id,
            label = "Test",
            message = "Test invoice",
            createdAt = now,
            updatedAt = now,
            confirmedAt = null
        )
    }
}
