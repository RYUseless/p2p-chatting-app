package ryu.masters_thesis.presentation.chatroom.domain

import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.messages.domain.Message
import kotlinx.coroutines.flow.MutableStateFlow
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState

object BluetoothControllerSingleton {
    var server: BluetoothController = NoopBluetoothController
    var client: BluetoothController = NoopBluetoothController
}

internal object NoopBluetoothController : BluetoothController {
    override val scannedDevices      = MutableStateFlow(emptyList<BluetoothDevice>())
    override val isConnected         = MutableStateFlow(false)
    override val isVerified          = MutableStateFlow(false)
    override val isSearching         = MutableStateFlow(false)
    override val isServer            = MutableStateFlow(false)
    override val currentRoomId       = MutableStateFlow<String?>(null)
    override val needsPassword       = MutableStateFlow(false)
    override val passwordError       = MutableStateFlow<String?>(null)
    override val connectedDeviceName = MutableStateFlow<String?>(null)
    override val channelMessages     = MutableStateFlow(emptyMap<String, List<Message>>())

    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.IDLE)
    override val canReconnect    = MutableStateFlow(false)
    override suspend fun reconnect() = Unit

    override fun startClientMode()                                         = Unit
    override suspend fun connectToDevice(device: BluetoothDevice)          = Unit
    override fun submitClientPassword(channelId: String, password: String) = Unit
    override fun submitServerPassword(channelId: String, password: String) = Unit
    override fun sendMessage(channelId: String, text: String)              = Unit
    override fun getMessages(channelId: String): List<Message>             = emptyList()
    override fun verifyConnection(): Boolean                               = false
    override fun resetConnection()                                         = Unit
    override fun unregisterReceiver()                                      = Unit
    override fun cleanup()                                                 = Unit
    override val connectionError = MutableStateFlow<String?>(null)
    override fun clearConnectionError() = Unit
    override val sessionDevice = MutableStateFlow<BluetoothDevice?>(null)
}