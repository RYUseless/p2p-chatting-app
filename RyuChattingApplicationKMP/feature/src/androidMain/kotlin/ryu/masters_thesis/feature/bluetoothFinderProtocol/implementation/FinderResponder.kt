package ryu.masters_thesis.feature.bluetoothFinderProtocol.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_MSG_HOSTING
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_MSG_NOT_HOSTING
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_MSG_QUERY
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_UUID
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderResponder
import ryu.masters_thesis.feature.lifecycle.domain.Terminable
import ryu.masters_thesis.feature.lifecycle.implementation.AppTerminationRegistry
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class FinderResponderImpl(
    private val context:   Context,
    // Lambda — vrátí roomId pokud zařízení právě hostuje, jinak null
    private val isHosting: () -> String?,
) : FinderResponder, Terminable {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var serverSocket: BluetoothServerSocket? = null
    private var acceptThread: Thread?                = null
    @Volatile private var running                    = false

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun start() {
        if (running) return
        running = true

        try {
            serverSocket = adapter?.listenUsingRfcommWithServiceRecord(
                "BFP",
                UUID.fromString(BFP_UUID),
            )
        } catch (e: Exception) {
            Log.e("BFP", "FinderResponder server socket failed: ${e.message}")
            running = false
            return
        }

        acceptThread = Thread {
            Log.d("BFP", "FinderResponder running")
            while (running) {
                try {
                    // accept s 3s timeoutem → loop se nezablokuje navždy
                    val socket = serverSocket?.accept(3000) ?: continue
                    handleQuery(socket)
                } catch (e: IOException) {
                    // timeout nebo socket zavřen → pokud running, zkus znovu
                    if (!running) break
                }
            }
            Log.d("BFP", "FinderResponder stopped")
        }.also { it.start() }
    }

    private fun handleQuery(socket: BluetoothSocket) {
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                val writer = socket.outputStream.bufferedWriter(Charsets.UTF_8)

                val line   = reader.readLine() ?: return@Thread
                val parts  = line.split(":", limit = 2)
                if (parts.size != 2 || parts[0] != BFP_MSG_QUERY) return@Thread

                val queriedRoomId = parts[1]
                val hostedRoomId  = isHosting()

                val response = if (hostedRoomId != null && hostedRoomId == queriedRoomId) {
                    "$BFP_MSG_HOSTING:$queriedRoomId"
                } else {
                    "$BFP_MSG_NOT_HOSTING:$queriedRoomId"
                }

                writer.write("$response\n")
                writer.flush()
                Log.d("BFP", "handleQuery roomId=$queriedRoomId hosted=$hostedRoomId → $response")
            } catch (e: Exception) {
                Log.e("BFP", "handleQuery error: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }.start()
    }

    override fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        acceptThread?.interrupt()
        acceptThread = null
    }

    init {
        AppTerminationRegistry.register(this)
    }

    override fun onTerminate() {
        stop()
        AppTerminationRegistry.unregister(this)
    }
}