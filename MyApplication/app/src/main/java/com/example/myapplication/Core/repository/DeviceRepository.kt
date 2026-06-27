package com.example.myapplication.Core.repository

import com.example.myapplication.Core.Models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DeviceRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 🔥 Lấy danh sách device của user
    fun getDevices(callback: (List<Device>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        val ref = database.getReference("users/$userId/devices")
        ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<Device>()
                for (deviceSnapshot in snapshot.children) {
                    val device = deviceSnapshot.getValue(Device::class.java)
                    if (device != null) {
                        device.id = deviceSnapshot.key ?: ""
                        list.add(device)
                    }
                }
                callback(list)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // Thêm device vào database của user
    fun addDevice(device: Device) {
        val userId = auth.currentUser?.uid ?: return
        
        val newDevice = device.copy(userId = userId)
        val ref = database.getReference("users/$userId/devices").push()
        ref.setValue(newDevice)
    }

    // Update trạng thái device
    fun updateDeviceStatus(deviceId: String, status: String) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users/$userId/devices").child(deviceId)
            .child("status").setValue(status)
    }

    // Xóa device
    fun deleteDevice(deviceId: String) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users/$userId/devices").child(deviceId).removeValue()
    }
}