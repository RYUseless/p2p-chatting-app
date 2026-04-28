package ryu.masters_thesis.feature.bluetoothNeighbourProtocol.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import androidx.annotation.RequiresPermission
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.LocalDevice
import java.util.UUID

class LocalDeviceImpl(
    private val adapter: BluetoothAdapter?,
    private val context: Context,
) : LocalDevice {

    private val selfId: String by lazy {
        val prefs = context.getSharedPreferences("bnp_prefs", Context.MODE_PRIVATE)
        prefs.getString("self_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("self_id", it).apply()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun getDeviceName(): String? = adapter?.name

    override fun getBluetoothAddress(): String = selfId

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun isDiscoverable(): Boolean =
        adapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
}