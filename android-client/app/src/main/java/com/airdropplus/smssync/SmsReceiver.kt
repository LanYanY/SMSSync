package com.airdropplus.smssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private val CODE_PATTERN = Pattern.compile("(?<!\\d)(\\d{4,8})(?!\\d)")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val matcher = CODE_PATTERN.matcher(body)
        val code = if (matcher.find()) matcher.group(1) else null

        val manager = MqttHolder.mqttManager
        if (manager?.isConnected() == true) {
            manager.publishSms(code, body)
            return
        }

        val prefs = context.getSharedPreferences("sms_sync", Context.MODE_PRIVATE)
        val broker = prefs.getString("broker", "") ?: ""
        val appId = prefs.getString("app_id", "") ?: ""
        if (broker.isNotBlank() && appId.isNotBlank()) {
            val serviceIntent = Intent(context, MqttForegroundService::class.java).apply {
                action = MqttForegroundService.ACTION_START
                putExtra(MqttForegroundService.EXTRA_BROKER, broker)
                putExtra(MqttForegroundService.EXTRA_APP_ID, appId)
                putExtra(MqttForegroundService.EXTRA_USERNAME, prefs.getString("username", ""))
                putExtra(MqttForegroundService.EXTRA_PASSWORD, prefs.getString("password", ""))
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
