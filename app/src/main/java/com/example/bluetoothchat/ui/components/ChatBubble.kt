package com.example.bluetoothchat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.bluetoothchat.data.model.ChatMessage
import com.example.bluetoothchat.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A stylish chat bubble composable that renders sent and received messages
 * with distinct visual styles, rounded corners, and timestamps.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val isFromMe = message.isFromLocalUser

    val bubbleColor = if (isFromMe) {
        if (isDarkTheme) ChatBubbleSent else ChatBubbleSentLight
    } else {
        if (isDarkTheme) ChatBubbleReceived else ChatBubbleReceivedLight
    }

    val textColor = if (isFromMe) {
        OnChatBubbleSent
    } else {
        if (isDarkTheme) OnChatBubbleReceived else OnChatBubbleReceivedLight
    }

    val timestampColor = if (isFromMe) {
        OnChatBubbleSent.copy(alpha = 0.7f)
    } else {
        if (isDarkTheme) OnChatBubbleReceived.copy(alpha = 0.5f)
        else OnChatBubbleReceivedLight.copy(alpha = 0.5f)
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isFromMe) 16.dp else 4.dp,
        bottomEnd = if (isFromMe) 4.dp else 16.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isFromMe) 60.dp else 12.dp,
                end = if (isFromMe) 12.dp else 60.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Sender name for received messages
        if (!isFromMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        // Bubble
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isFromMe) 4.dp else 2.dp,
                    shape = bubbleShape,
                    spotColor = if (isFromMe) ChatBubbleSent.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
                .clip(bubbleShape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = timestampColor,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * Formats a timestamp (epoch millis) into a human-readable time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
