package com.example.myapplication.Core.Models

data class Device(
    var id: String = "",
    val name: String = "",
    val room: String = "",
    val type: String = "",
    val image: String = "",
    val status: String = "OFF"
)