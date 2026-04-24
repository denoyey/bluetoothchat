package com.example.bluetoothchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothchat.service.BluetoothChatService
import com.example.bluetoothchat.ui.navigation.Screen
import com.example.bluetoothchat.ui.screens.ChatScreen
import com.example.bluetoothchat.ui.screens.HomeScreen
import com.example.bluetoothchat.ui.screens.SplashScreen
import com.example.bluetoothchat.ui.theme.BluetoothChatTheme
import com.example.bluetoothchat.ui.viewmodel.BluetoothViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter by lazy {
        getSystemService(BluetoothManager::class.java)?.adapter
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* handled */ }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        if (canConnect && bluetoothAdapter?.isEnabled == false) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        // Start foreground service after permissions granted
        if (canConnect) startBluetoothService()
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    // Check if we have permissions before starting
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || 
                        checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        startBluetoothService()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            BluetoothChatTheme(darkTheme = isDarkTheme) {
                val viewModel: BluetoothViewModel = viewModel(
                    factory = BluetoothViewModel.Factory(application)
                )
                val state by viewModel.state.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Error snackbar
                LaunchedEffect(state.errorMessage) {
                    state.errorMessage?.let { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar(error)
                            viewModel.clearError()
                        }
                    }
                }

                // Auto-navigate to chat on new incoming connection
                LaunchedEffect(Unit) {
                    viewModel.navigateToChat.collectLatest { address ->
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != Screen.Chat.route) {
                            navController.navigate(Screen.Chat.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                }

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(snackbarHostState) { data ->
                                Snackbar(data,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    ) { _ ->
                        NavHost(navController, startDestination = Screen.Splash.route) {

                            composable(
                                Screen.Splash.route,
                                exitTransition = { fadeOut(tween(300)) }
                            ) {
                                SplashScreen(
                                    onNavigateToDeviceList = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(
                                Screen.Home.route,
                                enterTransition = { fadeIn(tween(250)) },
                                exitTransition = { fadeOut(tween(250)) }
                            ) {
                                HomeScreen(
                                    state = state,
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                                    onHistoryClick = { item ->
                                        viewModel.connectFromHistory(item)
                                        navController.navigate(Screen.Chat.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    },
                                    onDeleteHistory = { viewModel.deleteHistory(it) },
                                    onFabClick = {
                                        navController.navigate(Screen.DeviceList.route)
                                    }
                                )
                            }

                            composable(
                                Screen.DeviceList.route,
                                enterTransition = { slideInHorizontally(tween(250)) { it } + fadeIn(tween(250)) },
                                popExitTransition = { slideOutHorizontally(tween(250)) { it } + fadeOut(tween(250)) }
                            ) {
                                com.example.bluetoothchat.ui.screens.DeviceListScreen(
                                    state = state,
                                    onStartScan = {
                                        try {
                                            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                            })
                                        } catch (_: Exception) {}
                                        viewModel.startScan()
                                    },
                                    onStopScan = { viewModel.stopScan() },
                                    onDeviceClick = { device ->
                                        viewModel.connectToDevice(device)
                                    },
                                    onWaitForConnection = { /* Server already running */ },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                Screen.Chat.route,
                                enterTransition = { slideInHorizontally(tween(250)) { it } + fadeIn(tween(250)) },
                                popExitTransition = { slideOutHorizontally(tween(250)) { it } + fadeOut(tween(250)) }
                            ) {
                                ChatScreen(
                                    state = state,
                                    isDarkTheme = isDarkTheme,
                                    onSendMessage = { viewModel.sendMessage(it) },
                                    onBack = {
                                        viewModel.closeChat()
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.addAll(listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            ))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun startBluetoothService() {
        val intent = Intent(this, BluetoothChatService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothStateReceiver) } catch (_: Exception) {}
    }
}