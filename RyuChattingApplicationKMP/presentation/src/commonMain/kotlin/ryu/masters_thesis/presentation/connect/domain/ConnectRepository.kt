package ryu.masters_thesis.presentation.connect.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

interface ConnectRepository {
    // TODO DUMMY: až bude BluetoothController dostupný, nahradit skutečnou implementací
    fun getScannedDevices(): Flow<List<ScannedDeviceUiModel>>
    fun getIsConnected(): Flow<Boolean>
    fun getIsVerified(): Flow<Boolean>
    fun getIsSearching(): Flow<Boolean>
    fun getCurrentRoomId(): Flow<String?>
    fun getNeedsPassword(): Flow<Boolean>
    fun getPasswordError(): Flow<String?>
    suspend fun startClientMode()
    suspend fun connectToDevice(device: ScannedDeviceUiModel)
    suspend fun submitPassword(password: String)
    fun unregisterReceiver()

    fun getConnectionError(): Flow<String?>
    fun clearConnectionError()
}