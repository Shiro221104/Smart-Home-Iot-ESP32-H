package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.Core.Models.*
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onAdd: (Device) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf(RoomType.LIVING_ROOM) }
    var type by remember { mutableStateOf(DeviceType.LIGHT) }
    var image by remember { mutableStateOf("") }

    var expandedType by remember { mutableStateOf(false) }
    var expandedRoom by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),

        title = { Text("➕ Add Device") },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    leadingIcon = { Icon(Icons.Default.Devices, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // 🏠 Room
                ExposedDropdownMenuBox(
                    expanded = expandedRoom,
                    onExpandedChange = { expandedRoom = !expandedRoom }
                ) {
                    OutlinedTextField(
                        value = room.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Room") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expandedRoom)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRoom,
                        onDismissRequest = { expandedRoom = false }
                    ) {
                        RoomType.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.displayName) },
                                onClick = {
                                    room = it
                                    expandedRoom = false
                                }
                            )
                        }
                    }
                }

                // 🔽 Type
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Device Type") },
                        leadingIcon = { Icon(Icons.Default.Category, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expandedType)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        DeviceType.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.displayName) },
                                onClick = {
                                    type = it
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("Image URL") },
                    leadingIcon = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {

                    val db = FirebaseDatabase.getInstance().getReference("devices")
                    val id = db.push().key ?: return@Button

                    val device = Device(
                        id = id, // 🔥 gán id tại đây
                        name = name,
                        room = room.code,
                        type = type.toFirebase(),
                        image = image,
                        status = if (type == DeviceType.DOOR) "CLOSED" else "OFF"
                    )

                    onAdd(device)
                }
            ) {
                Text("Add")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}