package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothServerManager(
    private val adapter:          BluetoothAdapter?,
    private val onClientConnected: (ClientSession) -> Unit,
    private val onClientDisconnected: (mac: String) -> Unit,
    private val onError:          (String) -> Unit,
) {
    private var serverSocket: BluetoothServerSocket? = null
    private var acceptThread: Thread? = null
    private val sessions = ConcurrentHashMap<String, ClientSession>()

    val connectedMacs: List<String> get() = sessions.keys.toList()
    val sessionCount:  Int          get() = sessions.size

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startAcceptLoop() {
        Log.d(BluetoothConstants.TAG_SERVER, "startAcceptLoop")
        try {
            serverSocket = adapter?.listenUsingRfcommWithServiceRecord(
                BluetoothConstants.APP_IDENTIFIER,
                UUID.fromString(BluetoothConstants.UUID_STRING),
            )
        } catch (e: IOException) {
            Log.e(BluetoothConstants.TAG_SERVER, "listenUsingRfcomm failed: ${e.message}")
            onError("Server socket: ${e.message}")
            return
        }

        acceptThread = Thread {
            Log.d(BluetoothConstants.TAG_SERVER, "Accept loop running")
            while (true) {
                val socket: BluetoothSocket = try {
                    serverSocket?.accept() ?: break
                } catch (e: IOException) {
                    Log.i(BluetoothConstants.TAG_SERVER, "Accept loop ended: ${e.message}")
                    break
                }
                val mac     = socket.remoteDevice.address
                val session = ClientSession(mac = mac, socket = socket)
                sessions[mac] = session
                Log.i(BluetoothConstants.TAG_SERVER, "Client connected: $mac total=${sessions.size}")
                onClientConnected(session)
            }
            Log.d(BluetoothConstants.TAG_SERVER, "Accept loop exited")
        }.also { it.start() }
    }

    fun broadcast(message: String) {
        sessions.values.forEach { session ->
            try {
                session.sendMessage(message)
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_SERVER, "broadcast failed for ${session.mac}: ${e.message}")
            }
        }
    }

    fun sendTo(mac: String, message: String) {
        val session = sessions[mac] ?: run {
            Log.w(BluetoothConstants.TAG_SERVER, "sendTo: no session for $mac")
            return
        }
        try {
            session.sendMessage(message)
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_SERVER, "sendTo $mac failed: ${e.message}")
        }
    }

    fun removeSession(mac: String) {
        sessions.remove(mac)?.close()
        Log.i(BluetoothConstants.TAG_SERVER, "Session removed: $mac remaining=${sessions.size}")
        onClientDisconnected(mac)
    }

    fun closeAll() {
        Log.d(BluetoothConstants.TAG_SERVER, "closeAll: sessions=${sessions.size}")
        sessions.values.forEach { it.close() }
        sessions.clear()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        acceptThread?.interrupt()
        acceptThread = null
    }
}