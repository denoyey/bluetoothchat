package com.example.bluetoothchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetoothchat.data.model.BluetoothDeviceInfo
import com.example.bluetoothchat.data.model.ChatHistoryItem
import com.example.bluetoothchat.data.model.ConnectionInfo
import com.example.bluetoothchat.ui.components.AnimatedScanIndicator
import com.example.bluetoothchat.ui.components.DeviceCard
import com.example.bluetoothchat.ui.viewmodel.BluetoothUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: BluetoothUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDeviceInfo) -> Unit,
    onWaitForConnection: () -> Unit,
    onHistoryClick: (ChatHistoryItem) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BluetoothChat",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                actions = {
                    // Active connections badge
                    if (state.activeConnections.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("${state.activeConnections.size}")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            "Toggle theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Devices") },
                    icon = { Icon(Icons.Default.Bluetooth, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("History")
                            if (state.chatHistory.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("${state.chatHistory.size}")
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.History, null, Modifier.size(18.dp)) }
                )
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> DevicesTab(state, onStartScan, onStopScan, onDeviceClick, onWaitForConnection)
                    1 -> HistoryTab(state.chatHistory, state.activeConnections, onHistoryClick, onDeleteHistory)
                }
            }
        }
    }
}

@Composable
private fun DevicesTab(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (BluetoothDeviceInfo) -> Unit,
    onWaitForConnection: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (state.isScanning) onStopScan() else onStartScan() },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isScanning) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (state.isScanning) Icons.Default.Stop else Icons.Default.Search,
                        null, Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.isScanning) "Stop" else "Scan", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onWaitForConnection,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.PhonelinkRing, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Wait", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item { AnimatedScanIndicator(isScanning = state.isScanning, modifier = Modifier.fillMaxWidth()) }

        if (state.isConnecting) {
            item {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Connecting...", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (state.pairedDevices.isNotEmpty()) {
            item { SectionLabel("Paired", state.pairedDevices.size) }
            items(state.pairedDevices) { device ->
                val isOnline = state.activeConnections.containsKey(device.address)
                DeviceCard(device, isPaired = true, isOnline = isOnline, onClick = { onDeviceClick(device) })
            }
        }

        item { SectionLabel("Available", state.scannedDevices.size) }
        if (state.scannedDevices.isEmpty()) {
            item { EmptyDeviceState(state.isScanning) }
        } else {
            items(state.scannedDevices) { device ->
                DeviceCard(device, isPaired = false, isOnline = false, onClick = { onDeviceClick(device) })
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryTab(
    history: List<ChatHistoryItem>,
    activeConnections: Map<String, ConnectionInfo>,
    onHistoryClick: (ChatHistoryItem) -> Unit,
    onDeleteHistory: (String) -> Unit
) {
    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Forum, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("No chat history yet", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(history, key = { it.deviceAddress }) { item ->
                val isOnline = activeConnections.containsKey(item.deviceAddress)
                HistoryCard(item, isOnline, { onHistoryClick(item) }, { onDeleteHistory(item.deviceAddress) })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: ChatHistoryItem, isOnline: Boolean,
    onClick: () -> Unit, onDelete: () -> Unit
) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar with online dot
            Box {
                Surface(
                    Modifier.size(42.dp), shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            item.deviceName.first().uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isOnline) {
                    Surface(
                        modifier = Modifier.size(12.dp).align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Surface(
                            modifier = Modifier.padding(2.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        item.deviceName, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    if (isOnline) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text("Online", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    } else {
                        Text(formatDate(item.lastTimestamp), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.lastMessage.ifEmpty { "No messages" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun EmptyDeviceState(isScanning: Boolean) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isScanning) Icons.AutoMirrored.Filled.BluetoothSearching else Icons.Default.BluetoothDisabled,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (isScanning) "Searching..." else "No devices found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
