package com.example.myapplication.Feature.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.MQTT.MQTTHandler

@Composable
fun SmartDeviceCard() {

    val context = LocalContext.current
    val mqttHandler = remember { MQTTHandler() }

    var isOn by remember { mutableStateOf(false) }

    // 🔥 connect MQTT 1 lần
    LaunchedEffect(Unit) {
        mqttHandler.connect(context)

        mqttHandler.setCallback { topic, message ->
            if (topic == "esp32/lamp/status") {
                isOn = message == "ON"
            }
        }
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(id = R.drawable.lamp),
                    contentDescription = "Lamp",
                    modifier = Modifier.size(40.dp)
                )

                Switch(
                    checked = isOn,
                    onCheckedChange = {
                        isOn = it
                        mqttHandler.controlLamp(it) // 🔥 gửi MQTT
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Smart Lamp",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isOn) "ON" else "OFF",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
