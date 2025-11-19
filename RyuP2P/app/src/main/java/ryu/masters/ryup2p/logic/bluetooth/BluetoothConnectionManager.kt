package ryu.masters.ryup2p.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val onConnected: (BluetoothSocket, String) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        const val UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
        
        const val TAG = "BluetoothConnMgr"
    }

    private var serverSocket: BluetoothServerSocket? = null
    var socket: BluetoothSocket? = null

    fun startServer() {
        Log.d(TAG, "Starting server...")
        Thread {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("RyuP2P", UUID.fromString(UUID_STRING))
                Log.d(TAG, "Server socket created, waiting for connection...")
                socket = serverSocket?.accept()
                if (socket != null) {
                    Log.d(TAG, "Client connected: ${socket!!.remoteDevice.name}")
                    onConnected(socket!!, socket!!.remoteDevice.name ?: "Unknown")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Server error: ${e.message}", e)
                onError("Server: ${e.message}")
            }
        }.start()
    }

    suspend fun connectAsClient(device: android.bluetooth.BluetoothDevice) {
        Log.d(TAG, "Connecting to client: ${device.name}")
        withContext(Dispatchers.IO) {
            try {
                bluetoothAdapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING))
                Log.d(TAG, "Socket created, connecting...")
                socket?.connect()
                Log.d(TAG, "Connected! Socket status: ${socket?.isConnected}")
                if (socket != null) {
                    onConnected(socket!!, device.name ?: "Unknown")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connect error: ${e.message}", e)
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
            Log.d(TAG, "Sent: $message")
        } catch (e: IOException) {
            Log.e(TAG, "Send error: ${e.message}")
        }
    }

    fun readMessage(): String? {
        return try {
            val inputStream = socket?.inputStream ?: return null
            val buffer = ByteArray(1024)
            val bytes = inputStream.read(buffer)
            if (bytes > 0) {
                val message = String(buffer, 0, bytes, Charsets.UTF_8).trim()
                Log.d(TAG, "Received: $message")
                //here decypher function
                message
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Read error: ${e.message}")
            null
        }
    }

    fun verifyConnection(): Boolean {
        val isConn = socket?.isConnected == true
        Log.d(TAG, "Verify connection: $isConn")
        return isConn
    }

    fun closeConnection() {
        try {
            serverSocket?.close()
            socket?.close()
            Log.d(TAG, "Connection closed")
        } catch (e: IOException) {
            Log.e(TAG, "Close error", e)
        }
    }
}