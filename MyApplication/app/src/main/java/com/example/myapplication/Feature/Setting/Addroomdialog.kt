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
import com.google.firebase.database.FirebaseDatabase

@Composable
fun AddRoomDialog(
    onDismiss: () -> Unit,
    onAdd: (Room) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

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
                    },
                    label = { Text("Tên phòng") },
                    leadingIcon = { Icon(Icons.Default.Home, null) },
                    isError = isError,
                    supportingText = {
                        if (isError) Text("Vui lòng nhập tên phòng")
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
                        return@Button
                    }

                    // Lưu lên Firebase theo cấu trúc: rooms/{pushKey}/id + name
                    val db = FirebaseDatabase.getInstance().getReference("rooms")
                    val key = db.push().key ?: return@Button

                    val room = Room(id = key, name = name.trim())

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
