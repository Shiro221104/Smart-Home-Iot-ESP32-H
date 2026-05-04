package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Notification
import com.google.firebase.database.*

class NotificationRepository {

    private val dbRef = FirebaseDatabase
        .getInstance()
        .getReference("notifications")

    fun getNotifications(onResult: (List<Notification>) -> Unit) {

        dbRef.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val list = mutableListOf<Notification>()

                for (child in snapshot.children) {
                    val noti = child.getValue(Notification::class.java)
                    if (noti != null) {
                        list.add(noti)
                    }
                }

                list.sortByDescending { it.time }

                onResult(list)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyList())
            }
        })
    }
}