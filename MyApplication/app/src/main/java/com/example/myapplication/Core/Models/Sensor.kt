package com.example.myapplication.Core.Models

data class Sensor(
    var temperature: Float = 0f,
    var humidity: Float = 0f,
    var gas_value: Int = 0,
    var gas_detected: Int = 0,
    var gas_status: String = "",
    val userId: String = ""
)