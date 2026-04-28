package com.example.myapplication.Core.Models

enum class RoomType(
    val code: String,
    val displayName: String
) {
    LIVING_ROOM("livingroom", "Phòng khách"),
    BEDROOM("bedroom", "Phòng ngủ"),
    KITCHEN("kitchen", "Phòng bếp");

    companion object {
        fun fromCode(code: String): RoomType {
            return entries.find { it.code == code } ?: LIVING_ROOM
        }
    }
}