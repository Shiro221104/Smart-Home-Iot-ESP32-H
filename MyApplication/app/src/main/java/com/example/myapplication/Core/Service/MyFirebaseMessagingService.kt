package com.example.myapplication.Core.Service
import com.example.myapplication.Core.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage)
    {
        val title = remoteMessage.notification?.title ?: ""
        val message = remoteMessage.notification?.body ?: ""
        NotificationHelper.show(this, title, message)
    }
}