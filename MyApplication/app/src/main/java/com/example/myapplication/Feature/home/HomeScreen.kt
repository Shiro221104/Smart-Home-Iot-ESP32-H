package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.MQTT.MQTTHandler
import com.example.myapplication.Core.ViewModels.DeviceViewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
@Composable
fun HomeScreen(
    mqttHandler: MQTTHandler
) {

    val viewModel: DeviceViewModel = viewModel()
    val devices = viewModel.devices

    var temperature by remember { mutableStateOf("0°C") }
    var humidity by remember { mutableStateOf("0%") }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    LaunchedEffect(Unit) {
        mqttHandler.setCallback { topic, message ->
            when (topic) {
                "esp32/dht11/temperature" -> temperature = "$message°C"
                "esp32/dht11/humidity" -> humidity = "$message%"
            }
        }
    }

    Scaffold { inner ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 🔥 2 cột
            contentPadding = inner,
            modifier = Modifier.padding(8.dp)
        ) {


            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                HomeHeader()
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Banner(temperature, humidity)
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text(
                    text = "All Devices",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }


            items(devices) { device ->
                SmartDeviceCard(
                    device = device,
                    mqttHandler = mqttHandler,
                    viewModel = viewModel
                )
            }
        }
    }
}