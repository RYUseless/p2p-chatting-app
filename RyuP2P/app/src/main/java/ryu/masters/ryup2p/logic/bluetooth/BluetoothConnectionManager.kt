package ryu.masters.ryup2p.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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

    fun startServer(roomId: String) {
        currentRoomId = roomId
        Log.d(BluetoothConstants.TAG_CONNECTION, "Starting server with room ID: $roomId")

        originalDeviceName = bluetoothAdapter?.name

        val prefs = context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
        prefs.edit { putString("original_bt_name", originalDeviceName) }
        Log.d(BluetoothConstants.TAG_CONNECTION, "Saved original name to SharedPreferences: $originalDeviceName")

        val newName = "${BluetoothConstants.APP_IDENTIFIER}_${roomId}"
        bluetoothAdapter?.setName(newName)
        Log.d(BluetoothConstants.TAG_CONNECTION, "Device name changed from '$originalDeviceName' to '$newName'")

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

    fun readMessage(): String? {
        return try {
            val inputStream = socket?.inputStream ?: return null
            val buffer = ByteArray(1024)
            val bytes = inputStream.read(buffer)
            if (bytes > 0) {
                val message = String(buffer, 0, bytes, Charsets.UTF_8).trim()
                Log.d(BluetoothConstants.TAG_CONNECTION, "Received: $message")
                //here decypher function
                message
            } else {
                null
            }
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


    //fun getRoomId(): String? = currentRoomId
}


