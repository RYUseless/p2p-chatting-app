package ryu.masters.ryup2p.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

@SuppressLint("MissingPermission")
data class BluetoothState(
    val isBluetoothSupported: Boolean,
    val isEnabled: Boolean,
    val localName: String?,
    val bondedDevices: Set<BluetoothDevice>,
    val connectedDevices: List<BluetoothDevice>,
    val discoveredDevices: Set<BluetoothDevice>,
    val isScanning: Boolean
) {
    companion object {
        fun fromContext(context: Context): BluetoothState {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter? = bm?.adapter

            val supported = adapter != null
            val enabled = adapter?.isEnabled == true

            // Na startu nikdy neshazovat: name/bonded jen pokud to OS dovolí, jinak null/empty
            val name = try { adapter?.name } catch (_: SecurityException) { null }

            val bonded: Set<BluetoothDevice> = try {
                adapter?.bondedDevices ?: emptySet()
            } catch (_: SecurityException) {
                emptySet()
            }

            // Nepokoušet se číst připojené profily při startu (ROM mohou vyhazovat IAE/SEC)
            val connected: List<BluetoothDevice> = emptyList()

            return BluetoothState(
                isBluetoothSupported = supported,
                isEnabled = enabled,
                localName = name,
                bondedDevices = bonded,
                connectedDevices = connected,
                discoveredDevices = emptySet(),
                isScanning = false
            )
        }
    }

    fun copyWithScan(discovered: Set<BluetoothDevice>, scanning: Boolean): BluetoothState =
        copy(discoveredDevices = discovered, isScanning = scanning)
}


