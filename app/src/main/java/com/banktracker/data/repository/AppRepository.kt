package com.banktracker.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.model.Transaction
import com.banktracker.util.JalaliUtil
import com.banktracker.util.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.banktracker.data.model.Bank

data class SmsThread(
    val address: String,
    val displayName: String,
    val lastMessage: String,
    val lastDate: Long,
    val messageCount: Int
)

class AppRepository(private val db: AppDatabase) {

    val allBanks: Flow<List<Bank>> = db.bankDao().getAllBanks()

    suspend fun insertBank(bank: Bank) = db.bankDao().insert(bank)
    suspend fun deleteBank(bank: Bank) = db.bankDao().delete(bank)
    suspend fun updateBank(bank: Bank) = db.bankDao().update(bank)

    fun getTransactionsByDate(date: String) = db.transactionDao().getByDate(date)
    fun sumDepositByDate(date: String) = db.transactionDao().sumDepositByDate(date)
    fun sumWithdrawalByDate(date: String) = db.transactionDao().sumWithdrawalByDate(date)
    fun getDistinctDates() = db.transactionDao().getDistinctDates()

    suspend fun listAllThreads(contentResolver: ContentResolver): List<SmsThread> =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse("content://sms/inbox")
            val cursor = contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                null, null, "date DESC"
            ) ?: return@withContext emptyList()

            val threads = LinkedHashMap<String, SmsThread>()
            cursor.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = it.getString(addrIdx)?.trim() ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    val date = it.getLong(dateIdx)

                    val existing = threads[address]
                    if (existing == null) {
                        threads[address] = SmsThread(
                            address = address,
                            displayName = address,
                            lastMessage = body.take(80).replace('\n', ' '),
                            lastDate = date,
                            messageCount = 1
                        )
                    } else {
                        threads[address] = existing.copy(messageCount = existing.messageCount + 1)
                    }
                }
            }
            threads.values.toList()
        }

    suspend fun importThreads(
        contentResolver: ContentResolver,
        addresses: Set<String>,
        sinceMs: Long
    ): Int = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://sms/inbox")
        val selection = if (sinceMs > 0) "date >= ?" else null
        val selectionArgs = if (sinceMs > 0) arrayOf(sinceMs.toString()) else null

        val cursor = contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            selection, selectionArgs, "date DESC"
        ) ?: return@withContext 0

        var count = 0
        cursor.use {
            val addrIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val address = it.getString(addrIdx)?.trim() ?: continue
                if (address !in addresses) continue
                val body = it.getString(bodyIdx) ?: continue
                val date = it.getLong(dateIdx)

                val parsed = SmsParser.parse(body) ?: continue

                val existing = db.transactionDao().findDuplicate(date, body)
                if (existing != null) continue

                db.transactionDao().insert(
                    Transaction(
                        bankName = address,
                        senderNumber = address,
                        amount = parsed.first,
                        type = parsed.second,
                        rawMessage = body,
                        timestamp = date,
                        jalaliDate = JalaliUtil.timestampToJalaliDate(date)
                    )
                )
                count++
            }
        }
        count
    }

    suspend fun processSms(senderNumber: String, messageBody: String, timestamp: Long) {
        val banks = db.bankDao().getAllBanksOnce()
        val bank = banks.firstOrNull { b ->
            val id = b.senderNumber.trim()
            id.isNotEmpty() && (
                senderNumber.contains(id, ignoreCase = true) ||
                id.contains(senderNumber, ignoreCase = true)
            )
        } ?: return

        val parsed = SmsParser.parse(messageBody) ?: return

        val existing = db.transactionDao().findDuplicate(timestamp, messageBody)
        if (existing != null) return

        db.transactionDao().insert(
            Transaction(
                bankName = bank.name,
                senderNumber = senderNumber,
                amount = parsed.first,
                type = parsed.second,
                rawMessage = messageBody,
                timestamp = timestamp,
                jalaliDate = JalaliUtil.timestampToJalaliDate(timestamp)
            )
        )
    }
}
