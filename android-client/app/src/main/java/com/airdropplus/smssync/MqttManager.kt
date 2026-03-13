package com.airdropplus.smssync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

class MqttManager(private val context: Context) {
    private var mqttClient: MqttAsyncClient? = null
    private var appId: String = ""

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    }

    fun connect(
        brokerUrl: String,
        appId: String,
        username: String?,
        password: String?,
        onStatus: (String) -> Unit
    ) {
        this.appId = appId
        val clientId = "android-$deviceId"

        mqttClient = MqttAsyncClient(brokerUrl, clientId, null).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    onStatus("连接断开: ${cause?.message ?: "unknown"}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (message == null) return
                    val payload = message.toString()
                    try {
                        val obj = JSONObject(payload)
                        val source = obj.optString("source_device", "unknown")
                        if (source == deviceId) return
                        val code = obj.optString("code")
                        val smsBody = obj.optString("sms_body")
                        val toCopy = if (code.isNotBlank()) code else smsBody
                        copyToClipboard(toCopy)
                        onStatus("收到验证码并复制: $toCopy")
                    } catch (e: Exception) {
                        onStatus("收到无效消息: ${e.message}")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                }
            })
        }

        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            if (!username.isNullOrBlank()) {
                userName = username
                this.password = password?.toCharArray()
            }
        }

        mqttClient?.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                onStatus("MQTT连接成功")
                subscribeSmsTopic(onStatus)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                onStatus("MQTT连接失败: ${exception?.message}")
            }
        })
    }

    private fun subscribeSmsTopic(onStatus: (String) -> Unit) {
        mqttClient?.subscribe(topicSms(), 1, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                onStatus("订阅成功: ${topicSms()}")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                onStatus("订阅失败: ${exception?.message}")
            }
        })
    }

    fun publishSms(code: String?, smsBody: String) {
        val obj = JSONObject().apply {
            put("source_device", deviceId)
            put("platform", "android")
            put("timestamp", Instant.now().toString())
            put("sms_body", smsBody)
            put("code", code ?: "")
        }

        val message = MqttMessage(obj.toString().toByteArray()).apply {
            qos = 1
        }
        mqttClient?.publish(topicSms(), message)
    }

    fun isConnected(): Boolean = mqttClient?.isConnected == true

    fun disconnect() {
        if (mqttClient?.isConnected == true) {
            mqttClient?.disconnect()
        }
        mqttClient?.close()
    }

    private fun topicSms(): String = "sms-sync/$appId/sms"

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("sms-code", text)
        clipboard.setPrimaryClip(clip)
    }
}
