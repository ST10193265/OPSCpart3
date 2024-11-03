package com.example.poe2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.poe2.MainActivity
import com.example.poe2.R

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCMService", "Received message from: ${remoteMessage.from}")

        // Log data payload if present
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCMService", "Message data payload: ${remoteMessage.data}")
            sendNotification(remoteMessage.data["message"] ?: "New notification from data payload")
        }

        // Log notification payload if present
        remoteMessage.notification?.let {
            Log.d("FCMService", "Message Notification Body: ${it.body}")
            sendNotification(it.body ?: "New notification from notification payload")
        }
    }

    private fun sendNotification(messageBody: String) {
        Log.d("FCMService", "Sending notification with message: $messageBody")

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "YourChannelId"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.redcircle)
            .setContentTitle("FCM Notification")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("FCMService", "Refreshed token: $token")
    }
}



