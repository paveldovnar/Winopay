package com.winopay.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.winopay.BuildConfig

/**
 * Room database for WinoPay.
 */
@Database(
    entities = [InvoiceEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun invoiceDao(): InvoiceDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "winopay_db"
        const val VERSION = 8

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from v1 to v2:
         * - Adds 'reference' column (required)
         * - Adds 'currency' column
         * - Adds 'label', 'message' columns
         * - Renames 'signature' to 'foundSignature'
         * - Renames 'tokenMint' to 'splTokenMint'
         *
         * Since this is a significant schema change with new required columns,
         * we drop and recreate the table. Production apps should migrate data.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old table and create new one
                db.execSQL("DROP TABLE IF EXISTS invoices")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS invoices (
                        id TEXT PRIMARY KEY NOT NULL,
                        reference TEXT NOT NULL,
                        recipientAddress TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        splTokenMint TEXT,
                        status TEXT NOT NULL,
                        foundSignature TEXT,
                        memo TEXT,
                        label TEXT,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        confirmedAt INTEGER
                    )
                """)
            }
        }

        /**
         * Migration from v2 to v3:
         * - Adds 'failureReason' column for storing FAILED invoice reasons
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN failureReason TEXT")
            }
        }

        /**
         * Migration from v3 to v4:
         * - Adds FX rate transparency columns for production-grade financial correctness
         * - fiatAmountMinor: Long (minor units, NOT Double)
         * - fiatCurrency: String
         * - fiatDecimals: Int
         * - rateUsed: String (BigDecimal.toPlainString(), NOT Double)
         * - rateDirection: String
         * - rateProvider: String
         * - rateDate: String
         * - rateFetchedAt: Long
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN fiatAmountMinor INTEGER")
                db.execSQL("ALTER TABLE invoices ADD COLUMN fiatCurrency TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN fiatDecimals INTEGER")
                db.execSQL("ALTER TABLE invoices ADD COLUMN rateUsed TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN rateDirection TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN rateProvider TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN rateDate TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN rateFetchedAt INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN networkId TEXT")
                db.execSQL("UPDATE invoices SET networkId = '${BuildConfig.SOLANA_CLUSTER}' WHERE networkId IS NULL OR networkId = ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS invoices_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        reference TEXT NOT NULL,
                        recipientAddress TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        currency TEXT NOT NULL,
                        splTokenMint TEXT,
                        networkId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        foundSignature TEXT,
                        memo TEXT,
                        label TEXT,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        confirmedAt INTEGER,
                        failureReason TEXT,
                        fiatAmountMinor INTEGER,
                        fiatCurrency TEXT,
                        fiatDecimals INTEGER,
                        rateUsed TEXT,
                        rateDirection TEXT,
                        rateProvider TEXT,
                        rateDate TEXT,
                        rateFetchedAt INTEGER
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "INSERT INTO invoices_new (" +
                    "id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status, foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt, failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed, rateDirection, rateProvider, rateDate, rateFetchedAt" +
                    ") SELECT " +
                    "id, reference, recipientAddress, amount, currency, splTokenMint, networkId, status, foundSignature, memo, label, message, createdAt, updatedAt, confirmedAt, failureReason, fiatAmountMinor, fiatCurrency, fiatDecimals, rateUsed, rateDirection, rateProvider, rateDate, rateFetchedAt" +
                    " FROM invoices"
                )

                db.execSQL("DROP TABLE invoices")
                db.execSQL("ALTER TABLE invoices_new RENAME TO invoices")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_invoices_foundSignature_unique ON invoices(foundSignature) WHERE foundSignature IS NOT NULL")
            }
        }

        val MIGRATION_4_5_PUBLIC: Migration = MIGRATION_4_5

        /**
         * Migration from v5 to v6:
         * - Adds 'recipientTokenAccount' column for ATA-based SPL polling
         * - Adds 'actualMintUsed' column for dual stablecoin acceptance
         * - Adds 'paymentWarningCode' column for tracking stablecoin mismatch
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add recipientTokenAccount if not exists (for ATA polling)
                db.execSQL("ALTER TABLE invoices ADD COLUMN recipientTokenAccount TEXT")
                // Add dual stablecoin tracking fields
                db.execSQL("ALTER TABLE invoices ADD COLUMN actualMintUsed TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paymentWarningCode TEXT")
            }
        }

        /**
         * Migration from v6 to v7:
         * - Adds 'railId' column for multi-chain support
         * - Backfills existing invoices with "solana" rail
         *
         * CHAIN-AGNOSTIC SCHEMA:
         * - railId: identifies the payment rail (solana, evm, tron)
         * - networkId: identifies the network within that rail (devnet, mainnet-beta, 1, 137)
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add railId column with default "solana" for existing invoices
                db.execSQL("ALTER TABLE invoices ADD COLUMN railId TEXT NOT NULL DEFAULT 'solana'")
            }
        }

        /**
         * Migration from v7 to v8:
         * - Adds payer info fields for refund support (multichain-ready)
         * - payerAddress: wallet address of payer (owner for SPL, sender for native)
         * - payerTokenAccount: token account address (SPL ATA for Solana, wallet for EVM)
         * - refundTxId: transaction ID if refund was issued
         * - deadlineAt: soft-expire deadline timestamp
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN deadlineAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN payerAddress TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN payerTokenAccount TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN refundTxId TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /** All migrations in order. Must cover v1→VERSION without gaps. */
        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )

        private fun buildDatabase(context: Context): AppDatabase {
            Log.i(TAG, "Building database: version=$VERSION, migrations=${ALL_MIGRATIONS.map { "${it.startVersion}→${it.endVersion}" }}")
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.i(TAG, "Database opened: version=${db.version}")
                    }
                })
                .build()
        }
    }
}
