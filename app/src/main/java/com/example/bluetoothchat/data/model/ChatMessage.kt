package com.example.bluetoothchat.data.model

/**
 * Represents a single chat message in the Bluetooth chat.
 *
 * @param content The text content of the message
 * @param senderName The display name of the sender
 * @param timestamp The time the message was sent (epoch millis)
 * @param isFromLocalUser Whether this message was sent by the local user
 */
data class ChatMessage(
    val content: String,
    val senderName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromLocalUser: Boolean
)
