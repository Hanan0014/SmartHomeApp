package com.smarthome.app.data.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smarthome.app.R

/**
 * Receives push notifications sent by the Cloud Function safety-cutoff worker
 * when a safety-critical device (e.g. an iron) exceeds its max_on_duration
 * and has been force-flipped to OFF server-side.
 */
class SmartHomeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Safety Alert"
        val body = message.notification?.body ?: "A device was automatically turned off."
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // In a full implementation, persist this token under
        // /users/{uid}/fcmTokens/{token} so the Cloud Function can target this device.
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "safety_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Safety Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
