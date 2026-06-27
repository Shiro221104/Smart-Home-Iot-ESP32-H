package com.example.myapplication.Core.Models

data class Notification(
    val message: String = "",
    val title: String = "",
    val time: Long = 0,
    val userId: String = ""
)