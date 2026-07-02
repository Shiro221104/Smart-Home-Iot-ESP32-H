package com.example.myapplication.Feature.Support

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.myapplication.Core.Models.Schedule
import java.util.Calendar

object ScheduleManager {

    private fun buildPendingIntent(context: Context, schedule: Schedule): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra("deviceType", schedule.deviceType)
            putExtra("roomId", schedule.roomId)
            putExtra("action", schedule.action)
            putExtra("repeatDaily", schedule.repeatDaily)
            putExtra("scheduleId", schedule.id)
        }
        val requestCode = schedule.id.hashCode()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleAlarm(context: Context, schedule: Schedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, schedule)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1) // nếu giờ đã qua trong ngày -> đặt cho ngày mai
            }
        }

        if (schedule.repeatDaily) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else {
            // Lưu ý: Android 12+ (API 31+) cần quyền SCHEDULE_EXACT_ALARM
            // và canScheduleExactAlarms() == true để dùng setExactAndAllowWhileIdle.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, schedule: Schedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, schedule)
        alarmManager.cancel(pendingIntent)
    }
}