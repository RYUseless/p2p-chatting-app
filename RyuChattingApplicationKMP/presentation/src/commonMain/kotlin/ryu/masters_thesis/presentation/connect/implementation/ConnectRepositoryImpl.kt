package ryu.masters_thesis.presentation.connect.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

class ConnectRepositoryImpl(
    private val controller: BluetoothController,
) : ConnectRepository {
    private val _password = MutableStateFlow<String?>(null)

    override fun getScannedDevices(): Flow<List<ScannedDeviceUiModel>> =
        controller.scannedDevices.map { devices ->
            devices.map { d ->
                ScannedDeviceUiModel(
                    address = d.address,
                    name    = d.name,
                    roomId  = d.roomId,
                )
            }
        }

    override fun getSessionDevice(): Flow<ScannedDeviceUiModel?> =
        controller.sessionDevice.map { device ->
            device?.let {
                ScannedDeviceUiModel(
                    address = it.address,
                    name    = it.name,
                    roomId  = it.roomId,
                )
            }
        }

    override suspend fun startClientMode() = controller.startClientMode()

    override suspend fun connectToDevice(device: ScannedDeviceUiModel) {
        controller.connectToDevice(
            BluetoothDevice(
                name    = device.name,
                address = device.address,
                roomId  = device.roomId,
            )
        )
    }

    override suspend fun submitPassword(password: String) {
        val channelId = controller.currentRoomId.value ?: return
        _password.value = password
        controller.submitClientPassword(channelId, password)
    }

    override suspend fun reconnect() = controller.reconnect()

    override fun unregisterReceiver()  = controller.unregisterReceiver()
    override fun clearConnectionError() = controller.clearConnectionError()

    override fun getIsConnected(): Flow<Boolean>   = controller.isConnected
    override fun getIsVerified(): Flow<Boolean>    = controller.isVerified
    override fun getIsSearching(): Flow<Boolean>   = controller.isSearching
    override fun getCurrentRoomId(): Flow<String?> = controller.currentRoomId
    override fun getNeedsPassword(): Flow<Boolean> = controller.needsPassword
    override fun getPasswordError(): Flow<String?> = controller.passwordError
    override fun getConnectionError(): Flow<String?> = controller.connectionError
    override fun getConnectionState(): Flow<ConnectionState> = controller.connectionState
    override fun getCanReconnect(): Flow<Boolean>  = controller.canReconnect
    override fun getPassword(): Flow<String?>      = _password.asStateFlow()
}