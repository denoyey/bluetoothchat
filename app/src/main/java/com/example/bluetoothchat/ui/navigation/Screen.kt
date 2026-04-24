package com.example.bluetoothchat.ui.navigation

/**
 * Defines the navigation destinations for the BluetoothChat app.
 */
sealed class Screen(val route: String) {
    /** Splash screen */
    data object Splash : Screen("splash")

    /** Home screen with tabs (Devices + History) */
    data object Home : Screen("home")

    /** Chat messaging screen */
    data object Chat : Screen("chat")
}
