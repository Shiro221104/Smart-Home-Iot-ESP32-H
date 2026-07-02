package com.example.myapplication.Feature.Support

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.Core.repository.DeviceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val deviceType = intent.getStringExtra("deviceType") ?: return
        val roomId = intent.getStringExtra("roomId")
        val action = intent.getStringExtra("action") ?: "ON"
        val repeatDaily = intent.getBooleanExtra("repeatDaily", true)
        val scheduleId = intent.getStringExtra("scheduleId")

        val deviceRepository = DeviceRepository()
        deviceRepository.getDevices { devices ->
            devices.filter {
                it.type == deviceType && (roomId == null || it.room == roomId)
            }.forEach { device ->
                deviceRepository.updateDeviceStatus(device.id, action)
            }
        }

        // Nếu không lặp lại hằng ngày, xoá lịch khỏi Firebase sau khi chạy xong
        if (!repeatDaily && scheduleId != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                FirebaseDatabase.getInstance()
                    .getReference("users/$userId/schedules/$scheduleId")
                    .removeValue()
            }
        }
    }
}