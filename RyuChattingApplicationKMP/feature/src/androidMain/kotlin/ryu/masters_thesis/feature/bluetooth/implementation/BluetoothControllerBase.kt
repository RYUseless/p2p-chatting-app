package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.feature.messages.domain.Message
import kotlin.math.log

abstract class BluetoothControllerBase(
    protected val context: Context,
    protected val cryptoFactory: (channelId: String) -> CryptoManager,
) : BluetoothController {

    protected val btManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    protected val adapter: BluetoothAdapter? = btManager?.adapter

    //new values:
    private val _incomingRawMessages = MutableSharedFlow<Triple<String, String, String>>()
    override val incomingRawMessages: SharedFlow<Triple<String, String, String>> = _incomingRawMessages.asSharedFlow()

    protected val _scannedDevices      = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    protected val _isConnected         = MutableStateFlow(false)
    protected val _isVerified          = MutableStateFlow(false)
    protected val _isSearching         = MutableStateFlow(false)
    protected val _isServer            = MutableStateFlow(false)
    protected val _currentRoomId       = MutableStateFlow<String?>(null)
    protected val _needsPassword       = MutableStateFlow(false)
    protected val _passwordError       = MutableStateFlow<String?>(null)
    protected val _connectedDeviceName = MutableStateFlow<String?>(null)
    protected val _channelMessages     = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    protected val _connectionError     = MutableStateFlow<String?>(null)
    protected val _connectionState     = MutableStateFlow(ConnectionState.IDLE)
    protected val _canReconnect        = MutableStateFlow(false)
    protected val _sessionDevice       = MutableStateFlow<BluetoothDevice?>(null)

    override val scannedDevices:      StateFlow<List<BluetoothDevice>>     = _scannedDevices.asStateFlow()
    override val isConnected:         StateFlow<Boolean>                    = _isConnected.asStateFlow()
    override val isVerified:          StateFlow<Boolean>                    = _isVerified.asStateFlow()
    override val isSearching:         StateFlow<Boolean>                    = _isSearching.asStateFlow()
    override val isServer:            StateFlow<Boolean>                    = _isServer.asStateFlow()
    override val currentRoomId:       StateFlow<String?>                    = _currentRoomId.asStateFlow()
    override val needsPassword:       StateFlow<Boolean>                    = _needsPassword.asStateFlow()
    override val passwordError:       StateFlow<String?>                    = _passwordError.asStateFlow()
    override val connectedDeviceName: StateFlow<String?>                    = _connectedDeviceName.asStateFlow()
    override val channelMessages:     StateFlow<Map<String, List<Message>>> = _channelMessages.asStateFlow()
    override val connectionError:     StateFlow<String?>                    = _connectionError.asStateFlow()
    override val connectionState:     StateFlow<ConnectionState>            = _connectionState.asStateFlow()
    override val canReconnect:        StateFlow<Boolean>                    = _canReconnect.asStateFlow()
    override val sessionDevice:       StateFlow<BluetoothDevice?>           = _sessionDevice.asStateFlow()

    protected abstract fun sendMessageTo(mac: String, packet: String)

    protected val scope             = CoroutineScope(Dispatchers.Main + SupervisorJob())
    protected var readThread:        Thread? = null
    protected var connectionManager: BluetoothConnectionManager? = null
    protected val cryptoManagers     = mutableMapOf<String, CryptoManager>()
    protected val pendingKeyData     = mutableMapOf<String, String>()
    protected var wasCleanDisconnect = false

    override fun clearConnectionError() { _connectionError.value = null }

    protected fun extractRoomId(name: String): String? {
        val prefix = "${BluetoothConstants.APP_IDENTIFIER}_"
        return if (name.startsWith(prefix)) name.removePrefix(prefix) else null
    }

    protected fun buildPacket(type: String, channelId: String, payload: String): String =
        "$type:$channelId:$payload"

    protected fun parsePacket(raw: String): Triple<String, String, String>? {
        val parts = raw.split(":", limit = 3)
        return if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null
    }

    protected fun addMessage(channelId: String, sender: String, content: String) {
        val current  = _channelMessages.value.toMutableMap()
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

    override fun sendMessage(channelId: String, text: String) {
        val crypto = cryptoManagers[channelId]
        scope.launch(Dispatchers.IO) {
            try {
                val encrypted = crypto?.encrypt(text)
                if (encrypted == null) Log.w(BluetoothConstants.TAG_BASE, "sendMessage: no crypto for $channelId, plain")
                val packet = encrypted
                    ?.let { buildPacket(BluetoothConstants.MSG_DATA, channelId, it) }
                    ?: buildPacket(BluetoothConstants.MSG_DATA, channelId, text)
                connectionManager?.sendMessage(packet)
                Log.d(BluetoothConstants.TAG_BASE, "sendMessage: channelId=$channelId len=${text.length}")
                withContext(Dispatchers.Main) { addMessage(channelId, "You", text) }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_BASE, "sendMessage error: ${e.message}", e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun handleIncoming(raw: String, senderMac: String? = null) {
        Log.v(BluetoothConstants.TAG_BASE, "handleIncoming raw=${raw.take(80)} sender=$senderMac")
        val (type, channelId, payload) = parsePacket(raw) ?: run {
            Log.w(BluetoothConstants.TAG_BASE, "unparseable packet=${raw.take(60)}")
            return
        }
        Log.d(BluetoothConstants.TAG_BASE, "type=$type channelId=$channelId payload=${payload.take(40)}")
        when (type) {
            BluetoothConstants.MSG_KEY_EXCHANGE -> {
                Log.d(BluetoothConstants.TAG_BASE, "KEY_EXCHANGE for $channelId")
                pendingKeyData[channelId] = payload
                scope.launch(Dispatchers.Main) {
                    _currentRoomId.value = channelId
                    _needsPassword.value = true
                }
            }
            BluetoothConstants.MSG_HANDSHAKE -> {
                Log.d(BluetoothConstants.TAG_BASE, "HANDSHAKE payload=$payload isServer=${_isServer.value}")
                scope.launch(Dispatchers.Main) {
                    when (payload) {
                        BluetoothConstants.HANDSHAKE_CLIENT_READY if _isServer.value -> {
                            Log.d(BluetoothConstants.TAG_BASE, "HANDSHAKE: sending CONFIRMED to $senderMac")
                            val packet = buildPacket(BluetoothConstants.MSG_HANDSHAKE, channelId, BluetoothConstants.HANDSHAKE_CONFIRMED)
                            if (senderMac != null) sendMessageTo(senderMac, packet) else sendMessage(channelId, packet)
                            _isConnected.value     = true
                            _isVerified.value      = true
                            _connectionState.value = ConnectionState.CONNECTED
                        }
                        BluetoothConstants.HANDSHAKE_CONFIRMED if !_isServer.value -> {
                            Log.d(BluetoothConstants.TAG_BASE, "HANDSHAKE: confirmed, chat open")
                            _isConnected.value     = true
                            _isVerified.value      = true
                            _connectionState.value = ConnectionState.CONNECTED
                        }
                        else -> Log.w(BluetoothConstants.TAG_BASE, "HANDSHAKE unexpected: payload=$payload isServer=${_isServer.value}")
                    }
                }
            }
            BluetoothConstants.MSG_DATA -> {
                val decrypted = try {
                    cryptoManagers[channelId]?.decrypt(payload).also {
                        if (it == null) Log.w(BluetoothConstants.TAG_BASE, "MSG_DATA: no crypto for $channelId")
                    } ?: payload
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_BASE, "decrypt error for $channelId: ${e.message}", e)
                    payload
                }
                Log.d(BluetoothConstants.TAG_BASE, "MSG_DATA: channelId=$channelId len=${decrypted.length}")
                scope.launch { _incomingRawMessages.emit(Triple(senderMac ?: "", channelId, decrypted)) }
                scope.launch(Dispatchers.Main) { addMessage(channelId, senderMac ?: "Remote", decrypted) }
            }
            BluetoothConstants.MSG_DISCONNECT -> {
                Log.i(BluetoothConstants.TAG_BASE, "DISCONNECT: channelId=$channelId reason=$payload sender=$senderMac")
                scope.launch(Dispatchers.Main) { onDisconnectPacket(senderMac) }
            }
            else -> Log.w(BluetoothConstants.TAG_BASE, "unknown type=$type")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun startReadThread() {
        readThread?.interrupt()
        Log.d(BluetoothConstants.TAG_BASE, "startReadThread")
        val capturedManager = connectionManager
        readThread = Thread {
            Log.d(BluetoothConstants.TAG_BASE, "Read thread running")
            while (capturedManager?.socket?.isConnected == true) {
                try {
                    val raw = capturedManager.readMessage() ?: run {
                        Log.w(BluetoothConstants.TAG_BASE, "readMessage null, ending thread")
                        break
                    }
                    handleIncoming(raw)
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_BASE, "Read thread exception: ${e.message}", e)
                    break
                }
            }
            Log.d(BluetoothConstants.TAG_BASE, "Read thread ended, clean=$wasCleanDisconnect")
            scope.launch(Dispatchers.Main) {
                if (connectionManager === capturedManager) {
                    _isConnected.value = false
                    _isVerified.value  = false
                    if (wasCleanDisconnect) {
                        resetState()
                    } else {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _canReconnect.value    = _sessionDevice.value != null
                    }
                } else {
                    Log.d(BluetoothConstants.TAG_BASE, "Read thread cleanup skipped — stale manager")
                }
            }
        }.also { it.start() }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    protected fun buildConnectionManager(
        onConnectedExtra: (() -> Unit)? = null,
    ) = BluetoothConnectionManager(
        context     = context,
        adapter     = adapter,
        onConnected = { _, address ->
            Log.d(BluetoothConstants.TAG_BASE, "Socket connected: address=$address")
            _connectedDeviceName.value = address
            _connectionState.value     = ConnectionState.CONNECTING
            onConnectedExtra?.invoke()
            startReadThread()
        },
        onError = { err ->
            Log.e(BluetoothConstants.TAG_BASE, "Connection error: $err")
            scope.launch(Dispatchers.Main) {
                _connectionError.value = err
                _isConnected.value     = false
                _isVerified.value      = false
                _connectionState.value = ConnectionState.FAILED
                _canReconnect.value    = _sessionDevice.value != null
            }
        },
        onDisconnected = {
            Log.i(BluetoothConstants.TAG_BASE, "Socket disconnected")
            scope.launch(Dispatchers.Main) {
                _isConnected.value = false
                _isVerified.value  = false
            }
        },
    )

    // Částečný reset – zachová session credentials
    protected fun resetConnectionState() {
        Log.d(BluetoothConstants.TAG_BASE, "resetConnectionState")
        _isConnected.value     = false
        _isVerified.value      = false
        _needsPassword.value   = false
        _passwordError.value   = null
        _connectionError.value = null
        cryptoManagers.clear()
        pendingKeyData.clear()
        wasCleanDisconnect     = false
    }

    // Plný reset – volat jen při startClientMode()
    protected fun resetState() {
        Log.d(BluetoothConstants.TAG_BASE, "resetState (full)")
        resetConnectionState()
        _currentRoomId.value   = null
        _sessionDevice.value   = null
        _canReconnect.value    = false
        _connectionState.value = ConnectionState.IDLE
    }

    //redo once again ffs
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun resetConnection() {
        Log.d(BluetoothConstants.TAG_BASE, "resetConnection")
        wasCleanDisconnect = true      // ← read thread cleanup půjde přes resetState(), ne DISCONNECTED
        val manager = connectionManager
        connectionManager = null
        readThread = null
        scope.launch(Dispatchers.IO) {
            manager?.closeConnection() // ← async, neblokuje
        }
        scope.launch(Dispatchers.Main) {
            resetConnectionState()
        }
    }
    protected open fun onDisconnectPacket(senderMac: String?) {
        // Base default — pro klienta
        _isConnected.value     = false
        _isVerified.value      = false
        _connectionState.value = ConnectionState.IDLE
    }

    override fun verifyConnection(): Boolean =
        connectionManager?.verifyConnection() == true
}