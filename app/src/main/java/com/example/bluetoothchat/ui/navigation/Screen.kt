package com.example.bluetoothchat.ui.navigation

/**
 * Defines the navigation destinations for the BluetoothChat app.
 */
sealed class Screen(val route: String) {
    /** Splash screen */
    data object Splash : Screen("splash")

    /** Home screen with chat history */
    data object Home : Screen("home")

    /** Screen for scanning and selecting devices */
    data object DeviceList : Screen("devices")

    /** Chat messaging screen */
    data object Chat : Screen("chat")
}
