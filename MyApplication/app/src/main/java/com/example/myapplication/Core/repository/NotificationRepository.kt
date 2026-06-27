package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NotificationRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lấy notifications của user hiện tại
    fun getNotifications(onResult: (List<Notification>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        val ref = database.getReference("users/$userId/notifications")
        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<Notification>()
                for (notifSnapshot in snapshot.children) {
                    val notification = notifSnapshot.getValue(Notification::class.java)
                    if (notification != null) {
                        list.add(notification)
                    }
                }
                // Sắp xếp theo time descending
                list.sortByDescending { it.time }
                onResult(list)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onResult(emptyList())
            }
        })
    }

    // Thêm notification
    fun addNotification(notification: Notification) {
        val userId = auth.currentUser?.uid ?: return

        val newNotif = notification.copy(userId = userId, time = System.currentTimeMillis())
        database.getReference("users/$userId/notifications").push().setValue(newNotif)
    }
}