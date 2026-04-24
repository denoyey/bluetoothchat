# ═══════════════════════════════════════════════════════
# BluetoothChat ProGuard Rules
# ═══════════════════════════════════════════════════════

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Kotlin ───
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ─── Coroutines ───
-dontwarn kotlinx.coroutines.**

# ─── Compose ───
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ─── Our data models (used in JSON serialization) ───
-keep class com.example.bluetoothchat.data.model.** { *; }
-keep class com.example.bluetoothchat.domain.** { *; }

# ─── Services ───
-keep class com.example.bluetoothchat.service.BluetoothChatService { *; }
-keep class com.example.bluetoothchat.BluetoothChatApp { *; }

# ─── AndroidX Security (EncryptedSharedPreferences) ───
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }

# ─── Bluetooth ───
-keep class android.bluetooth.** { *; }

# ─── Remove logging in release ───
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}