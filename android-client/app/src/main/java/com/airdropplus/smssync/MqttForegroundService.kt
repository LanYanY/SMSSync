package com.airdropplus.smssync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class MqttForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    private val healthCheckTask = object : Runnable {
        override fun run() {
            val manager = MqttHolder.mqttManager
            if (manager != null && !manager.isConnected()) {
                reconnectFromPrefs()
            }
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFY_ID, buildNotification("验证码同步服务运行中"))
        handler.post(healthCheckTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val broker = intent?.getStringExtra(EXTRA_BROKER)
        val appId = intent?.getStringExtra(EXTRA_APP_ID)
        val username = intent?.getStringExtra(EXTRA_USERNAME)
        val password = intent?.getStringExtra(EXTRA_PASSWORD)

        if (!broker.isNullOrBlank() && !appId.isNullOrBlank()) {
            val manager = (MqttHolder.mqttManager ?: MqttManager(applicationContext)).also {
                MqttHolder.mqttManager = it
            }
            manager.connect(broker, appId, username, password) { msg ->
                updateStatusNotification(msg)
            }
        } else {
            reconnectFromPrefs()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        MqttHolder.mqttManager?.disconnect()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun reconnectFromPrefs() {
        val prefs = getSharedPreferences("sms_sync", MODE_PRIVATE)
        val broker = prefs.getString("broker", "") ?: ""
        val appId = prefs.getString("app_id", "") ?: ""
        val username = prefs.getString("username", "")?.ifEmpty { null }
        val password = prefs.getString("password", "")?.ifEmpty { null }

        if (broker.isNotBlank() && appId.isNotBlank()) {
            val manager = (MqttHolder.mqttManager ?: MqttManager(applicationContext)).also {
                MqttHolder.mqttManager = it
            }
            manager.connect(broker, appId, username, password) { msg ->
                updateStatusNotification(msg)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "短信同步后台服务",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("SMS Sync")
            .setContentText(content)
            .setOngoing(true)
            .build()
    }

    private fun updateStatusNotification(content: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFY_ID, buildNotification(content))
    }

    companion object {
        private const val CHANNEL_ID = "sms_sync_service"
        private const val NOTIFY_ID = 1001

        const val ACTION_START = "com.airdropplus.smssync.action.START"
        const val ACTION_STOP = "com.airdropplus.smssync.action.STOP"
        const val EXTRA_BROKER = "extra_broker"
        const val EXTRA_APP_ID = "extra_app_id"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
    }
}
