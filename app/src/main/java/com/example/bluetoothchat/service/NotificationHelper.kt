package com.example.bluetoothchat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bluetoothchat.MainActivity
import com.example.bluetoothchat.R

/**
 * Manages notification channels and message notifications.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val SERVICE_CHANNEL_ID = "bluetooth_service"
        const val MESSAGE_CHANNEL_ID = "chat_messages"
        const val SERVICE_NOTIFICATION_ID = 1
        private const val MESSAGE_NOTIFICATION_BASE_ID = 100
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        // Silent service channel
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Bluetooth Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Bluetooth connections alive"
            setShowBadge(false)
        }

        // Message channel with default sound
        val messageChannel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            "Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming chat messages"
            enableVibration(true)
        }

        notificationManager.createNotificationChannels(listOf(serviceChannel, messageChannel))
    }

    /**
     * Build the persistent foreground service notification.
     */
    fun buildServiceNotification(activeCount: Int): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (activeCount > 0) "$activeCount active connection(s)"
        else "Listening for connections"

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BluetoothChat")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    /**
     * Show a notification for an incoming message.
     * Uses device address hash as notification ID for per-conversation grouping.
     */
    fun showMessageNotification(deviceName: String, deviceAddress: String, messageText: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_chat_address", deviceAddress)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, deviceAddress.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = MESSAGE_NOTIFICATION_BASE_ID + (deviceAddress.hashCode() and 0x7FFFFFFF) % 1000

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(deviceName)
            .setContentText(messageText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        notificationManager.notify(notifId, notification)
    }

    /**
     * Dismiss message notification for a specific device.
     */
    fun dismissNotification(deviceAddress: String) {
        val notifId = MESSAGE_NOTIFICATION_BASE_ID + (deviceAddress.hashCode() and 0x7FFFFFFF) % 1000
        notificationManager.cancel(notifId)
    }

    /**
     * Update service notification with current connection count.
     */
    fun updateServiceNotification(activeCount: Int) {
        notificationManager.notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(activeCount))
    }
}
