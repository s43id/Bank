package com.banktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val senderNumber: String,
    val amount: Long,
    val type: String,       // "DEPOSIT" | "WITHDRAWAL" | "UNKNOWN"
    val rawMessage: String,
    val timestamp: Long,
    val jalaliDate: String  // e.g. "1403/09/15"
)
