package ryu.masters_thesis.presentation.connect.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

class ConnectRepositoryImpl(
    private val controller: BluetoothController,
) : ConnectRepository {

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

    override fun getIsConnected(): Flow<Boolean>  = controller.isConnected
    override fun getIsVerified(): Flow<Boolean>   = controller.isVerified
    override fun getIsSearching(): Flow<Boolean>  = controller.isSearching
    override fun getCurrentRoomId(): Flow<String?> = controller.currentRoomId
    override fun getNeedsPassword(): Flow<Boolean> = controller.needsPassword
    override fun getPasswordError(): Flow<String?> = controller.passwordError



    override suspend fun startClientMode() {
        controller.startClientMode()
    }

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
        controller.submitClientPassword(channelId, password)
    }

    override fun unregisterReceiver() {
        controller.unregisterReceiver()
    }

    override fun getConnectionError(): Flow<String?> = controller.connectionError
    override fun clearConnectionError() = controller.clearConnectionError()
}