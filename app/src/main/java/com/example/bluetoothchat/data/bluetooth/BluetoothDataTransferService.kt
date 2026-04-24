package com.example.bluetoothchat.data.bluetooth

import android.bluetooth.BluetoothSocket
import com.example.bluetoothchat.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

/**
 * Handles data transfer over a single Bluetooth socket.
 * Security: escapes delimiter, validates buffer, enforces max message size.
 */
class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    companion object {
        private const val DELIMITER = ":::"
        private const val ESCAPED_DELIMITER = "::;:"
        private const val MAX_MESSAGE_LENGTH = 1000
    }

    fun listenForIncomingMessages(): Flow<ChatMessage> = flow {
        if (!socket.isConnected) return@flow
        val buffer = ByteArray(BluetoothConstants.BUFFER_SIZE)
        val inputStream = socket.inputStream

        while (coroutineContext.isActive) {
            val bytesRead = try {
                inputStream.read(buffer)
            } catch (e: IOException) {
                return@flow
            }
            if (bytesRead == -1) return@flow
            if (bytesRead <= 0 || bytesRead > buffer.size) continue

            val rawMessage = String(buffer, 0, bytesRead)
            val parts = rawMessage.split(DELIMITER, limit = 2)
            val senderName = if (parts.size == 2) parts[0] else "Unknown"
            val content = if (parts.size == 2) {
                parts[1].replace(ESCAPED_DELIMITER, DELIMITER)
            } else rawMessage

            emit(ChatMessage(
                content = content.take(MAX_MESSAGE_LENGTH),
                senderName = senderName.take(50),
                isFromLocalUser = false
            ))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendMessage(bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket.outputStream.write(bytes)
                socket.outputStream.flush()
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    /**
     * Encode message with escaped delimiter for safe transmission.
     */
    fun encodeMessage(senderName: String, content: String): ByteArray {
        val safeContent = content
            .take(MAX_MESSAGE_LENGTH)
            .replace(DELIMITER, ESCAPED_DELIMITER)
        return "$senderName$DELIMITER$safeContent".toByteArray()
    }
}
