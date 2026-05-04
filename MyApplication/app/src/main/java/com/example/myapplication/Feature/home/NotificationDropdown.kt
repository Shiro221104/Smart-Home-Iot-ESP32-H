package com.example.myapplication.Feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 📦 Model Notification
data class NotificationItem(
    val title: String,
    val message: String,
    val time: String,
    val isOn: Boolean
)

@Composable
fun NotificationDropdown() {

    // 🔥 Fake data (sau này thay bằng Firebase)
    val notifications = listOf(
        NotificationItem("Living Room Light", "Turned ON", "2 min ago", true),
        NotificationItem("Kitchen Light", "Turned OFF", "5 min ago", false),
        NotificationItem("Bedroom Fan", "Turned ON", "10 min ago", true),
        NotificationItem("Main Door", "Opened", "15 min ago", true)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Column(modifier = Modifier.padding(12.dp)) {

            // 🔔 Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Text(
                    text = "Notifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📜 List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp) // dropdown style
            ) {
                items(notifications) { item ->
                    NotificationCard(item)
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem) {

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isOn)
                Color(0xFFE3F2FD)
            else
                Color(0xFFF5F5F5)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                Icon(
                    imageVector = if (item.isOn)
                        Icons.Default.Lightbulb
                    else
                        Icons.Default.Power,
                    contentDescription = null
                )

                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.message,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Text(
                text = item.time,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationDropdownPreview() {
    NotificationDropdown()
}