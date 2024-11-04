package com.example.poe2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.poe2.MainActivity

class NotificationReceiver(
    private val notificationsList: MutableList<String>,
    private val notificationsAdapter: ArrayAdapter<String>

) : BroadcastReceiver() {

    override fun onReceive(context: Context, receivedIntent: Intent) {
        val message = receivedIntent.getStringExtra("message")
        val timestamp = receivedIntent.getLongExtra("timestamp", 0L)

        Log.d("NotificationReceiver", "Broadcast received with message: $message at timestamp: $timestamp")

        if (message != null) {
            notificationsList.add(message)
            notificationsAdapter.notifyDataSetChanged()
            Toast.makeText(context, "New notification received", Toast.LENGTH_SHORT).show()
            showNotification(context, message)
        } else {
            Log.e("NotificationReceiver", "Received null message in broadcast")
        }
    }

    private fun showNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "notification_channel"

        // Create a notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent to open your app when the notification is tapped
        val intent = Intent(context, MainActivity::class.java) // Replace with your main activity
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
            .setContentTitle("New Notification")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Notify the user
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
