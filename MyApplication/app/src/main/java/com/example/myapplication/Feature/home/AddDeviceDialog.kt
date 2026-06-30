package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.Core.Models.Device
import com.example.myapplication.Core.Models.DeviceType
import com.example.myapplication.Core.Models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    rooms: List<Room>,
    existingDevices: List<Device>,
    onDismiss: () -> Unit,
    onAdd: (Device) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedRoom by remember(rooms) { mutableStateOf(rooms.firstOrNull()) }
    var type by remember { mutableStateOf(DeviceType.LIGHT) }
    var image by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var expandedType by remember { mutableStateOf(false) }
    var expandedRoom by remember { mutableStateOf(false) }

    val isDuplicate = name.isNotBlank() && existingDevices.any {
        it.name.equals(name.trim(), ignoreCase = true) &&
                it.room == selectedRoom?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(text = "Thêm Thiết Bị", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Device Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Thiết Bị") },
                    leadingIcon = { Icon(Icons.Default.Devices, null) },
                    isError = isDuplicate,
                    supportingText = if (isDuplicate) {
                        { Text("Thiết bị này đã tồn tại trong phòng") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Room — load từ Firebase
                ExposedDropdownMenuBox(
                    expanded = expandedRoom,
                    onExpandedChange = { expandedRoom = !expandedRoom }
                ) {
                    OutlinedTextField(
                        value = selectedRoom?.name ?: "Chưa có phòng",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedRoom) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRoom,
                        onDismissRequest = { expandedRoom = false }
                    ) {
                        if (rooms.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Chưa có phòng — hãy thêm phòng trước") },
                                onClick = { expandedRoom = false },
                                enabled = false
                            )
                        } else {
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = { Text(room.name) },
                                    onClick = {
                                        selectedRoom = room
                                        expandedRoom = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Device Type
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Category, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedType) },
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
                                    if (it != DeviceType.DOOR) password = ""
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Image URL
                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("Image URL") },
                    leadingIcon = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password (chỉ hiện khi type là DOOR)
                if (type == DeviceType.DOOR) {
                    val isInvalid = password.isNotEmpty() && (password.length != 4 || !password.all { it.isDigit() })
                    OutlinedTextField(
                        value = password,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) password = it },
                        label = { Text("Mật khẩu cửa (4 chữ số)") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        isError = isInvalid,
                        supportingText = if (isInvalid) {
                            { Text("Nhập đúng 4 chữ số") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedRoom != null && name.isNotBlank() && !isDuplicate &&
                        (type != DeviceType.DOOR || (password.length == 4 && password.all { it.isDigit() })),
                onClick = {
                    val room = selectedRoom ?: return@Button
                    val auth = FirebaseAuth.getInstance()
                    val userId = auth.currentUser?.uid ?: return@Button

                    val db = FirebaseDatabase.getInstance().getReference("users/$userId/devices")
                    val id = db.push().key ?: return@Button

                    val device = Device(
                        id = id,
                        name = name.trim(),
                        room = room.id,
                        type = type.toFirebase(),
                        image = image,
                        status = "OFF",
                        password = if (type == DeviceType.DOOR) password else null,
                        userId = userId
                    )
                    onAdd(device)
                }
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}