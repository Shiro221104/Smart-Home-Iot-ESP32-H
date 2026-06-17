package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.Room

@Composable
fun RoomCategoryBar(
    rooms: List<Room>,          // 🔥 nhận rooms từ Firebase (thay vì dùng enum)
    devices: List<Device>,
    selectedRoom: String,
    onSelected: (String) -> Unit
) {
    // "All" + tất cả rooms từ Firebase
    val allItems: List<Pair<String, String>> =
        listOf("All" to "Tất cả") + rooms.map { it.id to it.name }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allItems) { (id, displayName) ->

            val count = if (id == "All") {
                devices.size
            } else {
                // Device.room lưu roomId (key Firebase) để khớp với Room.id
                devices.count { it.room == id }
            }

            FilterChip(
                selected = selectedRoom == id,
                onClick = { onSelected(id) },
                label = { Text("$displayName ($count)") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2196F3),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}