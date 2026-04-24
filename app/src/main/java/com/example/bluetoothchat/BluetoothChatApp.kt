package com.example.bluetoothchat

import android.app.Application
import com.example.bluetoothchat.data.bluetooth.AndroidBluetoothController
import com.example.bluetoothchat.data.chat.ChatHistoryManager
import com.example.bluetoothchat.service.NotificationHelper

/**
 * Application class that holds singleton instances of core components.
 * These persist across activity recreation and are shared with the foreground service.
 */
class BluetoothChatApp : Application() {
    val bluetoothController by lazy { AndroidBluetoothController(this) }
    val chatHistoryManager by lazy { ChatHistoryManager(this) }
    val notificationHelper by lazy { NotificationHelper(this) }
}
