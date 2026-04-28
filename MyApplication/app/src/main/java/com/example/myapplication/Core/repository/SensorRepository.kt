package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Sensor
import com.google.firebase.database.*

class SensorRepository {

    private val ref = FirebaseDatabase.getInstance()
        .getReference("sensors/device1/current")

    fun listenSensorData(onData: (Sensor?) -> Unit) {

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(Sensor::class.java)
                onData(data)
            }

            override fun onCancelled(error: DatabaseError) {
                onData(null)
            }
        })
    }
}