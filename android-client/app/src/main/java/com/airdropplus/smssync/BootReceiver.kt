package com.airdropplus.smssync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("sms_sync", Context.MODE_PRIVATE)
            val broker = prefs.getString("broker", "tcp://broker.emqx.io:1883").orEmpty()
            val appId = prefs.getString("app_id", "demo-room").orEmpty()
            if (broker.isNotBlank() && appId.isNotBlank()) {
                val username = prefs.getString("username", "").orEmpty().ifBlank { null }
                val password = prefs.getString("password", "").orEmpty().ifBlank { null }
                MqttSyncService.start(context, broker, appId, username, password)
            }
        }
    }
}
