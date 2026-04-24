package com.example.bluetoothchat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetoothchat.BluetoothChatApp
import com.example.bluetoothchat.data.chat.ChatHistoryManager
import com.example.bluetoothchat.data.model.BluetoothDeviceInfo
import com.example.bluetoothchat.data.model.ChatHistoryItem
import com.example.bluetoothchat.data.model.ChatMessage
import com.example.bluetoothchat.data.model.ConnectionInfo
import com.example.bluetoothchat.domain.BluetoothController
import com.example.bluetoothchat.domain.BluetoothEvent
import com.example.bluetoothchat.service.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val activeConnections: Map<String, ConnectionInfo> = emptyMap(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val currentChatAddress: String? = null,
    val currentMessages: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val chatHistory: List<ChatHistoryItem> = emptyList()
)

class BluetoothViewModel(
    application: Application,
    private val controller: BluetoothController,
    private val historyManager: ChatHistoryManager,
    private val notificationHelper: NotificationHelper
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BluetoothUiState())

    val state: StateFlow<BluetoothUiState> = combine(
        controller.scannedDevices,
        controller.pairedDevices,
        controller.activeConnections,
        _state
    ) { scanned, paired, connections, internal ->
        internal.copy(
            scannedDevices = scanned,
            pairedDevices = paired,
            activeConnections = connections
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())

    /** Emitted when a new incoming connection should auto-navigate to chat */
    private val _navigateToChat = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToChat: SharedFlow<String> = _navigateToChat.asSharedFlow()

    init {
        loadChatHistory()

        // Listen to all Bluetooth events
        viewModelScope.launch {
            controller.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: BluetoothEvent) {
        when (event) {
            is BluetoothEvent.Connected -> {
                _state.update { it.copy(isConnecting = false) }

                // If user is NOT in a chat, auto-navigate to this new connection
                if (_state.value.currentChatAddress == null) {
                    openChat(event.deviceAddress, event.deviceName)
                    _navigateToChat.tryEmit(event.deviceAddress)
                }
            }
            is BluetoothEvent.Disconnected -> {
                // Save chat for this device
                saveChatForDevice(event.deviceAddress)
                loadChatHistory()

                // If currently chatting with this device, clear
                if (_state.value.currentChatAddress == event.deviceAddress) {
                    _state.update {
                        it.copy(
                            currentChatAddress = null,
                            currentMessages = emptyList()
                        )
                    }
                }
            }
            is BluetoothEvent.MessageReceived -> {
                val address = event.deviceAddress
                if (_state.value.currentChatAddress == address) {
                    // Currently chatting — add to screen, no notification
                    _state.update {
                        it.copy(currentMessages = it.currentMessages + event.message)
                    }
                    saveChatForDevice(address)
                } else {
                    // Not chatting with this user — save + show notification
                    appendMessageToHistory(address, event.message)
                    val connName = controller.activeConnections.value[address]?.deviceName
                        ?: "Unknown"
                    notificationHelper.showMessageNotification(
                        connName, address, event.message.content
                    )
                    loadChatHistory()
                }
            }
            is BluetoothEvent.Error -> {
                _state.update { it.copy(errorMessage = event.message, isConnecting = false) }
            }
        }
    }

    fun startScan() {
        controller.startDiscovery()
        _state.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(12000)
            stopScan()
        }
    }

    fun stopScan() {
        controller.stopDiscovery()
        _state.update { it.copy(isScanning = false) }
    }

    fun connectToDevice(device: BluetoothDeviceInfo) {
        _state.update { it.copy(isConnecting = true) }
        controller.connectToDevice(device)
    }

    fun connectFromHistory(item: ChatHistoryItem) {
        val device = BluetoothDeviceInfo(item.deviceName, item.deviceAddress)

        // If already connected, just open chat
        if (controller.activeConnections.value.containsKey(item.deviceAddress)) {
            openChat(item.deviceAddress, item.deviceName)
            _navigateToChat.tryEmit(item.deviceAddress)
            return
        }

        _state.update { it.copy(isConnecting = true) }
        openChat(item.deviceAddress, item.deviceName)
        controller.connectToDevice(device)
    }

    fun openChat(address: String, deviceName: String? = null) {
        // Load messages from history
        val history = historyManager.getHistoryForDevice(address)
        _state.update {
            it.copy(
                currentChatAddress = address,
                currentMessages = history?.messages ?: emptyList()
            )
        }
        // Dismiss notification for this chat
        notificationHelper.dismissNotification(address)
    }

    fun closeChat() {
        val address = _state.value.currentChatAddress
        if (address != null) {
            saveChatForDevice(address)
            loadChatHistory()
        }
        _state.update {
            it.copy(currentChatAddress = null, currentMessages = emptyList())
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val address = _state.value.currentChatAddress ?: return

        viewModelScope.launch {
            val message = controller.sendMessage(address, text)
            if (message != null) {
                _state.update { it.copy(currentMessages = it.currentMessages + message) }
                saveChatForDevice(address)
            } else {
                _state.update { it.copy(errorMessage = "Failed to send") }
            }
        }
    }

    fun disconnectDevice(address: String) {
        saveChatForDevice(address)
        controller.closeConnection(address)
        if (_state.value.currentChatAddress == address) {
            _state.update {
                it.copy(currentChatAddress = null, currentMessages = emptyList())
            }
        }
        loadChatHistory()
    }

    fun deleteHistory(address: String) {
        historyManager.deleteHistory(address)
        loadChatHistory()
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun saveChatForDevice(address: String) {
        val messages = if (_state.value.currentChatAddress == address) {
            _state.value.currentMessages
        } else {
            historyManager.getHistoryForDevice(address)?.messages ?: return
        }
        if (messages.isEmpty()) return

        val name = controller.activeConnections.value[address]?.deviceName
            ?: historyManager.getHistoryForDevice(address)?.deviceName
            ?: "Unknown"

        historyManager.saveHistory(address, name, messages)
    }

    private fun appendMessageToHistory(address: String, message: ChatMessage) {
        val existing = historyManager.getHistoryForDevice(address)
        val messages = (existing?.messages ?: emptyList()) + message
        val name = controller.activeConnections.value[address]?.deviceName
            ?: existing?.deviceName ?: "Unknown"
        historyManager.saveHistory(address, name, messages)
    }

    private fun loadChatHistory() {
        _state.update { it.copy(chatHistory = historyManager.getAllHistory()) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            val app = application as BluetoothChatApp
            return BluetoothViewModel(
                application, app.bluetoothController,
                app.chatHistoryManager, app.notificationHelper
            ) as T
        }
    }
}
