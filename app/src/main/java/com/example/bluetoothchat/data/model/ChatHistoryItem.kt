package com.example.bluetoothchat.data.model

/**
 * Represents a previously connected device with its chat history.
 */
data class ChatHistoryItem(
    val deviceName: String,
    val deviceAddress: String,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)
