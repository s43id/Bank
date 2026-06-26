package com.banktracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val senderNumber: String
)
