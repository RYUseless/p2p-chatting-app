package ryu.masters_thesis.feature.bluetooth.domain

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
    fun verifyConnection(): Boolean
    fun unregisterReceiver()
    fun cleanup()

    fun resetConnection()

    val connectionState: StateFlow<ConnectionState>
    val canReconnect: StateFlow<Boolean>
    suspend fun reconnect()
}