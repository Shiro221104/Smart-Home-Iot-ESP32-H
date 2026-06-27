package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.example.myapplication.Navigation.MainScreen
import com.example.myapplication.Core.Models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.myapplication.Core.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // xin quyền thông báo Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        // Firebase listen notifications
        listenFirebaseNotification()

        setContent {
            MainScreen()
        }
    }

    private fun listenFirebaseNotification() {
        // Lấy user ID hiện tại
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        dbRef = FirebaseDatabase
            .getInstance()
            .getReference("users/$userId/notifications")

        dbRef.addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                    // Chuyển đổi sang Notification object
                    val notification = snapshot.getValue(Notification::class.java)
                    
                    if (notification != null) {
                        NotificationHelper.show(
                            this@MainActivity,
                            notification.title,
                            notification.message
                        )
                    }
                }

                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}