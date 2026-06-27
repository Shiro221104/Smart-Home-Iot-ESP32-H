package com.example.myapplication.Feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.Core.Models.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onAdd: (Room) -> Unit,
    existingRooms: List<Room> = emptyList()
) {
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),

        title = { Text(text = "Thêm Phòng", fontWeight = FontWeight.Bold) },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                        errorMessage = ""
                    },
                    label = { Text("Tên phòng") },
                    leadingIcon = { Icon(Icons.Default.Home, null) },
                    isError = isError,
                    supportingText = {
                        if (isError) Text(errorMessage)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        errorMessage = "Vui lòng nhập tên phòng"
                        return@Button
                    }

                    // Kiểm tra tên phòng trùng (không phân biệt hoa thường)
                    val trimmedName = name.trim()
                    val isDuplicate = existingRooms.any { 
                        it.name.lowercase() == trimmedName.lowercase() 
                    }
                    
                    if (isDuplicate) {
                        isError = true
                        errorMessage = "Tên phòng này đã tồn tại"
                        return@Button
                    }

                    // Lấy user ID hiện tại
                    val userId = auth.currentUser?.uid ?: return@Button

                    // Lưu lên Firebase theo cấu trúc: users/{userId}/rooms/{pushKey}/
                    val db = FirebaseDatabase.getInstance().getReference("users/$userId/rooms")
                    val key = db.push().key ?: return@Button

                    val room = Room(
                        id = key,
                        name = trimmedName,
                        userId = userId
                    )

                    db.child(key).setValue(room)
                        .addOnSuccessListener { onAdd(room) }
                }
            ) {
                Text("Thêm")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
