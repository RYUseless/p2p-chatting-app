package ryu.masters.ryup2p.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

@SuppressLint("MissingPermission")
class BluetoothController(
    private val context: Context,
    private val onUpdate: (BluetoothState) -> Unit,
    private val onError: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val scanMs: Long = 33_000L
    private val discoveredDevices: MutableSet<BluetoothDevice> = mutableSetOf()
    private var registered = false

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("BTController", "onReceive called with action: ${intent.action}")
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    Log.d("BTController", "ACTION_FOUND received!")
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        val deviceName = it.name
                        val deviceHardwareAddress = it.address
                        Log.d("BTController", "Device found: $deviceName ($deviceHardwareAddress)")
                        discoveredDevices.add(it)
                        publishState(true)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d("BTController", "ACTION_DISCOVERY_STARTED received")
                    discoveredDevices.clear()
                    publishState(true)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BTController", "ACTION_DISCOVERY_FINISHED received")
                    publishState(false)
                    handler.removeCallbacksAndMessages(null)
                }
            }
        }
    }

    fun buildEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun buildDiscoverableIntent(durationSeconds: Int = 300): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, durationSeconds)
        }

    fun getPairedDevices(): Set<BluetoothDevice> {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        return pairedDevices ?: emptySet()
    }

    fun startDiscovery() {
        val adapter = bluetoothAdapter ?: run {
            onError("Bluetooth adapter not available")
            Log.e("BTController", "Bluetooth adapter is null")
            return
        }

        if (!adapter.isEnabled) {
            onError("Bluetooth is disabled")
            Log.e("BTController", "Bluetooth is disabled")
            return
        }

        Log.d("BTController", "Starting discovery on API ${Build.VERSION.SDK_INT}")

        // MUSÍ být zaregistrovaný PŘED startDiscovery
        registerReceiver()

        if (adapter.isDiscovering) {
            Log.d("BTController", "Already discovering - canceling first")
            adapter.cancelDiscovery()
        }

        discoveredDevices.clear()

        val started = adapter.startDiscovery()
        Log.d("BTController", "startDiscovery() returned: $started")

        if (started) {
            publishState(true)
            handler.postDelayed({
                Log.d("BTController", "33s timeout - stopping discovery")
                stopDiscovery()
            }, scanMs)
        } else {
            onError("Failed to start discovery")
            publishState(false)
        }
    }

    fun stopDiscovery() {
        Log.d("BTController", "Stopping discovery")
        bluetoothAdapter?.cancelDiscovery()
        publishState(false)
        handler.removeCallbacksAndMessages(null)
    }

    private fun registerReceiver() {
        if (registered) {
            Log.d("BTController", "Receiver already registered")
            return
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        try {
            if (Build.VERSION.SDK_INT >= 33) {
                // Pro API 33+ (Android 13+) použij RECEIVER_EXPORTED
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                Log.d("BTController", "Receiver registered as EXPORTED for API ${Build.VERSION.SDK_INT}")
            } else {
                context.registerReceiver(receiver, filter)
                Log.d("BTController", "Receiver registered (no flag) for API ${Build.VERSION.SDK_INT}")
            }
            registered = true
        } catch (e: Exception) {
            Log.e("BTController", "Failed to register receiver: ${e.message}", e)
        }
    }


    fun unregisterReceiver() {
        if (!registered) {
            Log.d("BTController", "Receiver not registered - nothing to unregister")
            return
        }
        try {
            context.unregisterReceiver(receiver)
            Log.d("BTController", "Receiver unregistered successfully")
        } catch (e: Exception) {
            Log.e("BTController", "Failed to unregister receiver: ${e.message}")
        } finally {
            registered = false
        }
    }

    private fun publishState(scanning: Boolean) {
        val paired = getPairedDevices()
        val baseState = BluetoothState.fromContext(context)
        val updatedState = BluetoothState(
            isBluetoothSupported = baseState.isBluetoothSupported,
            isEnabled = baseState.isEnabled,
            localName = baseState.localName,
            bondedDevices = paired,
            connectedDevices = emptyList(),
            discoveredDevices = discoveredDevices.toSet(),
            isScanning = scanning
        )
        Log.d("BTController", "Publishing state - scanning: $scanning, discovered: ${discoveredDevices.size}")
        onUpdate(updatedState)
    }
}
