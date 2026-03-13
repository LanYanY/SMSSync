package com.airdropplus.smssync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var mqttManager: MqttManager
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mqttManager = MqttManager(applicationContext)
        statusText = findViewById(R.id.statusText)

        val brokerInput = findViewById<EditText>(R.id.brokerInput)
        val appIdInput = findViewById<EditText>(R.id.appIdInput)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        val prefs = getSharedPreferences("sms_sync", Context.MODE_PRIVATE)
        brokerInput.setText(prefs.getString("broker", "tcp://broker.emqx.io:1883"))
        appIdInput.setText(prefs.getString("app_id", "demo-room"))
        usernameInput.setText(prefs.getString("username", ""))
        passwordInput.setText(prefs.getString("password", ""))

        findViewById<Button>(R.id.connectBtn).setOnClickListener {
            val broker = brokerInput.text.toString().trim()
            val appId = appIdInput.text.toString().trim()
            val username = usernameInput.text.toString().trim().ifEmpty { null }
            val password = passwordInput.text.toString().trim().ifEmpty { null }
            prefs.edit()
                .putString("broker", broker)
                .putString("app_id", appId)
                .putString("username", username ?: "")
                .putString("password", password ?: "")
                .apply()

            mqttManager.connect(broker, appId, username, password) {
                runOnUiThread { statusText.text = it }
            }
            SmsReceiver.mqttManager = mqttManager
        }

        ensurePermissions()
    }

    private fun ensurePermissions() {
        val required = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        val need = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保留连接，不主动断开
    }
}
