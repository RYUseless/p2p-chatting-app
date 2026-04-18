package ryu.masters_thesis.feature.bluetooth.implementation

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.messages.domain.Message

@SuppressLint("MissingPermission")
class BluetoothControllerImpl(
    private val context:       Context,
    private val cryptoFactory: (channelId: String) -> CryptoManager,
) : BluetoothController {

    private val btManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = btManager?.adapter

    private val _scannedDevices      = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _isConnected         = MutableStateFlow(false)
    private val _isVerified          = MutableStateFlow(false)
    private val _isSearching         = MutableStateFlow(false)
    private val _isServer            = MutableStateFlow(false)
    private val _currentRoomId       = MutableStateFlow<String?>(null)
    private val _needsPassword       = MutableStateFlow(false)
    private val _passwordError       = MutableStateFlow<String?>(null)
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    private val _channelMessages     = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    override val scannedDevices:      StateFlow<List<BluetoothDevice>>     = _scannedDevices
    override val isConnected:         StateFlow<Boolean>                    = _isConnected
    override val isVerified:          StateFlow<Boolean>                    = _isVerified
    override val isSearching:         StateFlow<Boolean>                    = _isSearching
    override val isServer:            StateFlow<Boolean>                    = _isServer
    override val currentRoomId:       StateFlow<String?>                    = _currentRoomId
    override val needsPassword:       StateFlow<Boolean>                    = _needsPassword
    override val passwordError:       StateFlow<String?>                    = _passwordError
    override val connectedDeviceName: StateFlow<String?>                    = _connectedDeviceName
    override val channelMessages:     StateFlow<Map<String, List<Message>>> = _channelMessages

    private val scope              = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var discoveryJob:      Job? = null
    private var readThread:        Thread? = null
    private var connectionManager: BluetoothConnectionManager? = null
    private var receiverRegistered = false

    private val cryptoManagers = mutableMapOf<String, CryptoManager>()
    private val pendingKeyData = mutableMapOf<String, String>()

    private fun addMessage(channelId: String, sender: String, content: String) {
        val current = _channelMessages.value.toMutableMap()
        val existing = current.getOrDefault(channelId, emptyList())
        current[channelId] = existing + Message(
            sender    = sender,
            content   = content,
            timestamp = System.currentTimeMillis(),
        )
        _channelMessages.value = current
    }

    override fun getMessages(channelId: String): List<Message> =
        _channelMessages.value.getOrDefault(channelId, emptyList())

    private fun buildPacket(type: String, channelId: String, payload: String): String =
        "$type:$channelId:$payload"

    private fun parsePacket(raw: String): Triple<String, String, String>? {
        val parts = raw.split(":", limit = 3)
        if (parts.size != 3) return null
        return Triple(parts[0], parts[1], parts[2])
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != android.bluetooth.BluetoothDevice.ACTION_FOUND) return

            val device: android.bluetooth.BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(
                    android.bluetooth.BluetoothDevice.EXTRA_DEVICE,
                    android.bluetooth.BluetoothDevice::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
            }
            device ?: return

            val deviceName = intent.getStringExtra(android.bluetooth.BluetoothDevice.EXTRA_NAME)
                ?: if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) device.name else return

            val existing = _scannedDevices.value.find { it.address == device.address }
            if (existing != null && deviceName != null) {
                _scannedDevices.value = _scannedDevices.value.map {
                    if (it.address == device.address)
                        it.copy(name = deviceName, roomId = extractRoomId(deviceName))
                    else it
                }
                return
            }

            if (deviceName?.startsWith(BluetoothConstants.APP_IDENTIFIER) == true) {
                val bt = BluetoothDevice(name = deviceName, address = device.address, roomId = null)
                if (_scannedDevices.value.none { it.address == bt.address }) {
                    _scannedDevices.value += bt
                }
            }
        }
    }

    private fun extractRoomId(name: String): String? {
        val prefix = "${BluetoothConstants.APP_IDENTIFIER}_"
        return if (name.startsWith(prefix)) name.removePrefix(prefix) else null
    }

    override fun startClientMode() {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "startClientMode")
        resetState()
        _isServer.value       = false
        _scannedDevices.value = emptyList()
        loadPairedDevices()
        adapter?.startDiscovery()
        registerReceiver()
        _isSearching.value = true

        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            delay(BluetoothConstants.DISCOVERY_TIMEOUT_MS)
            adapter?.cancelDiscovery()
            unregisterReceiver()
        }
    }

    override suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CONTROLLER, "connectToDevice: ${device.address}")
        resetState()
        _currentRoomId.value = device.roomId
        connectionManager?.closeConnection()
        connectionManager = buildConnectionManager()

        val androidDevice = adapter?.getRemoteDevice(device.address)
        if (androidDevice != null) {
            connectionManager!!.connectAsClient(androidDevice)
        } else {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "Device not found: ${device.address}")
        }
    }

    override fun submitClientPassword(channelId: String, password: String) {
        if (password.isBlank()) { _passwordError.value = "Password cannot be empty"; return }
        val keyData = pendingKeyData[channelId] ?: run {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "No pending key data for channel: $channelId")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val crypto = cryptoFactory(channelId)
                val ok = crypto.initializeAsClient(keyData, password.trim())
                withContext(Dispatchers.Main) {
                    if (ok) {
                        cryptoManagers[channelId] = crypto
                        pendingKeyData.remove(channelId)
                        _needsPassword.value = false
                        _passwordError.value = null
                        connectionManager?.sendMessage(
                            buildPacket(
                                BluetoothConstants.MSG_HANDSHAKE,
                                channelId,
                                BluetoothConstants.HANDSHAKE_CLIENT_READY,
                            )
                        )
                    } else {
                        _passwordError.value = "Incorrect password"
                    }
                }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_CONTROLLER, "submitClientPassword error: ${e.message}", e)
                withContext(Dispatchers.Main) { _passwordError.value = "Incorrect password" }
            }
        }
    }

    override fun submitServerPassword(channelId: String, password: String) {
        if (password.isBlank()) { _passwordError.value = "Password cannot be empty"; return }

        _currentRoomId.value = channelId
        _needsPassword.value = false
        _passwordError.value = null
        _isServer.value      = true

        val crypto = cryptoFactory(channelId)
        val keyExchangeData = crypto.initializeAsServer(password.trim())
        cryptoManagers[channelId] = crypto

        connectionManager = buildConnectionManager(
            onConnectedExtra = {
                connectionManager?.sendMessage(
                    buildPacket(
                        BluetoothConstants.MSG_KEY_EXCHANGE,
                        channelId,
                        keyExchangeData,
                    )
                )
            }
        )
        connectionManager?.startServer(BluetoothConstants.APP_IDENTIFIER)
        makeDiscoverable()
    }

    override fun sendMessage(channelId: String, text: String) {
        val crypto = cryptoManagers[channelId]
        scope.launch(Dispatchers.IO) {
            try {
                val packet = crypto?.encrypt(text)?.let { enc ->
                    buildPacket(BluetoothConstants.MSG_DATA, channelId, enc)
                } ?: buildPacket(BluetoothConstants.MSG_DATA, channelId, text)

                connectionManager?.sendMessage(packet)
                withContext(Dispatchers.Main) {
                    addMessage(channelId, "You", text)
                }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_CONTROLLER, "sendMessage error: ${e.message}")
            }
        }
    }

    override fun verifyConnection(): Boolean = connectionManager?.verifyConnection() == true

    override fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
            _isSearching.value = false
            discoveryJob?.cancel()
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "unregisterReceiver error: ${e.message}")
        }
    }

    override fun cleanup() {
        unregisterReceiver()
        connectionManager?.closeConnection()
        scope.cancel()
    }

    private fun buildConnectionManager(
        onConnectedExtra: (() -> Unit)? = null,
    ) = BluetoothConnectionManager(
        context       = context,
        adapter       = adapter,
        onConnected   = { _: BluetoothSocket, address: String ->
            Log.d(BluetoothConstants.TAG_CONTROLLER, "Connected: $address")
            _connectedDeviceName.value = address
            onConnectedExtra?.invoke()
            startReadThread()
        },
        onError        = { err ->
            Log.e(BluetoothConstants.TAG_CONTROLLER, "BT error: $err")
        },
        onDisconnected = {
            scope.launch(Dispatchers.Main) {
                _isConnected.value = false
                _isVerified.value  = false
            }
        },
    )

    private fun startReadThread() {
        readThread?.interrupt()
        readThread = Thread {
            while (connectionManager?.socket?.isConnected == true) {
                try {
                    val raw = connectionManager?.readMessage() ?: break
                    handleIncoming(raw)
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_CONTROLLER, "Read thread error: ${e.message}")
                    break
                }
            }
            scope.launch(Dispatchers.Main) {
                _isConnected.value = false
                _isVerified.value  = false
            }
        }.also { it.start() }
    }

    private fun handleIncoming(raw: String) {
        val (type, channelId, payload) = parsePacket(raw) ?: run {
            Log.w(BluetoothConstants.TAG_CONTROLLER, "Cannot parse: ${raw.take(40)}")
            return
        }

        when (type) {
            BluetoothConstants.MSG_KEY_EXCHANGE -> {
                pendingKeyData[channelId] = payload
                scope.launch(Dispatchers.Main) {
                    _currentRoomId.value = channelId
                    _needsPassword.value = true
                }
            }
            BluetoothConstants.MSG_HANDSHAKE -> {
                scope.launch(Dispatchers.Main) {
                    when {
                        payload == BluetoothConstants.HANDSHAKE_CLIENT_READY && _isServer.value -> {
                            connectionManager?.sendMessage(
                                buildPacket(
                                    BluetoothConstants.MSG_HANDSHAKE,
                                    channelId,
                                    BluetoothConstants.HANDSHAKE_CONFIRMED,
                                )
                            )
                            _isConnected.value = true
                            _isVerified.value  = true
                        }
                        payload == BluetoothConstants.HANDSHAKE_CONFIRMED && !_isServer.value -> {
                            _isConnected.value = true
                            _isVerified.value  = true
                        }
                    }
                }
            }
            BluetoothConstants.MSG_DATA -> {
                val decrypted = cryptoManagers[channelId]?.decrypt(payload) ?: payload
                scope.launch(Dispatchers.Main) {
                    addMessage(channelId, "Remote", decrypted)
                }
            }
            else -> {
                Log.d(BluetoothConstants.TAG_CONTROLLER, "Unknown type: $type — delegating to NeighbourProtocol")
            }
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
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CONTROLLER, "registerReceiver error: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val bonded = adapter?.bondedDevices ?: return
        val ryuDevices = bonded
            .filter { it.name?.startsWith(BluetoothConstants.APP_IDENTIFIER) == true }
            .map { BluetoothDevice(name = it.name, address = it.address, roomId = null) }
        if (ryuDevices.isNotEmpty()) _scannedDevices.value = ryuDevices
    }

    private fun makeDiscoverable() {
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { context.startActivity(it) }
    }

    private fun resetState() {
        _isConnected.value   = false
        _isVerified.value    = false
        _currentRoomId.value = null
        _needsPassword.value = false
        _passwordError.value = null
        cryptoManagers.clear()
        pendingKeyData.clear()
    }
}