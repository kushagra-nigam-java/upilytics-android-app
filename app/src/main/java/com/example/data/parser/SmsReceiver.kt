package com.example.data.parser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val body = msg.messageBody ?: continue
                Log.d("SmsReceiver", "Incoming SMS: $body")

                // Parse SMS to see if it's a transaction
                val transaction = SmsParserService.parseMessage(body, msg.timestampMillis)
                if (transaction != null) {
                    scope.launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            db.transactionDao().insertTransaction(transaction)
                            Log.d("SmsReceiver", "Detected transaction of Rs. ${transaction.amount} at ${transaction.merchant}")
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Error saving transaction from receiver", e)
                        }
                    }
                }
            }
        }
    }
}
