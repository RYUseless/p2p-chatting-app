package ryu.masters_thesis.ryus_chatting_application.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import androidx.core.content.edit

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onConnected: (BluetoothSocket, String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var serverSocket: BluetoothServerSocket? = null
    var socket: BluetoothSocket? = null
    private var originalDeviceName: String? = null
    private var currentRoomId: String? = null

    // BufferedReader zajišťuje kompletní čtení řádků bez fragmentace RFCOMM streamu
    private var bufferedReader: BufferedReader? = null

    @SuppressLint("MissingPermission")
    fun startServer(advertisedName: String) {
        currentRoomId = advertisedName
        Log.d(BluetoothConstants.TAG_CONNECTION, "Starting server with advertised name: $advertisedName")

        originalDeviceName = bluetoothAdapter?.name
        val prefs = context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
        prefs.edit { putString("original_bt_name", originalDeviceName) }
        Log.d(BluetoothConstants.TAG_CONNECTION, "Saved original BT name: $originalDeviceName")

        // ← OPRAVA: použij advertisedName přímo, BluetoothController již přidal APP_IDENTIFIER prefix
        try {
            bluetoothAdapter?.name = advertisedName
            Log.d(BluetoothConstants.TAG_CONNECTION, "BT name changed: $originalDeviceName → $advertisedName")
        } catch (e: SecurityException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Cannot set BT name: ${e.message}")
        }

        BluetoothCleanupService.originalDeviceName = originalDeviceName
        BluetoothCleanupService.bluetoothAdapter = bluetoothAdapter

        Thread {
            val startTime = System.currentTimeMillis()
            val timeout = 300_000L
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    BluetoothConstants.APP_IDENTIFIER,
                    UUID.fromString(BluetoothConstants.UUID_STRING)
                )
                Log.d(BluetoothConstants.TAG_CONNECTION, "Server socket created, waiting for connection...")
                socket = serverSocket?.accept()

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= timeout) {
                    Log.e(BluetoothConstants.TAG_CONNECTION, "Server timeout after 300s")
                    closeConnection()
                    onError("Timeout: No client connected in 300s")
                    return@Thread
                }

                if (socket != null) {
                    Log.d(BluetoothConstants.TAG_CONNECTION, "Client connected: ${socket!!.remoteDevice.name}")
                    // OPRAVA: Chyběla inicializace bufferu na serveru!
                    bufferedReader = BufferedReader(InputStreamReader(socket!!.inputStream, Charsets.UTF_8))
                    onConnected(socket!!, socket!!.remoteDevice.name ?: "Unknown")
                }
            } catch (e: IOException) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Server error: ${e.message}", e)
                closeConnection()
                onError("Server: ${e.message}")
            }
        }.start()
    }

    suspend fun connectAsClient(device: android.bluetooth.BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONNECTION, "Connecting to client: ${device.name}")
        withContext(Dispatchers.IO) {
            try {
                bluetoothAdapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothConstants.UUID_STRING))
                Log.d(BluetoothConstants.TAG_CONNECTION, "Socket created, connecting...")
                socket?.connect()
                Log.d(BluetoothConstants.TAG_CONNECTION, "Connected! Socket status: ${socket?.isConnected}")

                if (socket != null) {
                    bufferedReader = BufferedReader(InputStreamReader(socket!!.inputStream, Charsets.UTF_8))
                    onConnected(socket!!, device.name ?: "Unknown")
                }
            } catch (e: IOException) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Connect error: ${e.message}", e)
                socket?.close()
                onError("Connect: ${e.message}")
            }
        }
    }

    fun sendMessage(message: String) {
        try {
            val bytes = (message + "\n").toByteArray(Charsets.UTF_8)
            socket?.outputStream?.write(bytes)
            socket?.outputStream?.flush()
            Log.d(BluetoothConstants.TAG_CONNECTION, "Sent: $message")
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Send error: ${e.message}")
        }
    }

    // readLine() blokuje dokud nepřijde celý řádek → žádná fragmentace RFCOMM streamu
    fun readMessage(): String? {
        return try {
            val line = bufferedReader?.readLine()
            if (line != null) {
                Log.d(BluetoothConstants.TAG_CONNECTION, "Received: $line")
            }
            line
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Read error: ${e.message}")
            null
        }
    }

    fun verifyConnection(): Boolean {
        val isConn = socket?.isConnected == true
        Log.d(BluetoothConstants.TAG_CONNECTION, "Verify connection: $isConn")
        return isConn
    }

    fun closeConnection() {
        try {
            bufferedReader?.close()
            bufferedReader = null
            serverSocket?.close()
            socket?.close()

            val prefs = context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
            val savedName = originalDeviceName ?: prefs.getString("original_bt_name", null)

            if (savedName != null) {
                bluetoothAdapter?.setName(savedName)
                Log.d(BluetoothConstants.TAG_CONNECTION, "Device name restored to: $savedName")
                prefs.edit { remove("original_bt_name") }
            }

            Log.d(BluetoothConstants.TAG_CONNECTION, "Connection closed")
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Close error", e)
        }
    }
}
