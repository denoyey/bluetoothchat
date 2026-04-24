package com.example.bluetoothchat.domain

import com.example.bluetoothchat.data.model.ChatMessage

/**
 * Unified event system for all Bluetooth activity.
 */
sealed interface BluetoothEvent {
    data class Connected(val deviceAddress: String, val deviceName: String) : BluetoothEvent
    data class Disconnected(val deviceAddress: String) : BluetoothEvent
    data class MessageReceived(val deviceAddress: String, val message: ChatMessage) : BluetoothEvent
    data class Error(val message: String) : BluetoothEvent
}
