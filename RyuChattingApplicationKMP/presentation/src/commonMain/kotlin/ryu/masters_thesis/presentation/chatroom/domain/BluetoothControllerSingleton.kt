package ryu.masters_thesis.presentation.chatroom.domain

import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.messages.domain.Message
import kotlinx.coroutines.flow.MutableStateFlow

//TODO : FULL DUMMY, PAK YEETNOUT DOPRDELE
// TODO DI: nahradit Koin/jiným DI frameworkem
object BluetoothControllerSingleton {
    var instance: BluetoothController? = null
}

// Fallback — prázdná implementace pro případ že DI ještě není nastavené
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

    override fun startClientMode()                                         = Unit
    override suspend fun connectToDevice(device: BluetoothDevice)          = Unit
    override fun submitClientPassword(channelId: String, password: String) = Unit
    override fun submitServerPassword(channelId: String, password: String) = Unit
    override fun sendMessage(channelId: String, text: String)              = Unit
    override fun getMessages(channelId: String): List<Message>             = emptyList()
    override fun verifyConnection(): Boolean                               = false
    override fun unregisterReceiver()                                      = Unit
    override fun cleanup()                                                 = Unit
}