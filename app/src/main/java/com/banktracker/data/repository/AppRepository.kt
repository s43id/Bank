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

data class SmsItem(
    val address: String,
    val body: String,
    val date: Long,
    val bankName: String,
    val bankId: Long,
    val preview: String = body.take(80).replace('\n', ' ')
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
    fun deleteTransaction(t: Transaction) = db.transactionDao()

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

    suspend fun listBankSms(contentResolver: ContentResolver, sinceMs: Long = 0L): List<SmsItem> =
        withContext(Dispatchers.IO) {
            val banks = db.bankDao().getAllBanksOnce().filter { it.senderNumber.isNotBlank() }
            if (banks.isEmpty()) return@withContext emptyList()

            val uri = Uri.parse("content://sms/inbox")
            val selection = if (sinceMs > 0) "date >= ?" else null
            val selectionArgs = if (sinceMs > 0) arrayOf(sinceMs.toString()) else null

            val cursor = contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                selection, selectionArgs, "date DESC"
            )

            val result = mutableListOf<SmsItem>()
            cursor?.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = it.getString(addrIdx)?.trim() ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    val date = it.getLong(dateIdx)

                    val bank = banks.firstOrNull { b ->
                        val id = b.senderNumber.trim()
                        address.contains(id, ignoreCase = true) || id.contains(address, ignoreCase = true)
                    } ?: continue

                    SmsParser.parse(body) ?: continue

                    result.add(SmsItem(address, body, date, bank.name, bank.id))
                }
            }
            result
        }

    suspend fun importSmsItems(contentResolver: ContentResolver, items: List<SmsItem>): Int =
        withContext(Dispatchers.IO) {
            var count = 0
            for (item in items) {
                val existing = db.transactionDao().findDuplicate(item.date, item.body)
                if (existing != null) continue
                val parsed = SmsParser.parse(item.body) ?: continue
                db.transactionDao().insert(
                    Transaction(
                        bankName = item.bankName,
                        senderNumber = item.address,
                        amount = parsed.first,
                        type = parsed.second,
                        rawMessage = item.body,
                        timestamp = item.date,
                        jalaliDate = JalaliUtil.timestampToJalaliDate(item.date)
                    )
                )
                count++
            }
            count
        }

    suspend fun scanInbox(contentResolver: ContentResolver, sinceMs: Long = 0L): Int =
        withContext(Dispatchers.IO) {
            val items = listBankSms(contentResolver, sinceMs)
            importSmsItems(contentResolver, items)
        }
}
