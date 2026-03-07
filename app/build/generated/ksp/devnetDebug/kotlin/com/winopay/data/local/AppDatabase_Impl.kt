package com.winopay.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _invoiceDao: Lazy<InvoiceDao> = lazy {
    InvoiceDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(8, "847ac3c8d76070ece3ec39f4c4d26462", "cd97e539220c94b47b08ff335cb9c3cf") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `invoices` (`id` TEXT NOT NULL, `reference` TEXT NOT NULL, `recipientAddress` TEXT NOT NULL, `recipientTokenAccount` TEXT, `amount` INTEGER NOT NULL, `currency` TEXT NOT NULL, `splTokenMint` TEXT, `railId` TEXT NOT NULL, `networkId` TEXT NOT NULL, `status` TEXT NOT NULL, `foundSignature` TEXT, `memo` TEXT, `label` TEXT, `message` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `confirmedAt` INTEGER, `failureReason` TEXT, `fiatAmountMinor` INTEGER, `fiatCurrency` TEXT, `fiatDecimals` INTEGER, `rateUsed` TEXT, `rateDirection` TEXT, `rateProvider` TEXT, `rateDate` TEXT, `rateFetchedAt` INTEGER, `actualMintUsed` TEXT, `paymentWarningCode` TEXT, `deadlineAt` INTEGER NOT NULL, `payerAddress` TEXT, `payerTokenAccount` TEXT, `refundTxId` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '847ac3c8d76070ece3ec39f4c4d26462')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `invoices`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsInvoices: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsInvoices.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("reference", TableInfo.Column("reference", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("recipientAddress", TableInfo.Column("recipientAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("recipientTokenAccount", TableInfo.Column("recipientTokenAccount", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("amount", TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("splTokenMint", TableInfo.Column("splTokenMint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("railId", TableInfo.Column("railId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("networkId", TableInfo.Column("networkId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("foundSignature", TableInfo.Column("foundSignature", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("memo", TableInfo.Column("memo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("label", TableInfo.Column("label", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("message", TableInfo.Column("message", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("confirmedAt", TableInfo.Column("confirmedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("failureReason", TableInfo.Column("failureReason", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("fiatAmountMinor", TableInfo.Column("fiatAmountMinor", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("fiatCurrency", TableInfo.Column("fiatCurrency", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("fiatDecimals", TableInfo.Column("fiatDecimals", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("rateUsed", TableInfo.Column("rateUsed", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("rateDirection", TableInfo.Column("rateDirection", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("rateProvider", TableInfo.Column("rateProvider", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("rateDate", TableInfo.Column("rateDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("rateFetchedAt", TableInfo.Column("rateFetchedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("actualMintUsed", TableInfo.Column("actualMintUsed", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("paymentWarningCode", TableInfo.Column("paymentWarningCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("deadlineAt", TableInfo.Column("deadlineAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("payerAddress", TableInfo.Column("payerAddress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("payerTokenAccount", TableInfo.Column("payerTokenAccount", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsInvoices.put("refundTxId", TableInfo.Column("refundTxId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysInvoices: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesInvoices: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoInvoices: TableInfo = TableInfo("invoices", _columnsInvoices, _foreignKeysInvoices, _indicesInvoices)
        val _existingInvoices: TableInfo = read(connection, "invoices")
        if (!_infoInvoices.equals(_existingInvoices)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |invoices(com.winopay.data.local.InvoiceEntity).
              | Expected:
              |""".trimMargin() + _infoInvoices + """
              |
              | Found:
              |""".trimMargin() + _existingInvoices)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "invoices")
  }

  public override fun clearAllTables() {
    super.performClear(false, "invoices")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(InvoiceDao::class, InvoiceDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun invoiceDao(): InvoiceDao = _invoiceDao.value
}
