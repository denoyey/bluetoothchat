package com.example.bluetoothchat.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.bluetoothchat.data.model.BluetoothDeviceInfo
import com.example.bluetoothchat.data.model.ChatMessage
import com.example.bluetoothchat.data.model.ConnectionInfo
import com.example.bluetoothchat.domain.BluetoothController
import com.example.bluetoothchat.domain.BluetoothEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-connection Bluetooth controller.
 * Manages simultaneous RFCOMM connections with multiple devices.
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy { bluetoothManager?.adapter }

    // --- State flows ---
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _activeConnections = MutableStateFlow<Map<String, ConnectionInfo>>(emptyMap())
    override val activeConnections: StateFlow<Map<String, ConnectionInfo>> = _activeConnections.asStateFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()

    // --- Internal connection tracking ---
    private data class ActiveConnection(
        val socket: BluetoothSocket,
        val transferService: BluetoothDataTransferService,
        val deviceName: String,
        val deviceAddress: String,
        val job: Job
    )

    private val connections = ConcurrentHashMap<String, ActiveConnection>()
    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null

    private val deviceFoundReceiver = BluetoothStateReceiver { device ->
        val info = device.toBluetoothDeviceInfo()
        _scannedDevices.update { list ->
            if (list.any { it.address == info.address }) list else list + info
        }
    }

    init {
        updatePairedDevices()
    }

    // ═══════════════════════════════════════════════════
    // Discovery
    // ═══════════════════════════════════════════════════
    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        try {
            context.registerReceiver(deviceFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } catch (_: Exception) {}
        _scannedDevices.update { emptyList() }
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    // ═══════════════════════════════════════════════════
    // Server — continuously accepts connections
    // ═══════════════════════════════════════════════════
    override fun startServer() {
        if (serverJob?.isActive == true) return
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    BluetoothConstants.SERVICE_NAME,
                    BluetoothConstants.SERVICE_UUID
                )

                while (isActive) {
                    val clientSocket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        break
                    } ?: continue

                    // Handle in separate coroutine, keep accepting
                    handleNewConnection(clientSocket)
                }
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════════════
    // Client — connect to specific device
    // ═══════════════════════════════════════════════════
    override fun connectToDevice(device: BluetoothDeviceInfo) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return

        // Already connected to this device?
        if (connections.containsKey(device.address)) return

        scope.launch(Dispatchers.IO) {
            try {
                val btDevice = bluetoothAdapter?.getRemoteDevice(device.address) ?: return@launch
                val socket = btDevice.createRfcommSocketToServiceRecord(BluetoothConstants.SERVICE_UUID)
                stopDiscovery()
                socket.connect()
                handleNewConnection(socket)
            } catch (e: IOException) {
                _events.emit(BluetoothEvent.Error("Connection failed: ${e.message}"))
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // Connection handler — shared by server & client
    // ═══════════════════════════════════════════════════
    private fun handleNewConnection(socket: BluetoothSocket) {
        val remoteDevice = socket.remoteDevice ?: return
        val address = remoteDevice.address ?: return
        val name = resolveDeviceName(remoteDevice, address) ?: "Unknown Device"

        // Close existing connection to same device if any
        connections[address]?.let { existing ->
            existing.job.cancel()
            try { existing.socket.close() } catch (_: IOException) {}
        }

        val service = BluetoothDataTransferService(socket)

        val job = scope.launch(Dispatchers.IO) {
            _events.emit(BluetoothEvent.Connected(address, name))
            updateConnectionsState()

            try {
                service.listenForIncomingMessages().collect { message ->
                    _events.emit(BluetoothEvent.MessageReceived(address, message))
                }
            } catch (_: Exception) {}

            // Connection lost
            connections.remove(address)
            updateConnectionsState()
            _events.emit(BluetoothEvent.Disconnected(address))
        }

        connections[address] = ActiveConnection(socket, service, name, address, job)
        updateConnectionsState()
    }

    // ═══════════════════════════════════════════════════
    // Messaging
    // ═══════════════════════════════════════════════════
    override suspend fun sendMessage(address: String, message: String): ChatMessage? {
        val conn = connections[address] ?: return null
        val senderName = bluetoothAdapter?.name ?: "Me"
        val chatMessage = ChatMessage(
            content = message.take(1000),
            senderName = senderName,
            isFromLocalUser = true
        )
        val bytes = conn.transferService.encodeMessage(senderName, message)
        val sent = conn.transferService.sendMessage(bytes)
        return if (sent) chatMessage else null
    }

    // ═══════════════════════════════════════════════════
    // Connection management
    // ═══════════════════════════════════════════════════
    override fun closeConnection(address: String) {
        connections.remove(address)?.let { conn ->
            conn.job.cancel()
            try { conn.socket.close() } catch (_: IOException) {}
        }
        updateConnectionsState()
    }

    override fun release() {
        serverJob?.cancel()
        try { serverSocket?.close() } catch (_: IOException) {}
        connections.values.forEach { conn ->
            conn.job.cancel()
            try { conn.socket.close() } catch (_: IOException) {}
        }
        connections.clear()
        updateConnectionsState()
        try { context.unregisterReceiver(deviceFoundReceiver) } catch (_: Exception) {}
        scope.cancel()
    }

    // ═══════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════
    private fun updateConnectionsState() {
        _activeConnections.value = connections.mapValues { (_, conn) ->
            ConnectionInfo(conn.deviceName, conn.deviceAddress)
        }
    }

    private fun resolveDeviceName(device: BluetoothDevice?, address: String): String? {
        device?.name?.let { return it }
        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT) && address.isNotEmpty()) {
            bluetoothAdapter?.bondedDevices?.forEach { bonded ->
                if (bonded.address == address) bonded.name?.let { return it }
            }
        }
        return null
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        bluetoothAdapter?.bondedDevices
            ?.map { it.toBluetoothDeviceInfo() }
            ?.also { devices -> _pairedDevices.update { devices } }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun BluetoothDevice.toBluetoothDeviceInfo(): BluetoothDeviceInfo {
        return BluetoothDeviceInfo(name = this.name, address = this.address)
    }
}
