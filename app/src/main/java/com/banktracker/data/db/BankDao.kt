package com.banktracker.data.db

import androidx.room.*
import com.banktracker.data.model.Bank
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: Bank)

    @Delete
    suspend fun delete(bank: Bank)

    @Update
    suspend fun update(bank: Bank)

    @Query("SELECT * FROM banks")
    suspend fun getAllBanksOnce(): List<Bank>
}
