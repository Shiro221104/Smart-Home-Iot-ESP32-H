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

import com.example.myapplication.Core.ViewModels.DeviceViewModel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.example.myapplication.Core.ViewModels.SensorViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
@Composable
fun HomeScreen(

) {

    val deviceViewModel: DeviceViewModel = viewModel()
    val sensorViewModel: SensorViewModel = viewModel()

    val devices = deviceViewModel.devices
    val sensor = sensorViewModel.sensor.value

    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        deviceViewModel.loadDevices()
    }

    val filteredDevices = if (selectedRoom == "All") {
        devices
    } else {
        devices.filter { it.room == selectedRoom }
    }

    Scaffold { inner ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = inner,
            modifier = Modifier.padding(8.dp)
        ) {

            item(span = { GridItemSpan(2) }) {
                HomeHeader()
            }

            item(span = { GridItemSpan(2) }) {
                Banner(
                    temp = "${sensor.temperature}°C",
                    humidity = "${sensor.humidity}%"
                )
            }

            // 🔥 TITLE + MENU 3 CHẤM
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "All Devices",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add Device") },
                                onClick = {
                                    expanded = false
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                RoomCategoryBar(
                    devices = devices,
                    selectedRoom = selectedRoom,
                    onSelected = { selectedRoom = it }
                )
            }

            items(filteredDevices) { device ->
                SmartDeviceCard(
                    deviceId = device.id,
                    device = device,
                    viewModel = deviceViewModel
                )
            }
        }
    }


    if (showAddDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { device ->
                deviceViewModel.addDevice(device)
                showAddDialog = false
            }
        )
    }
}