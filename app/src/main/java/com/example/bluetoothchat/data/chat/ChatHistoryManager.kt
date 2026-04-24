package com.example.bluetoothchat.data.chat

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.bluetoothchat.data.model.ChatHistoryItem
import com.example.bluetoothchat.data.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted chat history persistence.
 * Uses EncryptedSharedPreferences for secure storage at rest.
 */
class ChatHistoryManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "bluetooth_chat_history_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular prefs if encryption fails
        context.getSharedPreferences("bluetooth_chat_history", Context.MODE_PRIVATE)
    }

    fun saveHistory(deviceAddress: String, deviceName: String, messages: List<ChatMessage>) {
        val json = JSONObject().apply {
            put("deviceName", deviceName)
            put("deviceAddress", deviceAddress)
            put("lastTimestamp", System.currentTimeMillis())
            put("lastMessage", messages.lastOrNull()?.content ?: "")
            val arr = JSONArray()
            for (msg in messages.takeLast(100)) {
                arr.put(JSONObject().apply {
                    put("content", msg.content)
                    put("senderName", msg.senderName)
                    put("timestamp", msg.timestamp)
                    put("isFromLocalUser", msg.isFromLocalUser)
                })
            }
            put("messages", arr)
        }
        prefs.edit()
            .putString("device_$deviceAddress", json.toString())
            .apply()

        val addresses = getDeviceAddresses().toMutableSet()
        addresses.add(deviceAddress)
        prefs.edit().putStringSet("known_devices", addresses).apply()
    }

    fun getAllHistory(): List<ChatHistoryItem> {
        return getDeviceAddresses().mapNotNull { getHistoryForDevice(it) }
            .sortedByDescending { it.lastTimestamp }
    }

    fun getHistoryForDevice(deviceAddress: String): ChatHistoryItem? {
        val json = prefs.getString("device_$deviceAddress", null) ?: return null
        return try {
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until arr.length()).map { i ->
                val m = arr.getJSONObject(i)
                ChatMessage(
                    content = m.getString("content"),
                    senderName = m.getString("senderName"),
                    timestamp = m.getLong("timestamp"),
                    isFromLocalUser = m.getBoolean("isFromLocalUser")
                )
            }
            ChatHistoryItem(
                deviceName = obj.getString("deviceName"),
                deviceAddress = obj.getString("deviceAddress"),
                lastMessage = obj.optString("lastMessage", ""),
                lastTimestamp = obj.optLong("lastTimestamp", 0L),
                messages = messages
            )
        } catch (e: Exception) { null }
    }

    fun deleteHistory(deviceAddress: String) {
        prefs.edit().remove("device_$deviceAddress").apply()
        val addresses = getDeviceAddresses().toMutableSet()
        addresses.remove(deviceAddress)
        prefs.edit().putStringSet("known_devices", addresses).apply()
    }

    private fun getDeviceAddresses(): Set<String> {
        return prefs.getStringSet("known_devices", emptySet()) ?: emptySet()
    }
}
