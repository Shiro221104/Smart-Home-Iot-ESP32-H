package com.example.myapplication.Core.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.Core.Models.Notification
import com.example.myapplication.Core.repository.NotificationRepository

class NotificationViewModel : ViewModel() {

    private val repository = NotificationRepository()

    var notifications = mutableStateOf<List<Notification>>(emptyList())
        private set

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        repository.getNotifications { list ->
            notifications.value = list
        }
    }
}