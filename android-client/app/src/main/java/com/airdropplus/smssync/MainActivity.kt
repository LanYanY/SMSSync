package com.airdropplus.smssync

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MqttSyncService.ACTION_STATUS) {
                val msg = intent.getStringExtra(MqttSyncService.EXTRA_STATUS) ?: return
                statusText.text = msg
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        val brokerInput = findViewById<EditText>(R.id.brokerInput)
        val appIdInput = findViewById<EditText>(R.id.appIdInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        val prefs = getSharedPreferences(MqttSyncService.PREFS_NAME, Context.MODE_PRIVATE)
        brokerInput.setText(prefs.getString(MqttSyncService.KEY_BROKER, "tcp://broker.emqx.io:1883"))
        appIdInput.setText(prefs.getString(MqttSyncService.KEY_APP_ID, "demo-room"))
        usernameInput.setText(prefs.getString(MqttSyncService.KEY_USERNAME, ""))
        passwordInput.setText(prefs.getString(MqttSyncService.KEY_PASSWORD, ""))

        findViewById<Button>(R.id.connectBtn).setOnClickListener {
            val broker = brokerInput.text.toString().trim()
            val appId = appIdInput.text.toString().trim()
            val username = usernameInput.text.toString().trim().ifEmpty { null }
            val password = passwordInput.text.toString().trim().ifEmpty { null }

            MqttSyncService.start(this, broker, appId, username, password)
            statusText.text = "后台服务启动中..."
        }

        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            MqttSyncService.stop(this)
            statusText.text = "已停止后台同步"
        }

        ensurePermissions()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(statusReceiver, IntentFilter(MqttSyncService.ACTION_STATUS))
    }

    override fun onStop() {
        unregisterReceiver(statusReceiver)
        super.onStop()
    }

    private fun ensurePermissions() {
        val required = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val need = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 1001)
        }
    }
}
