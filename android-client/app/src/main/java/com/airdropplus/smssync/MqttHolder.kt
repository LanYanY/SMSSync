package com.airdropplus.smssync

object MqttHolder {
    @Volatile
    var mqttManager: MqttManager? = null
}
