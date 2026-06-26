package com.banktracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.banktracker.data.model.Bank
import com.banktracker.data.model.Transaction

@Database(entities = [Bank::class, Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bankDao(): BankDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "bank_tracker.db")
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
