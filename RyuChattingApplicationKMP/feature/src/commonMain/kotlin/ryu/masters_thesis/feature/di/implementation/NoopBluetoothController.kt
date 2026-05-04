package ryu.masters_thesis.feature.bluetooth.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.feature.messages.domain.Message
//hofixing issues?
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// rn pojmenovane takto, protože noop existuje v presentation stále :)
internal class BluetoothControllerNoop : BluetoothController {
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
    override val connectionState     = MutableStateFlow<ConnectionState>(ConnectionState.IDLE)
    override val canReconnect        = MutableStateFlow(false)
    override val connectionError     = MutableStateFlow<String?>(null)
    override val sessionDevice       = MutableStateFlow<BluetoothDevice?>(null)

    override suspend fun reconnect()                                       = Unit
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
    override fun clearConnectionError()                                    = Unit

    //new one:
    override val incomingRawMessages: SharedFlow<Triple<String, String, String>> = MutableSharedFlow()
}