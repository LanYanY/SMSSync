package com.airdropplus.smssync

import android.content.Context

object MqttRuntime {
    @Volatile
    private var manager: MqttManager? = null

    fun manager(context: Context): MqttManager {
        return manager ?: synchronized(this) {
            manager ?: MqttManager(context.applicationContext).also { manager = it }
        }
    }

    fun publishSmsIfConnected(code: String?, smsBody: String) {
        manager?.takeIf { it.isConnected() }?.publishSms(code, smsBody)
    }

    fun disconnect() {
        manager?.disconnect()
    }
}
