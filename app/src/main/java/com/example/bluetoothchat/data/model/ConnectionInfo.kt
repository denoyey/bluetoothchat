package com.example.bluetoothchat.data.model

/**
 * Info about an active Bluetooth connection.
 */
data class ConnectionInfo(
    val deviceName: String,
    val deviceAddress: String,
    val connectedSince: Long = System.currentTimeMillis()
)
