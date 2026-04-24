package com.example.bluetoothchat.data.model

/**
 * Represents a discovered or paired Bluetooth device.
 *
 * @param name The friendly name of the device (may be null for unknown devices)
 * @param address The hardware MAC address of the device
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
)
