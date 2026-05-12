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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.SchnorrProtocol
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.feature.lifecycle.domain.Terminable
import ryu.masters_thesis.feature.lifecycle.implementation.AppTerminationRegistry

class BluetoothControllerServer(
    context: Context,
    cryptoFactory: (channelId: String) -> CryptoManager,
    schnorr: SchnorrProtocol,
) : BluetoothControllerBase(context, cryptoFactory, schnorr), Terminable {

    init {
        AppTerminationRegistry.register(this)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onTerminate() {
        cleanup()
        AppTerminationRegistry.unregister(this)
    }

    private var serverManager: BluetoothServerManager? = null

    //pokus
    private val blacklistMap = mutableMapOf<String, Set<String>>()
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
        if (password.isBlank()) { _passwordError.value = "Password cannot be empty"; return }

        val old = serverManager
        serverManager = null
        old?.closeAll()
        resetState()
        _currentRoomId.value = channelId
        _isServer.value      = true

        scope.launch(Dispatchers.IO) {
            val crypto          = cryptoFactory(channelId)
            val keyExchangeData = crypto.initializeAsServer(password.trim())
            cryptoManagers[channelId] = crypto
            withContext(Dispatchers.Main) {
                serverManager = BluetoothServerManager(
                    adapter              = adapter,
                    onClientConnected    = { session -> onClientConnected(session, channelId, keyExchangeData) },
                    onClientDisconnected = { mac     -> onClientDisconnected(mac, channelId) },
                    onError              = { err     -> scope.launch(Dispatchers.Main) {
                        _connectionError.value = err
                        _connectionState.value = ConnectionState.FAILED
                    }},
                )
                setBluetoothName("${BluetoothConstants.APP_IDENTIFIER}_${channelId}")
                serverManager!!.startAcceptLoop()
                makeDiscoverable()
            }
        }
    }
    // ── per-client callbacks ──────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun onClientConnected(session: ClientSession, channelId: String, keyExchangeData: String) {
        Log.i(BluetoothConstants.TAG_SERVER, "onClientConnected: ${session.mac}")
        scope.launch(Dispatchers.Main) {
            _isConnected.value     = true
            _connectionState.value = ConnectionState.CONNECTED
            _connectedUserIds.value = serverManager?.connectedMacs?.toList() ?: emptyList()
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
            _connectedUserIds.value = serverManager?.connectedMacs?.toList() ?: emptyList()
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

    override fun onDisconnectPacket(senderMac: String?, payload: String) {
        Log.d(BluetoothConstants.TAG_SERVER, "onDisconnectPacket from $senderMac payload=$payload")
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
        val channelId = _currentRoomId.value
        val mgr       = serverManager

        // 1. Okamžitý reset stavů, aby to frontend hned poznal
        _isServer.value      = false
        _currentRoomId.value = null
        _isConnected.value   = false
        _isVerified.value    = false
        _sessionDevice.value = null
        scope.coroutineContext.cancelChildren()

        // 2. Odeslání paketu a BEZPEČNÉ uzavření soketu
        if (channelId != null && mgr != null) {
            val packet = buildPacket(
                BluetoothConstants.MSG_DISCONNECT,
                channelId,
                BluetoothConstants.DISCONNECT_SERVER_CLOSED,
            )
            Thread {
                try {
                    mgr.broadcast(packet)
                    // Zásadní: Dáme Bluetooth bufferu 150ms na fyzické odeslání bajtů!
                    Thread.sleep(150)
                } catch (_: Exception) {}

                // Až teď, když je zpráva prokazatelně odeslaná, zavřeme soket
                try { mgr.closeAll() } catch (_: Exception) {}
            }.also { it.isDaemon = true; it.start() }
        } else {
            mgr?.closeAll()
        }

        serverManager = null
    }
    // ── blacklist enforcement ─────────────────────────────────────────────────

    override fun setBlacklist(roomId: String, blacklist: Set<String>) {
        Log.d(BluetoothConstants.TAG_SERVER, "setBlacklist: roomId=$roomId size=${blacklist.size}")
        blacklistMap[roomId] = blacklist
    }

    override fun onHandshakeClientReady(senderMac: String?, channelId: String): Boolean {
        val blacklist = blacklistMap[channelId]
        // Blacklist: MAC v listu → zablokovat
        if (!blacklist.isNullOrEmpty() && senderMac in blacklist) {
            Log.w(BluetoothConstants.TAG_SERVER, "HANDSHAKE blocked: $senderMac is blacklisted for $channelId")
            val packet = buildPacket(
                BluetoothConstants.MSG_DISCONNECT,
                channelId,
                BluetoothConstants.DISCONNECT_BLOCKED,
            )
            sendMessageTo(senderMac ?: return false, packet)
            serverManager?.removeSession(senderMac)
            return false
        }
        return true
    }

// ── ROOM_CONFIG broadcast ─────────────────────────────────────────────────

    override fun sendRoomConfig(channelId: String, isSaved: Boolean) {
        val payload = if (isSaved) "isSaved=1" else "isSaved=0"
        val packet  = buildPacket(BluetoothConstants.MSG_ROOM_CONFIG, channelId, payload)
        serverManager?.broadcast(packet)
        Log.d(BluetoothConstants.TAG_SERVER, "ROOM_CONFIG broadcast: $payload")
    }

    // nickname
    override fun sendNickname(channelId: String, nickname: String) {
        if (nickname.isBlank()) return
        val packet = buildPacket(BluetoothConstants.MSG_NICKNAME, channelId, nickname)
        serverManager?.broadcast(packet)
        Log.d(BluetoothConstants.TAG_SERVER, "sendNickname broadcast: nickname=$nickname")
    }

    //handoff
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun triggerHandoffAndShutdown(successorMac: String) {
        val roomId = _currentRoomId.value ?: return
        val packet = buildPacket(BluetoothConstants.MSG_HANDOFF, "SYSTEM", "$successorMac|$roomId")
        serverManager?.broadcast(packet)

        // Čekání na vyprázdnění bufferu před ukončením soketů
        delay(500)
        cleanup()
    }

    // ── stubs ─────────────────────────────────────────────────────────────────

    override fun startClientMode()                                         = Unit
    override suspend fun connectToDevice(device: BluetoothDevice)          = Unit
    override fun submitClientPassword(channelId: String, password: String) = Unit
    override fun unregisterReceiver()                                      = Unit
    override suspend fun reconnect()                                       = Unit
}