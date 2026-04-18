package ryu.masters_thesis.feature.bluetooth.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.messages.domain.Message

class BluetoothControllerImpl : BluetoothController {
    override val scannedDevices:      StateFlow<List<BluetoothDevice>>     = MutableStateFlow(emptyList())
    override val isConnected:         StateFlow<Boolean>                    = MutableStateFlow(false)
    override val isVerified:          StateFlow<Boolean>                    = MutableStateFlow(false)
    override val isSearching:         StateFlow<Boolean>                    = MutableStateFlow(false)
    override val isServer:            StateFlow<Boolean>                    = MutableStateFlow(false)
    override val currentRoomId:       StateFlow<String?>                    = MutableStateFlow(null)
    override val needsPassword:       StateFlow<Boolean>                    = MutableStateFlow(false)
    override val passwordError:       StateFlow<String?>                    = MutableStateFlow(null)
    override val connectedDeviceName: StateFlow<String?>                    = MutableStateFlow(null)
    override val channelMessages:     StateFlow<Map<String, List<Message>>> = MutableStateFlow(emptyMap())

    override fun startClientMode()                                             = TODO("iOS: CoreBluetooth")
    override suspend fun connectToDevice(device: BluetoothDevice)              = TODO("iOS: CoreBluetooth")
    override fun submitClientPassword(channelId: String, password: String)     = TODO("iOS: CoreBluetooth")
    override fun submitServerPassword(channelId: String, password: String)     = TODO("iOS: CoreBluetooth")
    override fun sendMessage(channelId: String, text: String)                  = TODO("iOS: CoreBluetooth")
    override fun getMessages(channelId: String): List<Message>                 = emptyList()
    override fun verifyConnection(): Boolean                                   = false
    override fun unregisterReceiver()                                          = Unit
    override fun cleanup()                                                     = Unit
}