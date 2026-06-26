package com.banktracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val repo = AppRepository(AppDatabase.getInstance(context))

        messages.forEach { sms ->
            val sender = sms.originatingAddress ?: return@forEach
            val body = sms.messageBody ?: return@forEach
            val timestamp = sms.timestampMillis

            scope.launch {
                repo.processSms(sender, body, timestamp)
            }
        }
    }
}
