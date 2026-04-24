package com.example.bluetoothchat.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.example.bluetoothchat.BluetoothChatApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Foreground service that keeps Bluetooth connections alive when screen is off.
 * Starts the server, monitors connections, and updates the persistent notification.
 */
class BluetoothChatService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        stopForeground(true)
                    }
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val app = application as BluetoothChatApp
        val notificationHelper = app.notificationHelper

        // Start foreground immediately
        val notification = notificationHelper.buildServiceNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
        }

        // Acquire partial wake lock to keep CPU alive for Bluetooth I/O
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BluetoothChat::ServiceWakeLock"
        ).apply { acquire() }

        // Start the Bluetooth server
        app.bluetoothController.startServer()

        // Monitor active connections to update notification
        scope.launch {
            app.bluetoothController.activeConnections.collectLatest { connections ->
                notificationHelper.updateServiceNotification(connections.size)
            }
        }

        // Listen for Bluetooth OFF to kill service
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothStateReceiver) } catch (_: Exception) {}
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        scope.cancel()
    }
}
