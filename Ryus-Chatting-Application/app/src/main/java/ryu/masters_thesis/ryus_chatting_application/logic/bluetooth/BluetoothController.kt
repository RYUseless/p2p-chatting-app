package ryu.masters_thesis.ryus_chatting_application.logic.bluetooth

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
import ryu.masters_thesis.ryus_chatting_application.logic.CryptUtils.AESCryptoManager
import ryu.masters_thesis.ryus_chatting_application.logic.Messages.BluetoothChatManager
import ryu.masters_thesis.ryus_chatting_application.logic.Messages.Message
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothConstants.TAG_CONTROLLER
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

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private val _isServer = MutableStateFlow(false)
    val isServer: StateFlow<Boolean> = _isServer

    private val chatManagers = mutableMapOf<String, BluetoothChatManager>()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private fun getChatManager(roomId: String): BluetoothChatManager {
        return chatManagers.getOrPut(roomId) { BluetoothChatManager() }
    }

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

    private var cryptoUtils: AESCryptoManager? = null
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
                        val deviceName = intent.getStringExtra(android.bluetooth.BluetoothDevice.EXTRA_NAME)
                        Log.d(BluetoothConstants.TAG_CONTROLLER, "Found device: $deviceName (${it.address})")

                        // Aktualizuj název pokud MAC už známe (spárované zařízení se starým cached názvem)
                        val existing = _scannedDevices.value.find { d -> d.address == it.address }
                        if (existing != null && deviceName != null) {
                            val newRoomId = extractRoomIdFromName(deviceName)
                            _scannedDevices.value = _scannedDevices.value.map { d ->
                                if (d.address == it.address) d.copy(name = deviceName, roomId = newRoomId) else d
                            }
                            Log.d(BluetoothConstants.TAG_CONTROLLER, "Updated cached device name: $deviceName")
                            return
                        }

                        if (deviceName != null && deviceName.startsWith(BluetoothConstants.APP_IDENTIFIER)) {
                            val roomId = extractRoomIdFromName(deviceName)
                            val btDevice = BluetoothDevice(name = deviceName, address = it.address, roomId = roomId)
                            if (!_scannedDevices.value.any { d -> d.address == btDevice.address }) {
                                _scannedDevices.value += btDevice
                                Log.d(BluetoothConstants.TAG_CONTROLLER, "Added device to list: ${btDevice.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractRoomIdFromName(deviceName: String): String? {
        return if (deviceName.startsWith("${BluetoothConstants.APP_IDENTIFIER}_")) {
            deviceName.removePrefix("${BluetoothConstants.APP_IDENTIFIER}_")
        } else null
    }

    fun createRoomFromCreateScreen(password: String): String? {
        _isServer.value = true
        submitServerPassword(password)
        return _currentRoomId.value
    }

    fun initRoomId() { _currentRoomId.value = generateRoomId() }
    fun setRoomId(roomId: String) { _currentRoomId.value = roomId }

    fun submitClientPassword(password: String) {
        if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            return
        }
        val roomId = _currentRoomId.value ?: run {
            Log.e(TAG_CONTROLLER, "submitClientPassword: roomId is null!")
            return
        }
        val keyData = pendingKeyExchangeData ?: run {
            Log.e(TAG_CONTROLLER, "submitClientPassword: pendingKeyExchangeData is null!")
            return
        }

        Log.d(TAG_CONTROLLER, "submitClientPassword: room=$roomId, keyData=${keyData.take(50)}...")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                cryptoUtils = AESCryptoManager(context, roomId)
                val success = cryptoUtils!!.initializeAsClient(keyData, password.trim())

                withContext(Dispatchers.Main) {
                    if (success) {
                        _needsPassword.value = false
                        _passwordError.value = null

                        connectionManager?.sendMessage("HANDSHAKE:CLIENT_READY")
                        Log.d(TAG_CONTROLLER, "Client: sent HANDSHAKE:CLIENT_READY")
                    } else {
                        Log.e(TAG_CONTROLLER, "Password verification FAILED")
                        _passwordError.value = "Incorrect password"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_CONTROLLER, "submitClientPassword exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _passwordError.value = "Incorrect password"
                }
            }
        }
    }

    fun startClientMode() {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Starting client mode - searching for rooms")

        // --- KLÍČOVÝ RESET STAVŮ ZDE ---
        _isConnected.value = false
        _isVerified.value = false
        _currentRoomId.value = null
        _needsPassword.value = false
        _passwordError.value = null
        // ------------------------------

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
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(discoverableIntent)
    }

    suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "Attempting to connect to: ${device.name}")

        _isConnected.value = false
        _isVerified.value = false
        _needsPassword.value = false
        _passwordError.value = null

        _currentRoomId.value = device.roomId
        connectionManager?.closeConnection()
        connectionManager = BluetoothConnectionManager(
            context, bluetoothAdapter,
            onConnected = { _, remoteName ->
                Log.d(BluetoothConstants.TAG_CONTROLLER, "Client: socket connected to $remoteName, čekám na KEY_EXCHANGE")
                _connectedDeviceName.value = remoteName
                _messages.value = getChatManager(_currentRoomId.value ?: "unknown").messages.value
                startReadThread()
            },
            onError = { err -> Log.e(BluetoothConstants.TAG_CONTROLLER, "Client error: $err") }
        )

        val androidDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (androidDevice != null) {
            connectionManager!!.connectAsClient(androidDevice)
        } else {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "Cannot connect: device is null")
        }
    }

    fun submitServerPassword(password: String) {
        if (password.isEmpty()) {
            _passwordError.value = "Password cannot be empty";
            return
        }

        val roomId = _currentRoomId.value ?: generateRoomId()
        _currentRoomId.value = roomId
        _needsPassword.value = false
        _passwordError.value = null

        cryptoUtils = AESCryptoManager(context, roomId)
        val keyExchangeData = cryptoUtils!!.initializeAsServer(password.trim())

        connectionManager = BluetoothConnectionManager(
            context, bluetoothAdapter,
            onConnected = { _, remoteName ->
                Log.d(BluetoothConstants.TAG_CONTROLLER, "Server: connected to $remoteName in room $roomId")
                _connectedDeviceName.value = remoteName
                _messages.value = getChatManager(roomId).messages.value
                connectionManager?.sendMessage("KEY_EXCHANGE:$keyExchangeData")
                startReadThread()
            },
            onError = { err -> Log.e(BluetoothConstants.TAG_CONTROLLER, "Server error: $err") }
        )

        connectionManager?.startServer("${BluetoothConstants.APP_IDENTIFIER}_${roomId}")
        makeDiscoverable()
    }

    fun sendMessage(text: String) {
        val roomId = _currentRoomId.value ?: return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val encrypted = cryptoUtils?.encrypt(text)
                if (encrypted != null) {
                    connectionManager?.sendMessage("MSG:$encrypted")
                } else {
                    connectionManager?.sendMessage(text)
                }
                coroutineScope.launch(Dispatchers.Main) {
                    getChatManager(roomId).addMessage("You", text)
                    _messages.value = getChatManager(roomId).messages.value
                }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_CONTROLLER, "sendMessage error: ${e.message}")
            }
        }
    }

    private fun startReadThread() {
        readThread?.interrupt()
        readThread = null

        readThread = Thread {
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Read thread started")
            while (connectionManager?.socket?.isConnected == true) {
                try {
                    val message = connectionManager?.readMessage()
                    if (message != null) {
                        Log.d(BluetoothConstants.TAG_CONTROLLER, "Received: ${message.take(50)}")
                        when {
                            message.startsWith("KEY_EXCHANGE:") -> {
                                pendingKeyExchangeData = message.removePrefix("KEY_EXCHANGE:")
                                coroutineScope.launch(Dispatchers.Main) {
                                    _needsPassword.value = true
                                }
                            }
                            message.startsWith("HANDSHAKE:") -> {
                                val handshakeMsg = message.removePrefix("HANDSHAKE:")
                                if (handshakeMsg == "CLIENT_READY" && _isServer.value) {
                                    Log.d(BluetoothConstants.TAG_CONTROLLER, "Server: klient autentizován, odesílám HANDSHAKE:CONFIRMED")
                                    connectionManager?.sendMessage("HANDSHAKE:CONFIRMED")
                                    coroutineScope.launch(Dispatchers.Main) {
                                        _isConnected.value = true
                                        _isVerified.value = true
                                    }
                                } else if (handshakeMsg == "CONFIRMED" && !_isServer.value) {
                                    Log.d(BluetoothConstants.TAG_CONTROLLER, "Client: handshake potvrzen, připojuji do chatu")
                                    coroutineScope.launch(Dispatchers.Main) {
                                        _isConnected.value = true
                                        _isVerified.value = true
                                    }
                                }
                            }
                            message.startsWith("MSG:") -> {
                                val encryptedMsg = message.removePrefix("MSG:")
                                val decrypted = cryptoUtils?.decrypt(encryptedMsg) ?: encryptedMsg
                                val roomId = _currentRoomId.value ?: "unknown"
                                coroutineScope.launch(Dispatchers.Main) {
                                    getChatManager(roomId).addMessage("Remote", decrypted)
                                    _messages.value = getChatManager(roomId).messages.value
                                }
                            }
                            else -> {
                                val roomId = _currentRoomId.value ?: "unknown"
                                coroutineScope.launch(Dispatchers.Main) {
                                    getChatManager(roomId).addMessage("Remote", message)
                                    _messages.value = getChatManager(roomId).messages.value
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_CONTROLLER, "Read thread error: ${e.message}")
                    break
                }
            }
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Read thread ended")

            coroutineScope.launch(Dispatchers.Main) {
                _isConnected.value = false
                _isVerified.value = false
            }
        }
        readThread?.start()
    }

    fun verifyConnection(): Boolean = connectionManager?.verifyConnection() == true

    private fun updatePairedDevices() {
        val bondedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
        _pairedDevices.value = bondedDevices.map { BluetoothDevice(name = it.name, address = it.address) }

        val ryuPaired = bondedDevices
            .filter { it.name?.startsWith(BluetoothConstants.APP_IDENTIFIER) == true }
            .map { BluetoothDevice(
                name = it.name,
                address = it.address,
                roomId = extractRoomIdFromName(it.name ?: "")
            )}
        if (ryuPaired.isNotEmpty()) {
            _scannedDevices.value = ryuPaired.toList()
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Pre-added ${ryuPaired.size} bonded RyuP2P device(s)")
        }
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

    private fun generateRoomId(): String = UUID.randomUUID().toString().substring(0, 8)
}


