package com.winopay.data.model

import com.winopay.ui.components.TransactionStatus

data class BusinessIdentity(
    val name: String,
    val logoUri: String?,
    val handle: String, // @wino_business_name
    val publicKey: String,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: String,
    val signature: String,
    val amount: Double,
    val sender: String,
    val recipient: String,
    val reference: String,
    val timestamp: Long,
    val status: TransactionStatus,
    val memo: String? = null
)

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
