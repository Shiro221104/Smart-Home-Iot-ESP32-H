package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Sensor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SensorRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Lắng nghe sensor data hiện tại từ Firebase
    fun listenSensorData(onData: (Sensor?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        // Lắng nghe từ device1 (hoặc device đầu tiên)
        val ref = database.getReference("users/$userId/sensors/device1")
        
        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                try {
                    val sensor = snapshot.getValue(Sensor::class.java) ?: Sensor()
                    onData(sensor)
                } catch (e: Exception) {
                    onData(Sensor())
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onData(Sensor())
            }
        })
    }

    // Update sensor data
    fun updateSensorData(sensor: Sensor) {
        val userId = auth.currentUser?.uid ?: return
        
        val newSensor = sensor.copy(userId = userId)
        database.getReference("users/$userId/sensors/current").setValue(newSensor)
    }
}