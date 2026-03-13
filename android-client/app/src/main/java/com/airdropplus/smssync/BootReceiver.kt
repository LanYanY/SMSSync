package com.airdropplus.smssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("sms_sync", Context.MODE_PRIVATE)
        val broker = prefs.getString("broker", "") ?: ""
        val appId = prefs.getString("app_id", "") ?: ""
        if (broker.isBlank() || appId.isBlank()) return

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
