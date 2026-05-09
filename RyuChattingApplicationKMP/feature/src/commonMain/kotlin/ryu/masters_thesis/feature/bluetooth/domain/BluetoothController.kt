package ryu.masters_thesis.feature.bluetooth.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ryu.masters_thesis.feature.messages.domain.Message

interface BluetoothController {
    val scannedDevices:      StateFlow<List<BluetoothDevice>>
    val isConnected:         StateFlow<Boolean>
    val isVerified:          StateFlow<Boolean>
    val isSearching:         StateFlow<Boolean>
    val isServer:            StateFlow<Boolean>
    val currentRoomId:       StateFlow<String?>
    val needsPassword:       StateFlow<Boolean>
    val passwordError:       StateFlow<String?>
    val connectedDeviceName: StateFlow<String?>
    val channelMessages:     StateFlow<Map<String, List<Message>>>

    val connectionError: StateFlow<String?>
    fun clearConnectionError()
    fun startClientMode()
    suspend fun connectToDevice(device: BluetoothDevice)
    fun submitClientPassword(channelId: String, password: String)
    fun submitServerPassword(channelId: String, password: String)
    fun sendMessage(channelId: String, text: String)
    fun getMessages(channelId: String): List<Message>
    fun verifyConnection(): Boolean = false

    fun unregisterReceiver()
    fun cleanup()
    fun resetConnection()

    val connectionState: StateFlow<ConnectionState>
    val canReconnect:    StateFlow<Boolean>
    suspend fun reconnect()

    val sessionDevice: StateFlow<BluetoothDevice?>

    //new shit
    val incomingRawMessages: SharedFlow<Triple<String, String, String>>
    // Triple: senderMac, channelId, rawPayload

    val serverHandoffRequired: StateFlow<Boolean>
    fun clearServerHandoff()

    // Připojení peerů — MAC adresy aktivních sessions → SERVER SIDED
    val connectedUserIds: StateFlow<List<String>>

    // Příchozí ROOM_CONFIG pakety → KLIENT SIDED
    val incomingRoomConfig: SharedFlow<RoomConfigPacket>

    // Server → klient broadcast: ROOM_CONFIG:channelId:isSaved=0/1
    fun sendRoomConfig(channelId: String, isSaved: Boolean)

    // Server drží in-memory blacklist per room; vynucuje při HANDSHAKE_CLIENT_READY
    fun setBlacklist(roomId: String, blacklist: Set<String>)

    // Příchozí přezdívky — Pair(senderMac, nickname)
    val incomingNicknames: SharedFlow<Pair<String, String>>

    // Pošle svoji přezdívku peeru — client: unicast, server: broadcast
    fun sendNickname(channelId: String, nickname: String)

    //handoff logic
    val handoffEvent: Flow<HandoffData>
    suspend fun triggerHandoffAndShutdown(successorMac: String)
}