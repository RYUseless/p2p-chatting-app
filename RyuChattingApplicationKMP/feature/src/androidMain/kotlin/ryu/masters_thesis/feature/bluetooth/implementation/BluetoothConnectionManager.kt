package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
//import androidx.core.content.edit //issue, issme?
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

//internal → gone
class BluetoothConnectionManager(
    private val context:        Context,
    private val adapter:        BluetoothAdapter?,
    private val onConnected:    (BluetoothSocket, String) -> Unit,
    private val onError:        (String) -> Unit,
    private val onDisconnected: () -> Unit,
) {
    var socket:               BluetoothSocket?       = null
    private var serverSocket: BluetoothServerSocket? = null
    private var bufferedReader: BufferedReader?       = null
    private var originalDeviceName: String?          = null

    /*
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServer(advertisedName: String) {
        Log.d(BluetoothConstants.TAG_CONNECTION, "startServer: advertisedName=$advertisedName")

        originalDeviceName = adapter?.name?.let { name ->
            if (name.startsWith(BluetoothConstants.APP_IDENTIFIER)) {
                context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
                    .getString("original_bt_name", null)
            } else {
                name
            }
        }
        Log.d(BluetoothConstants.TAG_CONNECTION, "Saving original BT name: $originalDeviceName")
        context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
            .edit { putString("original_bt_name", originalDeviceName) }

        BluetoothCleanupService.originalDeviceName = originalDeviceName
        BluetoothCleanupService.bluetoothAdapter   = adapter

        val serviceIntent = Intent(context, BluetoothCleanupService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        try {
            serverSocket = adapter?.listenUsingRfcommWithServiceRecord(
                BluetoothConstants.APP_IDENTIFIER,
                UUID.fromString(BluetoothConstants.UUID_STRING),
            )
            Log.d(BluetoothConstants.TAG_CONNECTION, "Server socket ready, waiting for client...")
            try {
                adapter?.name = advertisedName
                Log.d(BluetoothConstants.TAG_CONNECTION, "BT name set to: $advertisedName")
            } catch (e: SecurityException) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Cannot set BT name: ${e.message}")
            }
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Server socket error: ${e.message}", e)
            onError("Server socket: ${e.message}")
            return
        } catch (e: SecurityException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "Server security error: ${e.message}", e)
            onError("Security: ${e.message}")
            return
        }

        Thread {
            try {
                socket = serverSocket?.accept()
                if (socket != null) {
                    serverSocket?.close()
                    bufferedReader = BufferedReader(InputStreamReader(socket!!.inputStream, Charsets.UTF_8))
                    Log.i(BluetoothConstants.TAG_CONNECTION, "Client connected: ${socket!!.remoteDevice.address}")
                    onConnected(socket!!, socket!!.remoteDevice.address)
                } else {
                    Log.w(BluetoothConstants.TAG_CONNECTION, "accept() returned null socket")
                }
            } catch (e: IOException) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Server accept error: ${e.message}", e)
                closeConnection()
                onError("Server: ${e.message}")
            }
        }.start()
    }
     */

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun connectAsClient(device: android.bluetooth.BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONNECTION, "connectAsClient: ${device.address}")
        withContext(Dispatchers.IO) {
            var connected = tryConnect(device, useFallback = false)
            if (!connected) {
                Log.w(BluetoothConstants.TAG_CONNECTION, "Standard connect failed, trying RFCOMM fallback")
                connected = tryConnect(device, useFallback = true)
            }
            if (!connected) {
                Log.e(BluetoothConstants.TAG_CONNECTION, "Both connect attempts failed for ${device.address}")
                onError("Cannot connect to ${device.address} after 2 attempts")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun tryConnect(device: android.bluetooth.BluetoothDevice, useFallback: Boolean): Boolean {
        Log.d(BluetoothConstants.TAG_CONNECTION, "tryConnect: address=${device.address} useFallback=$useFallback")
        return try {
            adapter?.cancelDiscovery()
            socket = if (useFallback) {
                @Suppress("DiscouragedPrivateApi")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                (method.invoke(device, 1) as BluetoothSocket).also {
                    Log.d(BluetoothConstants.TAG_CONNECTION, "Fallback RFCOMM socket created")
                }
            } else {
                device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothConstants.UUID_STRING)).also {
                    Log.d(BluetoothConstants.TAG_CONNECTION, "Standard RFCOMM socket created")
                }
            }

            if (device.bondState == android.bluetooth.BluetoothDevice.BOND_NONE) {
                Log.d(BluetoothConstants.TAG_CONNECTION, "Device not bonded, initiating createBond()")
                device.createBond()
                Thread.sleep(4000)
                Log.d(BluetoothConstants.TAG_CONNECTION, "Bond state after wait: ${device.bondState}")
            }

            socket?.connect()
            if (socket?.isConnected == true) {
                bufferedReader = BufferedReader(InputStreamReader(socket!!.inputStream, Charsets.UTF_8))
                Log.i(BluetoothConstants.TAG_CONNECTION, "Connected to ${device.address} useFallback=$useFallback")
                onConnected(socket!!, device.address)
                true
            } else {
                Log.w(BluetoothConstants.TAG_CONNECTION, "socket.isConnected=false after connect()")
                false
            }
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "tryConnect(fallback=$useFallback) failed: ${e.message}")
            socket?.close()
            socket = null
            false
        } catch (e: SecurityException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "tryConnect security error: ${e.message}")
            socket?.close()
            socket = null
            false
        }
    }
    fun sendMessage(message: String) {
        try {
            socket?.outputStream?.write((message + "\n").toByteArray(Charsets.UTF_8))
            socket?.outputStream?.flush()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "sendMessage error: ${e.message}")
        }
    }

    fun readMessage(): String? {
        return try {
            bufferedReader?.readLine()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "readMessage error: ${e.message}")
            null
        }
    }

    fun verifyConnection(): Boolean = socket?.isConnected == true

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun closeConnection() {
        Log.d(BluetoothConstants.TAG_CONNECTION, "closeConnection: socket=${socket != null} serverSocket=${serverSocket != null}")
        try {
            // zmena poradi kvuli chybam pri odpojeni, kdyz byl bufferreader pred, bylo to bloknute
            socket?.close()
            bufferedReader?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_CONNECTION, "closeConnection error: ${e.message}")
        } finally {
            bufferedReader  = null
            serverSocket    = null
            socket          = null
            restoreBluetoothName()
            onDisconnected()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun restoreBluetoothName() {
        val prefs     = context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
        val savedName = originalDeviceName ?: prefs.getString("original_bt_name", null)
        Log.d(BluetoothConstants.TAG_CONNECTION, "restoreBluetoothName: savedName=$savedName")
        if (savedName != null) {
            try {
                adapter?.name = savedName
                Log.i(BluetoothConstants.TAG_CONNECTION, "BT name restored: $savedName")
            } catch (e: SecurityException) {
                Log.w(BluetoothConstants.TAG_CONNECTION, "Cannot restore BT name: ${e.message}")
            }
            prefs.edit().remove("original_bt_name").apply()
        } else {
            Log.w(BluetoothConstants.TAG_CONNECTION, "No saved BT name to restore")
        }
    }
}