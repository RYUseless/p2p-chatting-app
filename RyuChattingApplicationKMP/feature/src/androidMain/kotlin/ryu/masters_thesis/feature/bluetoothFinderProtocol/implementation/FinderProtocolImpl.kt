package ryu.masters_thesis.feature.bluetoothFinderProtocol.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_CONNECT_TIMEOUT
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_MSG_HOSTING
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_MSG_QUERY
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_READ_TIMEOUT
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.BFP_UUID
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class FinderProtocolImpl(
    private val context: Context,
) : FinderProtocol {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun findHost(
        roomId:     String,
        knownPeers: List<String>,
    ): String? = withContext(Dispatchers.IO) {
        // 1. Nejdřív se ptáme známých peerů z místnosti
        for (address in knownPeers) {
            val result = queryPeer(address, roomId)
            if (result != null) return@withContext result
        }

        // 2. Pak zkusíme všechna ostatní spárovaná zařízení
        val otherBonded = adapter?.bondedDevices
            ?.map { it.address }
            ?.filter { it !in knownPeers }
            ?: emptyList()

        for (address in otherBonded) {
            val result = queryPeer(address, roomId)
            if (result != null) return@withContext result
        }

        // 3. Nikdo nehostuje → stáváme se serverem
        null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun queryPeer(address: String, roomId: String): String? {
        return try {
            val device = adapter?.getRemoteDevice(address) ?: return null
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BFP_UUID))
            adapter?.cancelDiscovery()

            // Connect s timeoutem přes Thread.join
            var connected = false
            val connectThread = Thread {
                try { socket.connect(); connected = true } catch (_: Exception) {}
            }
            connectThread.start()
            connectThread.join(BFP_CONNECT_TIMEOUT)

            if (!connected) {
                try { socket.close() } catch (_: Exception) {}
                Log.d("BFP", "queryPeer $address → no response (connect timeout)")
                return null
            }

            val writer = socket.outputStream.bufferedWriter(Charsets.UTF_8)
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

            writer.write("$BFP_MSG_QUERY:$roomId\n")
            writer.flush()

            // Read s timeoutem přes Thread.join
            var response: String? = null
            val readThread = Thread {
                response = try { reader.readLine() } catch (_: Exception) { null }
            }
            readThread.start()
            readThread.join(BFP_READ_TIMEOUT)

            try { socket.close() } catch (_: Exception) {}

            Log.d("BFP", "queryPeer $address roomId=$roomId response=$response")
            if (response == "$BFP_MSG_HOSTING:$roomId") address else null
        } catch (e: Exception) {
            Log.d("BFP", "queryPeer $address error: ${e.message}")
            null
        }
    }
}