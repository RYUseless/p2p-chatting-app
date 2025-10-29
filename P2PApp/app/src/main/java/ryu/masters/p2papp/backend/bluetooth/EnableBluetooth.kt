package ryu.masters.p2papp.backend.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class EnableBluetooth(private val context: Context) {

    companion object {
        const val REQUEST_ENABLE_BT = 1001
    }

    fun getBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager: BluetoothManager =
            context.getSystemService(BluetoothManager::class.java)
        return bluetoothManager.adapter
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = getBluetoothAdapter()
        return bluetoothAdapter?.isEnabled ?: false
    }

    fun getEnableBluetoothIntent(): Intent? {
        val bluetoothAdapter = getBluetoothAdapter()
        return if (bluetoothAdapter?.isEnabled == false) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        } else {
            null
        }
    }
}


