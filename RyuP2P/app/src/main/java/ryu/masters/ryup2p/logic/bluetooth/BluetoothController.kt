package ryu.masters.ryup2p.logic.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.*



@SuppressLint("MissingPermission")
class BluetoothController(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    // val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _isServer = MutableStateFlow(false)
    val isServer: StateFlow<Boolean> = _isServer

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    private var connectionManager: BluetoothConnectionManager? = null
    private var receiverRegistered = false
    private var readThread: Thread? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var discoveryTimeoutJob: Job? = null

    // timer pro find as client 30s search
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    companion object {
        const val TAG = "BTController"
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                android.bluetooth.BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE, android.bluetooth.BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d(TAG, "Found device: ${it.name}")
                        val btDevice = BluetoothDevice(name = it.name, address = it.address)
                        if (!_scannedDevices.value.any { d -> d.address == btDevice.address }) {
                            _scannedDevices.value = _scannedDevices.value + btDevice
                        }
                    }
                }
            }
        }
    }

    fun startServer() {
        Log.d(TAG, "Starting server mode")
        _isServer.value = true
        connectionManager = BluetoothConnectionManager(
            bluetoothAdapter,
            onConnected = { _, remoteName ->
                Log.d(TAG, "Server: connected to $remoteName")
                _isConnected.value = true
                _connectedDeviceName.value = remoteName
                startReadThread()
            },
            onError = { err -> Log.e(TAG, "Server error: $err") }
        )
        connectionManager?.startServer()
        makeDiscoverable()
    }

    fun startClientMode() {
        Log.d(TAG, "Starting client mode")
        discoveryTimeoutJob?.cancel()

        _isServer.value = false
        _scannedDevices.value = emptyList()
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
        registerReceiver()

        _isSearching.value = true

        discoveryTimeoutJob = coroutineScope.launch {
            //TODO: migrate origin value to BL_control.conf
            delay(30_000)
            unregisterReceiver()
        }
    }

    fun makeDiscoverable() {
        Log.d(TAG, "Making device discoverable")
        context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
        })
    }

    suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Attempting to connect to: ${device.name}")
        if (connectionManager == null) {
            Log.d(TAG, "Creating new connection manager")
            connectionManager = BluetoothConnectionManager(
                bluetoothAdapter,
                onConnected = { _, remoteName ->
                    Log.d(TAG, "Client: connected to $remoteName")
                    _isConnected.value = true
                    _connectedDeviceName.value = remoteName
                    startReadThread()
                },
                onError = { err -> Log.e(TAG, "Client error: $err") }
            )
        }
        val androidDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (androidDevice != null && connectionManager != null) {
            Log.d(TAG, "Initiating connection to ${androidDevice.name}")
            connectionManager!!.connectAsClient(androidDevice)
        } else {
            Log.e(TAG, "Cannot connect: device=$androidDevice, manager=$connectionManager")
        }
    }

    fun sendMessage(text: String) {
        connectionManager?.sendMessage(text)
        _messages.value = _messages.value + ("You: $text")
    }

    private fun startReadThread() {
        if (readThread != null) return
        readThread = Thread {
            Log.d(TAG, "Read thread started")
            while (_isConnected.value && connectionManager?.socket?.isConnected == true) {
                try {
                    val message = connectionManager?.readMessage()
                    if (message != null) {
                        _messages.value = _messages.value + ("Remote: $message")
                    }
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Log.e(TAG, "Read thread error: ${e.message}")
                    break
                }
            }
            Log.d(TAG, "Read thread ended")
        }
        readThread?.start()
    }

    fun verifyConnection(): Boolean = connectionManager?.verifyConnection() == true

    private fun updatePairedDevices() {
        val paired = bluetoothAdapter?.bondedDevices?.map {
            BluetoothDevice(name = it.name, address = it.address)
        } ?: emptyList()
        _pairedDevices.value = paired
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND)
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Register receiver error", e)
        }
    }

    fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
            _isSearching.value = false
            discoveryTimeoutJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Unregister receiver error", e)
        }
    }
}


