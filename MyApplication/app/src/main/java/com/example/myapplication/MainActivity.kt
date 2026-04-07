package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.Feature.home.MainScreen
import com.example.myapplication.MQTT.MQTTHandler

class MainActivity : ComponentActivity() {

    private lateinit var mqttHandler: MQTTHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mqttHandler = MQTTHandler()
        mqttHandler.connect(this)

        setContent {
            MainScreen(mqttHandler)
        }
    }
}