package com.airdropplus.smssync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MqttSyncService : Service() {
    companion object {
        const val ACTION_START = "com.airdropplus.smssync.action.START"
        const val ACTION_STOP = "com.airdropplus.smssync.action.STOP"
        const val ACTION_STATUS = "com.airdropplus.smssync.action.STATUS"
        const val EXTRA_STATUS = "status"

        private const val EXTRA_BROKER = "broker"
        private const val EXTRA_APP_ID = "app_id"
        private const val EXTRA_USERNAME = "username"
        private const val EXTRA_PASSWORD = "password"

        private const val CHANNEL_ID = "mqtt_sync_channel"
        private const val NOTIFICATION_ID = 10001

        fun start(
            context: Context,
            broker: String,
            appId: String,
            username: String?,
            password: String?
        ) {
            val intent = Intent(context, MqttSyncService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_BROKER, broker)
                putExtra(EXTRA_APP_ID, appId)
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_PASSWORD, password)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MqttSyncService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MqttRuntime.disconnect()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                ensureForeground()
                val broker = intent.getStringExtra(EXTRA_BROKER).orEmpty()
                val appId = intent.getStringExtra(EXTRA_APP_ID).orEmpty()
                val username = intent.getStringExtra(EXTRA_USERNAME)
                val password = intent.getStringExtra(EXTRA_PASSWORD)

                if (broker.isBlank() || appId.isBlank()) {
                    sendStatus("启动失败：Broker 或 app-id 不能为空")
                } else {
                    MqttRuntime.manager(applicationContext).connect(broker, appId, username, password) {
                        if (it.contains("MQTT连接成功")) {
                            MqttRuntime.flushPendingIfConnected()
                        }
                        sendStatus(it)
                    }
                    sendStatus("后台服务已启动，正在连接 MQTT...")
                }
            }
        }
        return START_STICKY
    }

    private fun ensureForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持 MQTT 连接并后台同步短信验证码"
            }
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Sync 正在运行")
            .setContentText("后台保持 MQTT 长连接")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun sendStatus(status: String) {
        sendBroadcast(Intent(ACTION_STATUS).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, status)
        })
    }
}
