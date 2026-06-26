package com.banktracker.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.model.Bank
import com.banktracker.data.model.Transaction
import com.banktracker.util.JalaliUtil
import com.banktracker.util.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val db: AppDatabase) {

    val allBanks: Flow<List<Bank>> = db.bankDao().getAllBanks()

    suspend fun insertBank(bank: Bank) = db.bankDao().insert(bank)
    suspend fun deleteBank(bank: Bank) = db.bankDao().delete(bank)
    suspend fun updateBank(bank: Bank) = db.bankDao().update(bank)

    fun getTransactionsByDate(date: String) = db.transactionDao().getByDate(date)
    fun sumDepositByDate(date: String) = db.transactionDao().sumDepositByDate(date)
    fun sumWithdrawalByDate(date: String) = db.transactionDao().sumWithdrawalByDate(date)
    fun getDistinctDates() = db.transactionDao().getDistinctDates()
    fun deleteTransaction(t: Transaction) = db.transactionDao()

    suspend fun processSms(senderNumber: String, messageBody: String, timestamp: Long) {
        val banks = db.bankDao().getAllBanksOnce()
        val bank = banks.firstOrNull { b ->
            senderNumber.contains(b.senderNumber.trim(), ignoreCase = true) ||
                    b.senderNumber.trim().contains(senderNumber, ignoreCase = true)
        } ?: return

        val parsed = SmsParser.parse(messageBody) ?: return

        val existing = db.transactionDao().findDuplicate(timestamp, messageBody)
        if (existing != null) return

        val tx = Transaction(
            bankName = bank.name,
            senderNumber = senderNumber,
            amount = parsed.first,
            type = parsed.second,
            rawMessage = messageBody,
            timestamp = timestamp,
            jalaliDate = JalaliUtil.timestampToJalaliDate(timestamp)
        )
        db.transactionDao().insert(tx)
    }

    suspend fun scanInbox(contentResolver: ContentResolver) = withContext(Dispatchers.IO) {
        val banks = db.bankDao().getAllBanksOnce()
        if (banks.isEmpty()) return@withContext 0

        val uri = Uri.parse("content://sms/inbox")
        val cursor = contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            null, null, "date DESC"
        )

        var count = 0
        cursor?.use {
            val addrIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val address = it.getString(addrIdx)?.trim() ?: continue
                val body = it.getString(bodyIdx) ?: continue
                val date = it.getLong(dateIdx)

                val bank = banks.firstOrNull { b ->
                    address.contains(b.senderNumber.trim(), ignoreCase = true) ||
                            b.senderNumber.trim().contains(address, ignoreCase = true)
                } ?: continue

                val parsed = SmsParser.parse(body) ?: continue

                val existing = db.transactionDao().findDuplicate(date, body)
                if (existing != null) continue

                val tx = Transaction(
                    bankName = bank.name,
                    senderNumber = address,
                    amount = parsed.first,
                    type = parsed.second,
                    rawMessage = body,
                    timestamp = date,
                    jalaliDate = JalaliUtil.timestampToJalaliDate(date)
                )
                db.transactionDao().insert(tx)
                count++
            }
        }
        count
    }
}
