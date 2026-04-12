package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.MQTT.MQTTHandler


import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    mqttHandler: MQTTHandler? = null
){



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

    ) { inner ->

        LazyColumn(contentPadding = inner) {
                item { HomeHeader() }


            item { Banner(temperature,humidity) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Devices",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            item{SmartDeviceCard()}

        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview(){
    HomeScreen()
}