package com.airdropplus.smssync

import android.content.Context

object MqttRuntime {
    @Volatile
    private var manager: MqttManager? = null
    private val pendingSms = ArrayDeque<Pair<String?, String>>()

    fun manager(context: Context): MqttManager {
        return manager ?: synchronized(this) {
            manager ?: MqttManager(context.applicationContext).also { manager = it }
        }
    }

    @Synchronized
    fun publishSmsWithQueue(code: String?, smsBody: String) {
        val mqtt = manager
        if (mqtt?.isConnected() == true) {
            mqtt.publishSms(code, smsBody)
        } else {
            pendingSms.addLast(code to smsBody)
        }
    }

    @Synchronized
    fun flushPendingIfConnected() {
        val mqtt = manager ?: return
        if (!mqtt.isConnected()) return
        while (pendingSms.isNotEmpty()) {
            val (code, body) = pendingSms.removeFirst()
            mqtt.publishSms(code, body)
        }
    }

    fun disconnect() {
        manager?.disconnect()
    }
}
