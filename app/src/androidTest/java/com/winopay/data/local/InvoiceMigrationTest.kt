package com.winopay.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.winopay.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InvoiceMigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate4To5_backfillsNetworkId_andCreatesUniqueIndex() {
        val dbV4 = helper.createDatabase(dbName, 4)

        dbV4.execSQL(
            """
            INSERT INTO invoices (
                id, reference, recipientAddress, amount, currency, splTokenMint, status,
                foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt,
                failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed,
                rateDirection, rateProvider, rateDate, rateFetchedAt
            ) VALUES (
                'inv1', 'ref1', 'rcpt1', 1, 'USDC', 'mint1', 'CREATED',
                NULL, 'memo', 'label', 'msg', 1000, 1000, NULL,
                NULL, NULL, NULL, NULL, NULL,
                NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )

        dbV4.close()

        val dbV5 = helper.runMigrationsAndValidate(
            dbName,
            5,
            true,
            AppDatabase.MIGRATION_4_5_PUBLIC
        )

        dbV5.query("SELECT networkId FROM invoices WHERE id='inv1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(BuildConfig.SOLANA_CLUSTER, c.getString(0))
        }

        // Verify index exists
        dbV5.query("PRAGMA index_list('invoices')").use { c ->
            var found = false
            var isUnique = false
            while (c.moveToNext()) {
                val name = c.getString(c.getColumnIndexOrThrow("name"))
                if (name == "index_invoices_foundSignature_unique") {
                    found = true
                    isUnique = c.getInt(c.getColumnIndexOrThrow("unique")) == 1
                    break
                }
            }
            assertTrue("Index should exist", found)
            assertTrue("Index should be UNIQUE", isUnique)
        }

        // Verify index covers foundSignature column
        dbV5.query("PRAGMA index_info('index_invoices_foundSignature_unique')").use { c ->
            assertTrue("Index should have columns", c.moveToFirst())
            val columnName = c.getString(c.getColumnIndexOrThrow("name"))
            assertEquals("foundSignature", columnName)
        }

        dbV5.close()
    }

    /**
     * Test that partial unique index allows multiple NULL foundSignature values.
     * This is critical for the CREATED/PENDING invoices that don't have signatures yet.
     */
    @Test
    fun partialUniqueIndex_allowsMultipleNullSignatures() {
        val dbV5 = helper.createDatabase(dbName, 5)

        // Insert multiple invoices with NULL foundSignature - should NOT violate constraint
        dbV5.execSQL(
            """
            INSERT INTO invoices (
                id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status,
                foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt,
                failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed,
                rateDirection, rateProvider, rateDate, rateFetchedAt
            ) VALUES (
                'inv1', 'ref1', 'rcpt1', 1, 'USDC', 'mint1', 'devnet', 'CREATED',
                NULL, 'memo', 'label', 'msg', 1000, 1000, NULL,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )

        dbV5.execSQL(
            """
            INSERT INTO invoices (
                id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status,
                foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt,
                failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed,
                rateDirection, rateProvider, rateDate, rateFetchedAt
            ) VALUES (
                'inv2', 'ref2', 'rcpt2', 2, 'USDC', 'mint2', 'devnet', 'CREATED',
                NULL, 'memo2', 'label2', 'msg2', 2000, 2000, NULL,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )

        // Verify both exist
        dbV5.query("SELECT COUNT(*) FROM invoices WHERE foundSignature IS NULL").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2, c.getInt(0))
        }

        dbV5.close()
    }

    /**
     * Test that unique index prevents duplicate non-null signatures.
     * This is the core race-condition protection.
     */
    @Test
    fun partialUniqueIndex_preventsDuplicateSignatures() {
        val dbV5 = helper.createDatabase(dbName, 5)

        val signature = "test_signature_12345"

        // First insert with signature - should succeed
        dbV5.execSQL(
            """
            INSERT INTO invoices (
                id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status,
                foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt,
                failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed,
                rateDirection, rateProvider, rateDate, rateFetchedAt
            ) VALUES (
                'inv1', 'ref1', 'rcpt1', 1, 'USDC', 'mint1', 'devnet', 'CONFIRMED',
                '$signature', 'memo', 'label', 'msg', 1000, 1000, 1000,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
            )
            """.trimIndent()
        )

        // Second insert with SAME signature - should fail with constraint violation
        var exceptionThrown = false
        try {
            dbV5.execSQL(
                """
                INSERT INTO invoices (
                    id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status,
                    foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt,
                    failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed,
                    rateDirection, rateProvider, rateDate, rateFetchedAt
                ) VALUES (
                    'inv2', 'ref2', 'rcpt2', 2, 'USDC', 'mint2', 'devnet', 'CONFIRMED',
                    '$signature', 'memo2', 'label2', 'msg2', 2000, 2000, 2000,
                    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
                )
                """.trimIndent()
            )
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            exceptionThrown = true
        }

        assertTrue("Should throw SQLiteConstraintException for duplicate signature", exceptionThrown)

        // Verify only one invoice exists with this signature
        dbV5.query("SELECT COUNT(*) FROM invoices WHERE foundSignature = ?", arrayOf(signature)).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }

        dbV5.close()
    }
}
