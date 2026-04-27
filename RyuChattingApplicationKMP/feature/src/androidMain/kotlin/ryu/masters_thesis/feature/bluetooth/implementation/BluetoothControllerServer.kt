package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState

class BluetoothControllerServer(
    context: Context,
    cryptoFactory: (channelId: String) -> CryptoManager,
) : BluetoothControllerBase(context, cryptoFactory) {

    private var serverManager: BluetoothServerManager? = null

    // ── sendMessage = broadcast ───────────────────────────────────────────────

    override fun sendMessage(channelId: String, text: String) {
        val crypto = cryptoManagers[channelId]
        scope.launch(Dispatchers.IO) {
            try {
                val payload = crypto?.encrypt(text) ?: text
                val packet  = buildPacket(BluetoothConstants.MSG_DATA, channelId, payload)
                serverManager?.broadcast(packet)
                withContext(Dispatchers.Main) { addMessage(channelId, "You", text) }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_SERVER, "sendMessage error: ${e.message}", e)
            }
        }
    }

    // ── unicast (HANDSHAKE CONFIRMED, KEY_EXCHANGE) ───────────────────────────

    override fun sendMessageTo(mac: String, packet: String) {
        scope.launch(Dispatchers.IO) {
            serverManager?.sendTo(mac, packet)
        }
    }

    // ── submitServerPassword → spustí accept loop ─────────────────────────────

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE])
    override fun submitServerPassword(channelId: String, password: String) {
        Log.d(BluetoothConstants.TAG_SERVER, "submitServerPassword: channelId=$channelId")
        if (password.isBlank()) {
            _passwordError.value = "Password cannot be empty"
            return
        }

        // Zavřít předchozí server pokud existuje
        val old = serverManager
        serverManager = null
        old?.closeAll()

        resetState()
        _currentRoomId.value = channelId
        _isServer.value      = true

        val crypto          = cryptoFactory(channelId)
        val keyExchangeData = crypto.initializeAsServer(password.trim())
        cryptoManagers[channelId] = crypto

        serverManager = BluetoothServerManager(
            adapter              = adapter,
            onClientConnected    = { session -> onClientConnected(session, channelId, keyExchangeData) },
            onClientDisconnected = { mac     -> onClientDisconnected(mac, channelId) },
            onError              = { err     ->
                scope.launch(Dispatchers.Main) {
                    _connectionError.value = err
                    _connectionState.value = ConnectionState.FAILED
                }
            },
        )

        setBluetoothName("${BluetoothConstants.APP_IDENTIFIER}_${channelId}")
        serverManager!!.startAcceptLoop()
        makeDiscoverable()
    }

    // ── per-client callbacks ──────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun onClientConnected(session: ClientSession, channelId: String, keyExchangeData: String) {
        Log.i(BluetoothConstants.TAG_SERVER, "onClientConnected: ${session.mac}")
        scope.launch(Dispatchers.Main) {
            _isConnected.value     = true
            _connectionState.value = ConnectionState.CONNECTED
        }
        // Unicast KEY_EXCHANGE tomuto klientovi
        try {
            session.sendMessage(buildPacket(BluetoothConstants.MSG_KEY_EXCHANGE, channelId, keyExchangeData))
            Log.d(BluetoothConstants.TAG_SERVER, "KEY_EXCHANGE sent to ${session.mac}")
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_SERVER, "KEY_EXCHANGE send failed: ${e.message}")
        }

        // Broadcast aktualizovaného ROOM_MEMBERS všem
        broadcastRoomMembers(channelId)

        // Spustit read thread pro tohoto klienta
        startClientReadThread(session)
    }

    private fun onClientDisconnected(mac: String, channelId: String) {
        Log.i(BluetoothConstants.TAG_SERVER, "onClientDisconnected: $mac remaining=${serverManager?.sessionCount}")
        scope.launch(Dispatchers.Main) {
            if (serverManager?.sessionCount == 0) {
                _isConnected.value     = false
                _connectionState.value = ConnectionState.IDLE
            }
        }
        broadcastRoomMembers(channelId)
    }

    // ── read thread per client ────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startClientReadThread(session: ClientSession) {
        Thread {
            Log.d(BluetoothConstants.TAG_SERVER, "Read thread start: ${session.mac}")
            while (session.socket.isConnected) {
                try {
                    val raw = session.readLine() ?: break
                    handleIncoming(raw, senderMac = session.mac)
                } catch (e: Exception) {
                    Log.e(BluetoothConstants.TAG_SERVER, "Read error ${session.mac}: ${e.message}")
                    break
                }
            }
            Log.d(BluetoothConstants.TAG_SERVER, "Read thread end: ${session.mac}")
            serverManager?.removeSession(session.mac)
        }.start()
    }

    // ── ROOM_MEMBERS broadcast ────────────────────────────────────────────────

    private fun broadcastRoomMembers(channelId: String) {
        val macs    = serverManager?.connectedMacs ?: return
        val payload = macs.sorted().joinToString(",")
        val packet  = buildPacket(BluetoothConstants.MSG_ROOM_MEMBERS, channelId, payload)
        serverManager?.broadcast(packet)
        Log.d(BluetoothConstants.TAG_SERVER, "ROOM_MEMBERS broadcast: $payload")
    }

    // ── onDisconnectPacket override ───────────────────────────────────────────

    override fun onDisconnectPacket(senderMac: String?) {
        // Klient nás informoval o odpojení → read thread to stejně detekuje,
        // ale pro jistotu session odstraníme okamžitě
        Log.d(BluetoothConstants.TAG_SERVER, "onDisconnectPacket from $senderMac")
        if (senderMac != null) {
            serverManager?.removeSession(senderMac)
        }
    }

    // ── BT name + discoverable ────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun setBluetoothName(name: String) {
        val original = adapter?.name?.let {
            if (it.startsWith(BluetoothConstants.APP_IDENTIFIER)) {
                context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
                    .getString("original_bt_name", null)
            } else it
        }
        if (original != null) {
            context.getSharedPreferences("bluetooth_state", Context.MODE_PRIVATE)
                .edit()
                .putString("original_bt_name", original)
                .apply()
            BluetoothCleanupService.originalDeviceName = original
            BluetoothCleanupService.bluetoothAdapter   = adapter
        }
        val serviceIntent = Intent(context, BluetoothCleanupService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        try {
            adapter?.name = name
            Log.d(BluetoothConstants.TAG_SERVER, "BT name set: $name")
        } catch (e: SecurityException) {
            Log.e(BluetoothConstants.TAG_SERVER, "Cannot set BT name: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun makeDiscoverable() {
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { context.startActivity(it) }
    }

    // ── cleanup ───────────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun cleanup() {
        Log.d(BluetoothConstants.TAG_SERVER, "cleanup")
        val channelId = _currentRoomId.value
        if (channelId != null && _isConnected.value) {
            serverManager?.broadcast(
                buildPacket(BluetoothConstants.MSG_DISCONNECT, channelId, BluetoothConstants.DISCONNECT_SERVER_CLOSED)
            )
        }
        serverManager?.closeAll()
        serverManager = null
        scope.cancel()
    }

    // ── stubs ─────────────────────────────────────────────────────────────────

    override fun startClientMode()                                         = Unit
    override suspend fun connectToDevice(device: BluetoothDevice)          = Unit
    override fun submitClientPassword(channelId: String, password: String) = Unit
    override fun unregisterReceiver()                                      = Unit
    override suspend fun reconnect()                                       = Unit
}