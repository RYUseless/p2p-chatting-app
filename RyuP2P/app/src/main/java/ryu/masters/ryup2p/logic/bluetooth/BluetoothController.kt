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
import ryu.masters.ryup2p.logic.cryptUtils.MessageCryptoUtils
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothController(private val context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _isServer = MutableStateFlow(false)
    val isServer: StateFlow<Boolean> = _isServer

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId

    private val _needsPassword = MutableStateFlow(false)
    val needsPassword: StateFlow<Boolean> = _needsPassword

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    private var connectionManager: BluetoothConnectionManager? = null
    private var receiverRegistered = false
    private var readThread: Thread? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var discoveryTimeoutJob: Job? = null

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private var cryptoUtils: MessageCryptoUtils? = null
    private var pendingKeyExchangeData: String? = null

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
                        Log.d(BluetoothConstants.TAG_CONTROLLER, "Found device: ${it.name} (${it.address})")
                        val deviceName = it.name
                        if (deviceName != null && deviceName.startsWith(BluetoothConstants.APP_IDENTIFIER)) {
                            val roomId = extractRoomIdFromName(deviceName)
                            Log.d(BluetoothConstants.TAG_CONTROLLER, "Found RyuP2P device with room: $roomId")
                            val btDevice = BluetoothDevice(
                                name = deviceName,
                                address = it.address,
                                roomId = roomId
                            )

                            if (!_scannedDevices.value.any { d -> d.address == btDevice.address }) {
                                _scannedDevices.value = _scannedDevices.value + btDevice
                                Log.d(BluetoothConstants.TAG_CONTROLLER, "Added device to list: ${btDevice.name}")
                            }
                        } else {
                            Log.d(BluetoothConstants.TAG_CONTROLLER, "Skipping device (not RyuP2P): ${deviceName}")
                        }
                    }
                }
            }
        }
    }

    private fun extractRoomIdFromName(deviceName: String): String? {
        return if (deviceName.startsWith("${BluetoothConstants.APP_IDENTIFIER}_")) {
            deviceName.removePrefix("${BluetoothConstants.APP_IDENTIFIER}_")
        } else {
            null
        }
    }

    fun startServer() {
        _needsPassword.value = true
        _isServer.value = true
    }

    fun submitServerPassword(password: String) {
        if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            return
        }

        val roomId = generateRoomId()
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Creating room with ID: $roomId")
        _currentRoomId.value = roomId
        _needsPassword.value = false
        _passwordError.value = null

        cryptoUtils = MessageCryptoUtils(context, roomId)
        val keyExchangeData = cryptoUtils!!.initializeAsServer(password)

        connectionManager = BluetoothConnectionManager(
            bluetoothAdapter,
            onConnected = { _, remoteName ->
                Log.d(BluetoothConstants.TAG_CONTROLLER, "Server: connected to $remoteName in room $roomId")
                _isConnected.value = true
                _connectedDeviceName.value = remoteName

                connectionManager?.sendMessage("KEY_EXCHANGE:$keyExchangeData")

                startReadThread()
            },
            onError = { err -> Log.e(BluetoothConstants.TAG_CONTROLLER, "Server error: $err") }
        )

        connectionManager?.startServer(roomId)
        makeDiscoverable()
    }

    fun startClientMode() {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Starting client mode - searching for rooms")
        discoveryTimeoutJob?.cancel()
        _isServer.value = false
        _scannedDevices.value = emptyList()
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
        registerReceiver()
        _isSearching.value = true

        discoveryTimeoutJob = coroutineScope.launch {
            delay(30_000)
            bluetoothAdapter?.cancelDiscovery()
            unregisterReceiver()
        }
    }

    fun makeDiscoverable() {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Making device discoverable")
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(discoverableIntent)
    }

    suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Attempting to connect to: ${device.name}")
        _currentRoomId.value = device.roomId

        if (connectionManager == null) {
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Creating new connection manager")
            connectionManager = BluetoothConnectionManager(
                bluetoothAdapter,
                onConnected = { _, remoteName ->
                    Log.d(BluetoothConstants.TAG_CONTROLLER, "Client: connected to $remoteName")
                    _isConnected.value = true
                    _connectedDeviceName.value = remoteName
                    startReadThread()
                },
                onError = { err -> Log.e(BluetoothConstants.TAG_CONTROLLER, "Client error: $err") }
            )
        }

        val androidDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (androidDevice != null && connectionManager != null) {
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Initiating connection to ${androidDevice.name}")
            connectionManager!!.connectAsClient(androidDevice)
        } else {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "Cannot connect: device=$androidDevice, manager=$connectionManager")
        }
    }

    fun submitClientPassword(password: String) {
        if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            return
        }

        val roomId = _currentRoomId.value ?: return
        val keyData = pendingKeyExchangeData ?: return

        cryptoUtils = MessageCryptoUtils(context, roomId)
        val success = cryptoUtils!!.initializeAsClient(keyData, password)

        if (success) {
            _needsPassword.value = false
            _passwordError.value = null
        } else {
            _passwordError.value = "Incorrect password"
        }
    }

    fun sendMessage(text: String) {
        val encrypted = cryptoUtils?.encrypt(text)
        if (encrypted != null) {
            connectionManager?.sendMessage("MSG:$encrypted")
        } else {
            connectionManager?.sendMessage(text)
        }
        _messages.value = _messages.value + ("You: $text")
    }

    private fun startReadThread() {
        if (readThread != null) return

        readThread = Thread {
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Read thread started")
            while (_isConnected.value && connectionManager?.socket?.isConnected == true) {
                try {
                    val message = connectionManager?.readMessage()
                    if (message != null) {
                        when {
                            message.startsWith("KEY_EXCHANGE:") -> {
                                pendingKeyExchangeData = message.removePrefix("KEY_EXCHANGE:")
                                coroutineScope.launch(Dispatchers.Main) {
                                    _needsPassword.value = true
                                }
                            }
                            message.startsWith("MSG:") -> {
                                val encryptedMsg = message.removePrefix("MSG:")
                                val decrypted = cryptoUtils?.decrypt(encryptedMsg) ?: encryptedMsg
                                _messages.value = _messages.value + ("Remote: $decrypted")
                            }
                            else -> {
                                _messages.value = _messages.value + ("Remote: $message")
                            }
                        }
                    }
                    Thread.sleep(100)
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_CONTROLLER, "Read thread error: ${e.message}")
                    break
                }
            }
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Read thread ended")
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
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Receiver registered")
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "Register receiver error", e)
        }
    }

    fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
            _isSearching.value = false
            discoveryTimeoutJob?.cancel()
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Receiver unregistered")
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "Unregister receiver error", e)
        }
    }

    private fun generateRoomId(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }
}





