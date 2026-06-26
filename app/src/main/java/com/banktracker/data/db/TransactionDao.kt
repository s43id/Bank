package com.banktracker.data.db

import androidx.room.*
import com.banktracker.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE jalaliDate = :date ORDER BY timestamp DESC")
    fun getByDate(date: String): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE jalaliDate = :date AND type = 'DEPOSIT'")
    fun sumDepositByDate(date: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE jalaliDate = :date AND type = 'WITHDRAWAL'")
    fun sumWithdrawalByDate(date: String): Flow<Long>

    @Query("SELECT DISTINCT jalaliDate FROM transactions ORDER BY timestamp DESC")
    fun getDistinctDates(): Flow<List<String>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 50")
    fun getRecent(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp = :ts AND rawMessage = :msg LIMIT 1")
    suspend fun findDuplicate(ts: Long, msg: String): Transaction?
}
