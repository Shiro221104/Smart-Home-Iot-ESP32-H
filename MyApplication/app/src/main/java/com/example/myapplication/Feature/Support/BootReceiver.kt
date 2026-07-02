package com.example.myapplication.Feature.Support

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.Core.Models.Schedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * AlarmManager bị huỷ hết khi điện thoại khởi động lại.
 * Receiver này nạp lại toàn bộ lịch từ Firebase và đặt báo thức lại sau khi BOOT_COMPLETED.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val appContext = context.applicationContext

        FirebaseDatabase.getInstance()
            .getReference("users/$userId/schedules")
            .get()
            .addOnSuccessListener { snapshot ->
                for (s in snapshot.children) {
                    val schedule = s.getValue(Schedule::class.java) ?: continue
                    schedule.id = s.key ?: continue
                    if (schedule.enabled) {
                        ScheduleManager.scheduleAlarm(appContext, schedule)
                    }
                }
            }
    }
}