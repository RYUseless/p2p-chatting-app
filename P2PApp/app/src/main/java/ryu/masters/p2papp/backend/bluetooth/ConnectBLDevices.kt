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

class ConnectBLDevices(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    var onConnectionSuccess: ((String) -> Unit)? = null
    var onConnectionFailed: ((String) -> Unit)? = null
    var onDataReceived: ((String) -> Unit)? = null

    private var bondReceiver: BroadcastReceiver? = null
    private var targetAddress: String? = null

    companion object {
        private const val TAG = "ConnectBLDevices"
    }

    fun connectToDevice(deviceAddress: String) {
        if (!hasBluetoothConnectPermission()) {
            onConnectionFailed?.invoke("Missing BLUETOOTH_CONNECT permission")
            Log.e(TAG, "Missing permission")
            return
        }

        targetAddress = deviceAddress

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                onConnectionFailed?.invoke("Device not found")
                Log.e(TAG, "Device not found: $deviceAddress")
                return
            }

            Log.d(TAG, "Current bond state: ${device.bondState}")

            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "Device already bonded")
                onConnectionSuccess?.invoke(deviceAddress)
                return
            }

            setupBondListener()

            Log.d(TAG, "Initiating pairing with: $deviceAddress")
            device.createBond()

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            onConnectionFailed?.invoke("SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            onConnectionFailed?.invoke("Error: ${e.message}")
        }
    }

    private fun setupBondListener() {
        bondReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)

                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                device?.let {
                    if (it.address == targetAddress) {
                        Log.d(TAG, "Bond state: $prevState → $state (${it.address})")

                        when (state) {
                            BluetoothDevice.BOND_BONDED -> {
                                Log.d(TAG, "BONDED: ${it.address}")
                                onConnectionSuccess?.invoke(it.address)
                                unregisterBondListener()
                            }
                            BluetoothDevice.BOND_BONDING -> {
                                Log.d(TAG, "BONDING in progress...")
                            }
                            BluetoothDevice.BOND_NONE -> {
                                Log.d(TAG, "BOND_NONE: ${it.address}")
                                if (prevState == BluetoothDevice.BOND_BONDING) {
                                    onConnectionFailed?.invoke("Pairing failed or cancelled")
                                    unregisterBondListener()
                                }
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bondReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(bondReceiver, filter)
            }
            Log.d(TAG, "Bond listener registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering bond listener: ${e.message}")
        }
    }

    private fun unregisterBondListener() {
        try {
            if (bondReceiver != null) {
                context.unregisterReceiver(bondReceiver)
                bondReceiver = null
                Log.d(TAG, "Bond listener unregistered")
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    fun disconnect() {
        unregisterBondListener()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
