package com.airdropplus.smssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {
    companion object {
        var mqttManager: MqttManager? = null
        private val CODE_PATTERN = Pattern.compile("(?<!\\d)(\\d{4,8})(?!\\d)")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val matcher = CODE_PATTERN.matcher(body)
        val code = if (matcher.find()) matcher.group(1) else null

        mqttManager?.takeIf { it.isConnected() }?.publishSms(code, body)
    }
}
