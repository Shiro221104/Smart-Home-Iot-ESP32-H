package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.MQTT.MQTTHandler
import com.example.myapplication.Core.ViewModels.DeviceViewModel
import com.example.myapplication.R

@Composable
fun SmartDeviceCard(
    device: Device,
    mqttHandler: MQTTHandler,
    viewModel: DeviceViewModel
) {

    var isOn by remember { mutableStateOf(device.status == "ON") }

    // 🔄 Sync khi Firebase update
    LaunchedEffect(device.status) {
        isOn = device.status == "ON"
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

        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {


                AsyncImage(
                    model = device.image,
                    contentDescription = device.name,
                    modifier = Modifier.size(40.dp),
                    placeholder = painterResource(R.drawable.lamp), // fallback
                    error = painterResource(R.drawable.lamp)        // nếu lỗi
                )

                Switch(
                    checked = isOn,
                    onCheckedChange = {
                        val newStatus = if (it) "ON" else "OFF"
                        isOn = it

                        // 📨 Gửi MQTT
                        mqttHandler.publish(device.topic, newStatus)

                        // 🔥 Update Firebase
                        viewModel.updateStatus(device.id, newStatus)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = device.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = device.room,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(
                text = if (isOn) "ON" else "OFF",
                fontSize = 12.sp,
                color = if (isOn) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}