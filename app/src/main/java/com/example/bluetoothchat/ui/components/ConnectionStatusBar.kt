package com.example.bluetoothchat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.example.bluetoothchat.ui.theme.StatusConnecting
import com.example.bluetoothchat.ui.theme.StatusOffline
import com.example.bluetoothchat.ui.theme.StatusOnline

/**
 * Connection status indicator.
 */
enum class ConnectionStatus {
    CONNECTED, CONNECTING, DISCONNECTED
}

/**
 * An animated connection status bar showing current Bluetooth connection state
 * with pulsing indicators and smooth color transitions.
 */
@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    deviceName: String?,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> StatusOnline
            ConnectionStatus.CONNECTING -> StatusConnecting
            ConnectionStatus.DISCONNECTED -> StatusOffline
        },
        animationSpec = tween(500),
        label = "status_color"
    )

    val statusText = when (status) {
        ConnectionStatus.CONNECTED -> "Connected"
        ConnectionStatus.CONNECTING -> "Connecting..."
        ConnectionStatus.DISCONNECTED -> "Disconnected"
    }

    val statusIcon = when (status) {
        ConnectionStatus.CONNECTED -> Icons.Default.BluetoothConnected
        ConnectionStatus.CONNECTING -> Icons.AutoMirrored.Filled.BluetoothSearching
        ConnectionStatus.DISCONNECTED -> Icons.Default.BluetoothDisabled
    }

    // Pulse animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == ConnectionStatus.CONNECTING) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Animated dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(statusColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = statusIcon,
            contentDescription = statusText,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor
        )
    }
}
