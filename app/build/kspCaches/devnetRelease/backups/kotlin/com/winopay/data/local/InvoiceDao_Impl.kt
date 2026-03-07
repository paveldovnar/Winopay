package com.winopay.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class InvoiceDao_Impl(
  __db: RoomDatabase,
) : InvoiceDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfInvoiceEntity: EntityInsertAdapter<InvoiceEntity>

  private val __updateAdapterOfInvoiceEntity: EntityDeleteOrUpdateAdapter<InvoiceEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfInvoiceEntity = object : EntityInsertAdapter<InvoiceEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `invoices` (`id`,`reference`,`recipientAddress`,`recipientTokenAccount`,`amount`,`currency`,`splTokenMint`,`railId`,`networkId`,`status`,`foundSignature`,`memo`,`label`,`message`,`createdAt`,`updatedAt`,`confirmedAt`,`failureReason`,`fiatAmountMinor`,`fiatCurrency`,`fiatDecimals`,`rateUsed`,`rateDirection`,`rateProvider`,`rateDate`,`rateFetchedAt`,`actualMintUsed`,`paymentWarningCode`,`deadlineAt`,`payerAddress`,`payerTokenAccount`,`refundTxId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: InvoiceEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.reference)
        statement.bindText(3, entity.recipientAddress)
        val _tmpRecipientTokenAccount: String? = entity.recipientTokenAccount
        if (_tmpRecipientTokenAccount == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpRecipientTokenAccount)
        }
        statement.bindLong(5, entity.amount)
        statement.bindText(6, __PaymentCurrency_enumToString(entity.currency))
        val _tmpSplTokenMint: String? = entity.splTokenMint
        if (_tmpSplTokenMint == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpSplTokenMint)
        }
        statement.bindText(8, entity.railId)
        statement.bindText(9, entity.networkId)
        statement.bindText(10, __InvoiceStatus_enumToString(entity.status))
        val _tmpFoundSignature: String? = entity.foundSignature
        if (_tmpFoundSignature == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpFoundSignature)
        }
        val _tmpMemo: String? = entity.memo
        if (_tmpMemo == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpMemo)
        }
        val _tmpLabel: String? = entity.label
        if (_tmpLabel == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpLabel)
        }
        val _tmpMessage: String? = entity.message
        if (_tmpMessage == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpMessage)
        }
        statement.bindLong(15, entity.createdAt)
        statement.bindLong(16, entity.updatedAt)
        val _tmpConfirmedAt: Long? = entity.confirmedAt
        if (_tmpConfirmedAt == null) {
          statement.bindNull(17)
        } else {
          statement.bindLong(17, _tmpConfirmedAt)
        }
        val _tmpFailureReason: String? = entity.failureReason
        if (_tmpFailureReason == null) {
          statement.bindNull(18)
        } else {
          statement.bindText(18, _tmpFailureReason)
        }
        val _tmpFiatAmountMinor: Long? = entity.fiatAmountMinor
        if (_tmpFiatAmountMinor == null) {
          statement.bindNull(19)
        } else {
          statement.bindLong(19, _tmpFiatAmountMinor)
        }
        val _tmpFiatCurrency: String? = entity.fiatCurrency
        if (_tmpFiatCurrency == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmpFiatCurrency)
        }
        val _tmpFiatDecimals: Int? = entity.fiatDecimals
        if (_tmpFiatDecimals == null) {
          statement.bindNull(21)
        } else {
          statement.bindLong(21, _tmpFiatDecimals.toLong())
        }
        val _tmpRateUsed: String? = entity.rateUsed
        if (_tmpRateUsed == null) {
          statement.bindNull(22)
        } else {
          statement.bindText(22, _tmpRateUsed)
        }
        val _tmpRateDirection: String? = entity.rateDirection
        if (_tmpRateDirection == null) {
          statement.bindNull(23)
        } else {
          statement.bindText(23, _tmpRateDirection)
        }
        val _tmpRateProvider: String? = entity.rateProvider
        if (_tmpRateProvider == null) {
          statement.bindNull(24)
        } else {
          statement.bindText(24, _tmpRateProvider)
        }
        val _tmpRateDate: String? = entity.rateDate
        if (_tmpRateDate == null) {
          statement.bindNull(25)
        } else {
          statement.bindText(25, _tmpRateDate)
        }
        val _tmpRateFetchedAt: Long? = entity.rateFetchedAt
        if (_tmpRateFetchedAt == null) {
          statement.bindNull(26)
        } else {
          statement.bindLong(26, _tmpRateFetchedAt)
        }
        val _tmpActualMintUsed: String? = entity.actualMintUsed
        if (_tmpActualMintUsed == null) {
          statement.bindNull(27)
        } else {
          statement.bindText(27, _tmpActualMintUsed)
        }
        val _tmpPaymentWarningCode: String? = entity.paymentWarningCode
        if (_tmpPaymentWarningCode == null) {
          statement.bindNull(28)
        } else {
          statement.bindText(28, _tmpPaymentWarningCode)
        }
        statement.bindLong(29, entity.deadlineAt)
        val _tmpPayerAddress: String? = entity.payerAddress
        if (_tmpPayerAddress == null) {
          statement.bindNull(30)
        } else {
          statement.bindText(30, _tmpPayerAddress)
        }
        val _tmpPayerTokenAccount: String? = entity.payerTokenAccount
        if (_tmpPayerTokenAccount == null) {
          statement.bindNull(31)
        } else {
          statement.bindText(31, _tmpPayerTokenAccount)
        }
        val _tmpRefundTxId: String? = entity.refundTxId
        if (_tmpRefundTxId == null) {
          statement.bindNull(32)
        } else {
          statement.bindText(32, _tmpRefundTxId)
        }
      }
    }
    this.__updateAdapterOfInvoiceEntity = object : EntityDeleteOrUpdateAdapter<InvoiceEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `invoices` SET `id` = ?,`reference` = ?,`recipientAddress` = ?,`recipientTokenAccount` = ?,`amount` = ?,`currency` = ?,`splTokenMint` = ?,`railId` = ?,`networkId` = ?,`status` = ?,`foundSignature` = ?,`memo` = ?,`label` = ?,`message` = ?,`createdAt` = ?,`updatedAt` = ?,`confirmedAt` = ?,`failureReason` = ?,`fiatAmountMinor` = ?,`fiatCurrency` = ?,`fiatDecimals` = ?,`rateUsed` = ?,`rateDirection` = ?,`rateProvider` = ?,`rateDate` = ?,`rateFetchedAt` = ?,`actualMintUsed` = ?,`paymentWarningCode` = ?,`deadlineAt` = ?,`payerAddress` = ?,`payerTokenAccount` = ?,`refundTxId` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: InvoiceEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.reference)
        statement.bindText(3, entity.recipientAddress)
        val _tmpRecipientTokenAccount: String? = entity.recipientTokenAccount
        if (_tmpRecipientTokenAccount == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpRecipientTokenAccount)
        }
        statement.bindLong(5, entity.amount)
        statement.bindText(6, __PaymentCurrency_enumToString(entity.currency))
        val _tmpSplTokenMint: String? = entity.splTokenMint
        if (_tmpSplTokenMint == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpSplTokenMint)
        }
        statement.bindText(8, entity.railId)
        statement.bindText(9, entity.networkId)
        statement.bindText(10, __InvoiceStatus_enumToString(entity.status))
        val _tmpFoundSignature: String? = entity.foundSignature
        if (_tmpFoundSignature == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpFoundSignature)
        }
        val _tmpMemo: String? = entity.memo
        if (_tmpMemo == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpMemo)
        }
        val _tmpLabel: String? = entity.label
        if (_tmpLabel == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpLabel)
        }
        val _tmpMessage: String? = entity.message
        if (_tmpMessage == null) {
          statement.bindNull(14)
        } else {
          statement.bindText(14, _tmpMessage)
        }
        statement.bindLong(15, entity.createdAt)
        statement.bindLong(16, entity.updatedAt)
        val _tmpConfirmedAt: Long? = entity.confirmedAt
        if (_tmpConfirmedAt == null) {
          statement.bindNull(17)
        } else {
          statement.bindLong(17, _tmpConfirmedAt)
        }
        val _tmpFailureReason: String? = entity.failureReason
        if (_tmpFailureReason == null) {
          statement.bindNull(18)
        } else {
          statement.bindText(18, _tmpFailureReason)
        }
        val _tmpFiatAmountMinor: Long? = entity.fiatAmountMinor
        if (_tmpFiatAmountMinor == null) {
          statement.bindNull(19)
        } else {
          statement.bindLong(19, _tmpFiatAmountMinor)
        }
        val _tmpFiatCurrency: String? = entity.fiatCurrency
        if (_tmpFiatCurrency == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmpFiatCurrency)
        }
        val _tmpFiatDecimals: Int? = entity.fiatDecimals
        if (_tmpFiatDecimals == null) {
          statement.bindNull(21)
        } else {
          statement.bindLong(21, _tmpFiatDecimals.toLong())
        }
        val _tmpRateUsed: String? = entity.rateUsed
        if (_tmpRateUsed == null) {
          statement.bindNull(22)
        } else {
          statement.bindText(22, _tmpRateUsed)
        }
        val _tmpRateDirection: String? = entity.rateDirection
        if (_tmpRateDirection == null) {
          statement.bindNull(23)
        } else {
          statement.bindText(23, _tmpRateDirection)
        }
        val _tmpRateProvider: String? = entity.rateProvider
        if (_tmpRateProvider == null) {
          statement.bindNull(24)
        } else {
          statement.bindText(24, _tmpRateProvider)
        }
        val _tmpRateDate: String? = entity.rateDate
        if (_tmpRateDate == null) {
          statement.bindNull(25)
        } else {
          statement.bindText(25, _tmpRateDate)
        }
        val _tmpRateFetchedAt: Long? = entity.rateFetchedAt
        if (_tmpRateFetchedAt == null) {
          statement.bindNull(26)
        } else {
          statement.bindLong(26, _tmpRateFetchedAt)
        }
        val _tmpActualMintUsed: String? = entity.actualMintUsed
        if (_tmpActualMintUsed == null) {
          statement.bindNull(27)
        } else {
          statement.bindText(27, _tmpActualMintUsed)
        }
        val _tmpPaymentWarningCode: String? = entity.paymentWarningCode
        if (_tmpPaymentWarningCode == null) {
          statement.bindNull(28)
        } else {
          statement.bindText(28, _tmpPaymentWarningCode)
        }
        statement.bindLong(29, entity.deadlineAt)
        val _tmpPayerAddress: String? = entity.payerAddress
        if (_tmpPayerAddress == null) {
          statement.bindNull(30)
        } else {
          statement.bindText(30, _tmpPayerAddress)
        }
        val _tmpPayerTokenAccount: String? = entity.payerTokenAccount
        if (_tmpPayerTokenAccount == null) {
          statement.bindNull(31)
        } else {
          statement.bindText(31, _tmpPayerTokenAccount)
        }
        val _tmpRefundTxId: String? = entity.refundTxId
        if (_tmpRefundTxId == null) {
          statement.bindNull(32)
        } else {
          statement.bindText(32, _tmpRefundTxId)
        }
        statement.bindText(33, entity.id)
      }
    }
  }

  public override suspend fun insert(invoice: InvoiceEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfInvoiceEntity.insert(_connection, invoice)
  }

  public override suspend fun update(invoice: InvoiceEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfInvoiceEntity.handle(_connection, invoice)
  }

  public override suspend fun getById(id: String): InvoiceEntity? {
    val _sql: String = "SELECT * FROM invoices WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeById(id: String): Flow<InvoiceEntity?> {
    val _sql: String = "SELECT * FROM invoices WHERE id = ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBySignature(signature: String): InvoiceEntity? {
    val _sql: String = "SELECT * FROM invoices WHERE foundSignature = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, signature)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByReference(reference: String): InvoiceEntity? {
    val _sql: String = "SELECT * FROM invoices WHERE reference = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByReference(reference: String): Flow<InvoiceEntity?> {
    val _sql: String = "SELECT * FROM invoices WHERE reference = ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, reference)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>> {
    val _sql: String = "SELECT * FROM invoices WHERE status = ? ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, __InvoiceStatus_enumToString(status))
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: MutableList<InvoiceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: InvoiceEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _item = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<InvoiceEntity>> {
    val _sql: String = "SELECT * FROM invoices ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: MutableList<InvoiceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: InvoiceEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _item = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRecent(limit: Int): Flow<List<InvoiceEntity>> {
    val _sql: String = "SELECT * FROM invoices ORDER BY createdAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: MutableList<InvoiceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: InvoiceEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _item = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPendingInvoices(): List<InvoiceEntity> {
    val _sql: String = "SELECT * FROM invoices WHERE status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: MutableList<InvoiceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: InvoiceEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _item = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getActiveInvoiceCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM invoices WHERE status IN ('CREATED', 'PENDING')"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getActiveInvoice(): InvoiceEntity? {
    val _sql: String = "SELECT * FROM invoices WHERE status IN ('CREATED', 'PENDING') ORDER BY createdAt DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfReference: Int = getColumnIndexOrThrow(_stmt, "reference")
        val _columnIndexOfRecipientAddress: Int = getColumnIndexOrThrow(_stmt, "recipientAddress")
        val _columnIndexOfRecipientTokenAccount: Int = getColumnIndexOrThrow(_stmt, "recipientTokenAccount")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfSplTokenMint: Int = getColumnIndexOrThrow(_stmt, "splTokenMint")
        val _columnIndexOfRailId: Int = getColumnIndexOrThrow(_stmt, "railId")
        val _columnIndexOfNetworkId: Int = getColumnIndexOrThrow(_stmt, "networkId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfFoundSignature: Int = getColumnIndexOrThrow(_stmt, "foundSignature")
        val _columnIndexOfMemo: Int = getColumnIndexOrThrow(_stmt, "memo")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfConfirmedAt: Int = getColumnIndexOrThrow(_stmt, "confirmedAt")
        val _columnIndexOfFailureReason: Int = getColumnIndexOrThrow(_stmt, "failureReason")
        val _columnIndexOfFiatAmountMinor: Int = getColumnIndexOrThrow(_stmt, "fiatAmountMinor")
        val _columnIndexOfFiatCurrency: Int = getColumnIndexOrThrow(_stmt, "fiatCurrency")
        val _columnIndexOfFiatDecimals: Int = getColumnIndexOrThrow(_stmt, "fiatDecimals")
        val _columnIndexOfRateUsed: Int = getColumnIndexOrThrow(_stmt, "rateUsed")
        val _columnIndexOfRateDirection: Int = getColumnIndexOrThrow(_stmt, "rateDirection")
        val _columnIndexOfRateProvider: Int = getColumnIndexOrThrow(_stmt, "rateProvider")
        val _columnIndexOfRateDate: Int = getColumnIndexOrThrow(_stmt, "rateDate")
        val _columnIndexOfRateFetchedAt: Int = getColumnIndexOrThrow(_stmt, "rateFetchedAt")
        val _columnIndexOfActualMintUsed: Int = getColumnIndexOrThrow(_stmt, "actualMintUsed")
        val _columnIndexOfPaymentWarningCode: Int = getColumnIndexOrThrow(_stmt, "paymentWarningCode")
        val _columnIndexOfDeadlineAt: Int = getColumnIndexOrThrow(_stmt, "deadlineAt")
        val _columnIndexOfPayerAddress: Int = getColumnIndexOrThrow(_stmt, "payerAddress")
        val _columnIndexOfPayerTokenAccount: Int = getColumnIndexOrThrow(_stmt, "payerTokenAccount")
        val _columnIndexOfRefundTxId: Int = getColumnIndexOrThrow(_stmt, "refundTxId")
        val _result: InvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpReference: String
          _tmpReference = _stmt.getText(_columnIndexOfReference)
          val _tmpRecipientAddress: String
          _tmpRecipientAddress = _stmt.getText(_columnIndexOfRecipientAddress)
          val _tmpRecipientTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfRecipientTokenAccount)) {
            _tmpRecipientTokenAccount = null
          } else {
            _tmpRecipientTokenAccount = _stmt.getText(_columnIndexOfRecipientTokenAccount)
          }
          val _tmpAmount: Long
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount)
          val _tmpCurrency: PaymentCurrency
          _tmpCurrency = __PaymentCurrency_stringToEnum(_stmt.getText(_columnIndexOfCurrency))
          val _tmpSplTokenMint: String?
          if (_stmt.isNull(_columnIndexOfSplTokenMint)) {
            _tmpSplTokenMint = null
          } else {
            _tmpSplTokenMint = _stmt.getText(_columnIndexOfSplTokenMint)
          }
          val _tmpRailId: String
          _tmpRailId = _stmt.getText(_columnIndexOfRailId)
          val _tmpNetworkId: String
          _tmpNetworkId = _stmt.getText(_columnIndexOfNetworkId)
          val _tmpStatus: InvoiceStatus
          _tmpStatus = __InvoiceStatus_stringToEnum(_stmt.getText(_columnIndexOfStatus))
          val _tmpFoundSignature: String?
          if (_stmt.isNull(_columnIndexOfFoundSignature)) {
            _tmpFoundSignature = null
          } else {
            _tmpFoundSignature = _stmt.getText(_columnIndexOfFoundSignature)
          }
          val _tmpMemo: String?
          if (_stmt.isNull(_columnIndexOfMemo)) {
            _tmpMemo = null
          } else {
            _tmpMemo = _stmt.getText(_columnIndexOfMemo)
          }
          val _tmpLabel: String?
          if (_stmt.isNull(_columnIndexOfLabel)) {
            _tmpLabel = null
          } else {
            _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          }
          val _tmpMessage: String?
          if (_stmt.isNull(_columnIndexOfMessage)) {
            _tmpMessage = null
          } else {
            _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpConfirmedAt: Long?
          if (_stmt.isNull(_columnIndexOfConfirmedAt)) {
            _tmpConfirmedAt = null
          } else {
            _tmpConfirmedAt = _stmt.getLong(_columnIndexOfConfirmedAt)
          }
          val _tmpFailureReason: String?
          if (_stmt.isNull(_columnIndexOfFailureReason)) {
            _tmpFailureReason = null
          } else {
            _tmpFailureReason = _stmt.getText(_columnIndexOfFailureReason)
          }
          val _tmpFiatAmountMinor: Long?
          if (_stmt.isNull(_columnIndexOfFiatAmountMinor)) {
            _tmpFiatAmountMinor = null
          } else {
            _tmpFiatAmountMinor = _stmt.getLong(_columnIndexOfFiatAmountMinor)
          }
          val _tmpFiatCurrency: String?
          if (_stmt.isNull(_columnIndexOfFiatCurrency)) {
            _tmpFiatCurrency = null
          } else {
            _tmpFiatCurrency = _stmt.getText(_columnIndexOfFiatCurrency)
          }
          val _tmpFiatDecimals: Int?
          if (_stmt.isNull(_columnIndexOfFiatDecimals)) {
            _tmpFiatDecimals = null
          } else {
            _tmpFiatDecimals = _stmt.getLong(_columnIndexOfFiatDecimals).toInt()
          }
          val _tmpRateUsed: String?
          if (_stmt.isNull(_columnIndexOfRateUsed)) {
            _tmpRateUsed = null
          } else {
            _tmpRateUsed = _stmt.getText(_columnIndexOfRateUsed)
          }
          val _tmpRateDirection: String?
          if (_stmt.isNull(_columnIndexOfRateDirection)) {
            _tmpRateDirection = null
          } else {
            _tmpRateDirection = _stmt.getText(_columnIndexOfRateDirection)
          }
          val _tmpRateProvider: String?
          if (_stmt.isNull(_columnIndexOfRateProvider)) {
            _tmpRateProvider = null
          } else {
            _tmpRateProvider = _stmt.getText(_columnIndexOfRateProvider)
          }
          val _tmpRateDate: String?
          if (_stmt.isNull(_columnIndexOfRateDate)) {
            _tmpRateDate = null
          } else {
            _tmpRateDate = _stmt.getText(_columnIndexOfRateDate)
          }
          val _tmpRateFetchedAt: Long?
          if (_stmt.isNull(_columnIndexOfRateFetchedAt)) {
            _tmpRateFetchedAt = null
          } else {
            _tmpRateFetchedAt = _stmt.getLong(_columnIndexOfRateFetchedAt)
          }
          val _tmpActualMintUsed: String?
          if (_stmt.isNull(_columnIndexOfActualMintUsed)) {
            _tmpActualMintUsed = null
          } else {
            _tmpActualMintUsed = _stmt.getText(_columnIndexOfActualMintUsed)
          }
          val _tmpPaymentWarningCode: String?
          if (_stmt.isNull(_columnIndexOfPaymentWarningCode)) {
            _tmpPaymentWarningCode = null
          } else {
            _tmpPaymentWarningCode = _stmt.getText(_columnIndexOfPaymentWarningCode)
          }
          val _tmpDeadlineAt: Long
          _tmpDeadlineAt = _stmt.getLong(_columnIndexOfDeadlineAt)
          val _tmpPayerAddress: String?
          if (_stmt.isNull(_columnIndexOfPayerAddress)) {
            _tmpPayerAddress = null
          } else {
            _tmpPayerAddress = _stmt.getText(_columnIndexOfPayerAddress)
          }
          val _tmpPayerTokenAccount: String?
          if (_stmt.isNull(_columnIndexOfPayerTokenAccount)) {
            _tmpPayerTokenAccount = null
          } else {
            _tmpPayerTokenAccount = _stmt.getText(_columnIndexOfPayerTokenAccount)
          }
          val _tmpRefundTxId: String?
          if (_stmt.isNull(_columnIndexOfRefundTxId)) {
            _tmpRefundTxId = null
          } else {
            _tmpRefundTxId = _stmt.getText(_columnIndexOfRefundTxId)
          }
          _result = InvoiceEntity(_tmpId,_tmpReference,_tmpRecipientAddress,_tmpRecipientTokenAccount,_tmpAmount,_tmpCurrency,_tmpSplTokenMint,_tmpRailId,_tmpNetworkId,_tmpStatus,_tmpFoundSignature,_tmpMemo,_tmpLabel,_tmpMessage,_tmpCreatedAt,_tmpUpdatedAt,_tmpConfirmedAt,_tmpFailureReason,_tmpFiatAmountMinor,_tmpFiatCurrency,_tmpFiatDecimals,_tmpRateUsed,_tmpRateDirection,_tmpRateProvider,_tmpRateDate,_tmpRateFetchedAt,_tmpActualMintUsed,_tmpPaymentWarningCode,_tmpDeadlineAt,_tmpPayerAddress,_tmpPayerTokenAccount,_tmpRefundTxId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTotalRevenue(): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE status = 'CONFIRMED'"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeRevenueToday(startOfDay: Long): Flow<Long> {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0) FROM invoices WHERE status = 'CONFIRMED' AND createdAt >= ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeInvoiceCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM invoices"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeConfirmedCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM invoices WHERE status = 'CONFIRMED'"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeInvoiceCountToday(startOfDay: Long): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM invoices WHERE createdAt >= ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeConfirmedCountToday(startOfDay: Long): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM invoices WHERE status = 'CONFIRMED' AND createdAt >= ?"
    return createFlow(__db, false, arrayOf("invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun expireAllActiveInvoices(now: Long) {
    val _sql: String = "UPDATE invoices SET status = 'EXPIRED', updatedAt = ? WHERE status IN ('CREATED', 'PENDING')"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: String) {
    val _sql: String = "DELETE FROM invoices WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM invoices"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __PaymentCurrency_enumToString(_value: PaymentCurrency): String = when (_value) {
    PaymentCurrency.SOL -> "SOL"
    PaymentCurrency.USDC -> "USDC"
    PaymentCurrency.USDT -> "USDT"
  }

  private fun __InvoiceStatus_enumToString(_value: InvoiceStatus): String = when (_value) {
    InvoiceStatus.CREATED -> "CREATED"
    InvoiceStatus.PENDING -> "PENDING"
    InvoiceStatus.CONFIRMED -> "CONFIRMED"
    InvoiceStatus.FAILED -> "FAILED"
    InvoiceStatus.EXPIRED -> "EXPIRED"
    InvoiceStatus.CANCELED -> "CANCELED"
  }

  private fun __PaymentCurrency_stringToEnum(_value: String): PaymentCurrency = when (_value) {
    "SOL" -> PaymentCurrency.SOL
    "USDC" -> PaymentCurrency.USDC
    "USDT" -> PaymentCurrency.USDT
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __InvoiceStatus_stringToEnum(_value: String): InvoiceStatus = when (_value) {
    "CREATED" -> InvoiceStatus.CREATED
    "PENDING" -> InvoiceStatus.PENDING
    "CONFIRMED" -> InvoiceStatus.CONFIRMED
    "FAILED" -> InvoiceStatus.FAILED
    "EXPIRED" -> InvoiceStatus.EXPIRED
    "CANCELED" -> InvoiceStatus.CANCELED
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
