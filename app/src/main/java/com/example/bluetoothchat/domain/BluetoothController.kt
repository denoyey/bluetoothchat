package com.example.bluetoothchat.domain

import com.example.bluetoothchat.data.model.BluetoothDeviceInfo
import com.example.bluetoothchat.data.model.ChatMessage
import com.example.bluetoothchat.data.model.ConnectionInfo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Multi-connection Bluetooth controller interface.
 */
interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>>
    val activeConnections: StateFlow<Map<String, ConnectionInfo>>
    val events: SharedFlow<BluetoothEvent>

    fun startDiscovery()
    fun stopDiscovery()

    /** Start server — continuously accepts incoming connections */
    fun startServer()

    /** Connect to a device as client */
    fun connectToDevice(device: BluetoothDeviceInfo)

    /** Send message to a specific connected device */
    suspend fun sendMessage(address: String, message: String): ChatMessage?

    /** Close a specific connection */
    fun closeConnection(address: String)

    /** Close all connections and release resources */
    fun release()
}
