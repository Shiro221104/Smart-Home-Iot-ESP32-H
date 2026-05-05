package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.example.myapplication.MQTT.MQTTHandler
import com.example.myapplication.Navigation.MainScreen
import com.google.firebase.database.*
import com.example.myapplication.Core.utils.NotificationHelper
class MainActivity : ComponentActivity() {

    private lateinit var mqttHandler: MQTTHandler
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

        // MQTT
        mqttHandler = MQTTHandler()
        mqttHandler.connect(this)

        // Firebase listen notifications
        listenFirebaseNotification()

        setContent {
            MainScreen(mqttHandler)
        }
    }

    private fun listenFirebaseNotification() {

        dbRef = FirebaseDatabase
            .getInstance()
            .getReference("notifications")

        dbRef.limitToLast(1)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {

                    val title = snapshot.child("title")
                        .getValue(String::class.java) ?: "Cảnh báo"

                    val message = snapshot.child("message")
                        .getValue(String::class.java) ?: ""

                    NotificationHelper.show(
                        this@MainActivity,
                        title,
                        message
                    )
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