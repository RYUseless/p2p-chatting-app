package ryu.masters_thesis.feature.bluetooth.implementation

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

@SuppressLint("MissingPermission")
internal class BluetoothConnectionManager(
    private val context:        Context,
    private val adapter:        BluetoothAdapter?,
    private val onConnected:    (BluetoothSocket, String) -> Unit,
    private val onError:        (String) -> Unit,
    private val onDisconnected: () -> Unit,
) {
    var socket:           BluetoothSocket?       = null
    private var serverSocket: BluetoothServerSocket? = null
    private var bufferedReader: BufferedReader?   = null
    private var originalDeviceName: String?      = null

    // ─── SERVER ──────────────────────────────────────────────────────────────

    fun startServer(advertisedName: String) {
        Log.d(BluetoothConstants.TAG_CONNECTION, "startServer: $advertisedName")
        originalDeviceName = adapter?.name
        context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
            .edit { putString("original_bt_name", originalDeviceName) }

        try {
            adapter?.name = advertisedName
        } catch (e: SecurityException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Cannot set BT name: ${e.message}")
        }

        BluetoothCleanupService.originalDeviceName = originalDeviceName
        BluetoothCleanupService.bluetoothAdapter   = adapter

        Thread {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord(
                    BluetoothConstants.APP_IDENTIFIER,
                    UUID.fromString(BluetoothConstants.UUID_STRING),
                )
                Log.d(BluetoothConstants.TAG_CONNECTION, "Server socket ready, waiting…")
                socket = serverSocket?.accept()

                if (socket != null) {
                    serverSocket?.close()
                    bufferedReader = BufferedReader(
                        InputStreamReader(socket!!.inputStream, Charsets.UTF_8)
                    )
                    Log.d(BluetoothConstants.TAG_CONNECTION, "Client connected: ${socket!!.remoteDevice.address}")
                    onConnected(socket!!, socket!!.remoteDevice.address)
                }
            } catch (e: IOException) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Server error: ${e.message}", e)
                closeConnection()
                onError("Server: ${e.message}")
            }
        }.start()
    }

    // ─── CLIENT ──────────────────────────────────────────────────────────────

    suspend fun connectAsClient(device: android.bluetooth.BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONNECTION, "connectAsClient: ${device.address}")
        withContext(Dispatchers.IO) {
            var connected = tryConnect(device, useFallback = false)
            if (!connected) {
                Log.w(BluetoothConstants.TAG_CONNECTION, "Standard connect failed, trying fallback…")
                connected = tryConnect(device, useFallback = true)
            }
            if (!connected) {
                onError("Cannot connect to ${device.address} after 2 attempts")
            }
        }
    }

    private fun tryConnect(
        device:      android.bluetooth.BluetoothDevice,
        useFallback: Boolean,
    ): Boolean {
        return try {
            adapter?.cancelDiscovery()
            socket = if (useFallback) {
                @Suppress("DiscouragedPrivateApi")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                method.invoke(device, 1) as BluetoothSocket
            } else {
                device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothConstants.UUID_STRING))
            }
            socket?.connect()

            if (socket?.isConnected == true) {
                bufferedReader = BufferedReader(
                    InputStreamReader(socket!!.inputStream, Charsets.UTF_8)
                )
                onConnected(socket!!, device.address)
                true
            } else {
                false
            }
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "tryConnect(fallback=$useFallback) failed: ${e.message}")
            socket?.close()
            socket = null
            false
        }
    }

    // ─── I/O ─────────────────────────────────────────────────────────────────

    fun sendMessage(message: String) {
        try {
            socket?.outputStream?.write((message + "\n").toByteArray(Charsets.UTF_8))
            socket?.outputStream?.flush()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Send error: ${e.message}")
        }
    }

    fun readMessage(): String? {
        return try {
            bufferedReader?.readLine()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Read error: ${e.message}")
            null
        }
    }

    fun verifyConnection(): Boolean = socket?.isConnected == true

    // ─── CLEANUP ─────────────────────────────────────────────────────────────

    fun closeConnection() {
        try {
            bufferedReader?.close()
            serverSocket?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Close error: ${e.message}")
        } finally {
            bufferedReader = null
            serverSocket   = null
            socket         = null
            restoreBluetoothName()
            onDisconnected()
        }
    }

    private fun restoreBluetoothName() {
        val prefs     = context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
        val savedName = originalDeviceName ?: prefs.getString("original_bt_name", null)
        if (savedName != null) {
            try {
                adapter?.name = savedName
                Log.d(BluetoothConstants.TAG_CONNECTION, "BT name restored: $savedName")
            } catch (e: SecurityException) {
                Log.w(BluetoothConstants.TAG_CONNECTION, "Cannot restore BT name: ${e.message}")
            }
            prefs.edit { remove("original_bt_name") }
        }
    }
}