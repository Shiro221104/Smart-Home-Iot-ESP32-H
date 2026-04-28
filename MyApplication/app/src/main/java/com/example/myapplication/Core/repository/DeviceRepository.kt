package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Device

import com.google.firebase.database.*

class DeviceRepository {

    private val db = FirebaseDatabase
        .getInstance()
        .getReference("devices")

    // 🔥 Lấy danh sách realtime
    fun getDevices(callback: (List<Device>) -> Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val list = mutableListOf<Device>()

                for (child in snapshot.children) {
                    val device = child.getValue(Device::class.java)


                    device?.apply { id = child.key ?: ""  }?.let { list.add(it) }                }

                callback(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addDevice(device: Device) {
        val id = db.push().key ?: return

        val newDevice = device.copy(id = id)

        db.child(id).setValue(newDevice)
    }

    fun updateDeviceStatus(deviceId: String, status: String) {
        db.child(deviceId).child("status").setValue(status)
    }

    fun deleteDevice(deviceId: String) {
        db.child(deviceId).removeValue()
    }
}