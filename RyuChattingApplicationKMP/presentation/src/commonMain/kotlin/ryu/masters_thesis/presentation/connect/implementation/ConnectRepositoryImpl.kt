package ryu.masters_thesis.presentation.connect.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

// TODO DUMMY: veškerá data jsou dummy, až bude BluetoothController z :core dostupný nahradit
class ConnectRepositoryImpl : ConnectRepository {

    private val _scannedDevices = MutableStateFlow<List<ScannedDeviceUiModel>>(
        listOf(
            ScannedDeviceUiModel(address = "AA:BB:CC:DD:EE:01", name = "Device 1", roomId = "Room Alpha"),
            ScannedDeviceUiModel(address = "AA:BB:CC:DD:EE:02", name = "Device 2", roomId = null),
        )
    )

    override fun getScannedDevices(): Flow<List<ScannedDeviceUiModel>> = _scannedDevices
    override fun getIsConnected(): Flow<Boolean> = flowOf(false)
    override fun getIsVerified(): Flow<Boolean> = flowOf(false)
    override fun getIsSearching(): Flow<Boolean> = flowOf(true)
    override fun getCurrentRoomId(): Flow<String?> = flowOf(null)
    override fun getNeedsPassword(): Flow<Boolean> = flowOf(false)
    override fun getPasswordError(): Flow<String?> = flowOf(null)

    // TODO DUMMY: prázdné implementace, nahradit BluetoothController voláními
    override suspend fun startClientMode() {}
    override suspend fun connectToDevice(device: ScannedDeviceUiModel) {}
    override suspend fun submitPassword(password: String) {}
    override fun unregisterReceiver() {}
}