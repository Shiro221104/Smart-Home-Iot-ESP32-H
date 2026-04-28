package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.RoomType
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun RoomCategoryBar(
    devices: List<Device>,
    selectedRoom: String,
    onSelected: (String) -> Unit
) {

    // 🔥 dùng enum thay vì lấy từ device
    val rooms = listOf("All") + RoomType.values().map { it.code }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(rooms) { room ->

            val count = if (room == "All") {
                devices.size
            } else {
                devices.count { it.room == room }
            }

            val displayName = if (room == "All") {
                "All"
            } else {
                RoomType.fromCode(room).displayName
            }

            FilterChip(
                selected = selectedRoom == room,
                onClick = { onSelected(room) },
                label = {
                    Text("$displayName ($count)")
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}