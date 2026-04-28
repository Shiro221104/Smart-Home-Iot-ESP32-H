package com.example.myapplication.Core.Models

enum class DeviceType(val displayName: String) {
    LIGHT("Light"),
    FAN("Fan"),
    DOOR("Door");

    fun toFirebase(): String {
        return name.lowercase()
    }

    companion object {
        fun fromString(value: String): DeviceType {
            return entries.find { it.name.equals(value, true) } ?: LIGHT
        }
    }
}