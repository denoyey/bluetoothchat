package com.example.bluetoothchat.domain

import com.example.bluetoothchat.data.model.ChatMessage

/**
 * Sealed interface representing the result of a Bluetooth connection attempt.
 */
sealed interface ConnectionResult {
    /** Connection established — includes remote device name and address */
    data class ConnectionEstablished(
        val deviceName: String?,
        val deviceAddress: String
    ) : ConnectionResult

    /** A chat message was received over the connection */
    data class MessageReceived(val message: ChatMessage) : ConnectionResult

    /** An error occurred during connection */
    data class Error(val errorMessage: String) : ConnectionResult
}
