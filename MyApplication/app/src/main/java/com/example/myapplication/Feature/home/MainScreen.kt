package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.MQTT.MQTTHandler



import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*

@Composable
fun MainScreen(
    mqttHandler: MQTTHandler? = null
){
    var selectedBottom by remember { mutableStateOf(0) }


    var temperature by remember { mutableStateOf("0°C") }
    var humidity by remember { mutableStateOf("0%") }

    LaunchedEffect(Unit) {
        mqttHandler?.setCallback { topic, message ->


            if (topic == "esp32/dht11/temperature") {
                temperature = "$message°C"
            }
            if (topic == "esp32/dht11/humidity") {
                humidity = "$message%"
            }
        }
    }

    Scaffold(
        contentColor = Color.White,
        bottomBar = {
            HomeBottomBar(
                selected = selectedBottom,
                onSelect = { selectedBottom = it }
            )
        }
    ) { inner ->

        LazyColumn(contentPadding = inner) {
                item { HomeHeader() }


            item { Banner(temperature,humidity) }

        }
    }
}

@Preview
@Composable
fun MainScreenPreview(){
    MainScreen()
}