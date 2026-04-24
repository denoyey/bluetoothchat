package com.example.bluetoothchat.data.bluetooth

import java.util.UUID

/**
 * Constants used for Bluetooth communication.
 */
object BluetoothConstants {
    /** Service name for SDP record */
    const val SERVICE_NAME = "BluetoothChatService"

    /** UUID for RFCOMM channel — must be the same on both devices */
    val SERVICE_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    /** Buffer size for reading data from Bluetooth socket */
    const val BUFFER_SIZE = 1024
}
