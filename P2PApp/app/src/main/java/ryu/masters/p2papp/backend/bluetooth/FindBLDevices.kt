package ryu.masters.p2papp.backend.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FindBLDevices(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    var onDeviceDiscovered: ((String, String) -> Unit)? = null
    var onDiscoveryFinished: (() -> Unit)? = null

    private var isDiscovering = false
    private var discoveryReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "FindBLDevices"
    }

    private fun createDiscoveryReceiver(): BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (!hasBluetoothPermissions()) return

                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        try {
                            val deviceName = it.name ?: "Unknown Device"
                            val deviceAddress = it.address
                            Log.d(TAG, "Device found: $deviceName ($deviceAddress)")
                            onDeviceDiscovered?.invoke(deviceName, deviceAddress)
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Security exception: ${e.message}")
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Discovery finished")
                    onDiscoveryFinished?.invoke()
                }
            }
        }
    }

    fun startDiscovery(timeoutMs: Long) {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Missing Bluetooth permissions")
            return
        }

        if (isDiscovering) {
            stopDiscovery()
        }

        try {
            discoveryReceiver = createDiscoveryReceiver()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(discoveryReceiver, filter)
            }

            bluetoothAdapter?.startDiscovery()
            isDiscovering = true
            Log.d(TAG, "Discovery started")

            CoroutineScope(Dispatchers.Main).launch {
                delay(timeoutMs)
                if (isDiscovering) {
                    stopDiscovery()
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting discovery: ${e.message}")
        }
    }

    fun stopDiscovery() {
        try {
            if (hasBluetoothPermissions()) {
                bluetoothAdapter?.cancelDiscovery()
            }

            if (discoveryReceiver != null) {
                context.unregisterReceiver(discoveryReceiver)
                discoveryReceiver = null
            }

            isDiscovering = false
            Log.d(TAG, "Discovery stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery: ${e.message}")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}


