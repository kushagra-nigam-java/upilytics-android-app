package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val merchant: String,
    val category: String, // TransactionCategory enum value
    val subcategory: String? = null,
    val transactionType: String, // "debit", "credit", "refund"
    val timestamp: Long,
    val upiRefId: String? = null,
    val bankName: String? = null,
    val rawMessage: String,
    val confidenceScore: Double,
    val isRecurring: Boolean = false,
    val isSubscription: Boolean = false,
    val userVerified: Boolean = false,
    val note: String? = null
)
