package com.example.myapplication.Core.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    fun formatFull(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    fun timeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestamp

        return when {
            diff < 60 -> "Vừa xong"

            diff < 3600 -> "${diff / 60} phút trước"

            diff < 86400 -> "${diff / 3600} giờ trước"

            diff < 172800 -> "Hôm qua" // < 2 ngày

            diff < 604800 -> "${diff / 86400} ngày trước" // <

            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp * 1000))
            }
        }
    }
    fun isToday(timestamp: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()

        cal2.time = java.util.Date(timestamp * 1000)

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
    fun isYesterday(timestamp: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()

        cal1.add(java.util.Calendar.DAY_OF_YEAR, -1)
        cal2.time = java.util.Date(timestamp * 1000)

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}