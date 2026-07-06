package com.example.myapplication.Core.Models

data class Schedule(
    var id: String = "",
    val deviceType: String = "",   // "light" / "fan"
    val roomId: String? = null,
    val roomName: String? = null,
    val action: String = "ON",     // ON / OFF
    val hour: Int = 0,
    val minute: Int = 0,
    val repeatDaily: Boolean = true,
    val scheduleDate: String? = null,  // ISO date format "yyyy-MM-dd" for single-day schedules, null for daily
    val enabled: Boolean = true
)
