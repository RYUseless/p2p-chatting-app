package ryu.masters_thesis.presentation.connect.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

interface ConnectRepository {
    fun getScannedDevices(): Flow<List<ScannedDeviceUiModel>>
    fun getIsConnected(): Flow<Boolean>
    fun getIsVerified(): Flow<Boolean>
    fun getIsSearching(): Flow<Boolean>
    fun getCurrentRoomId(): Flow<String?>
    fun getPassword(): Flow<String?>
    fun getNeedsPassword(): Flow<Boolean>
    fun getPasswordError(): Flow<String?>
    fun getConnectionError(): Flow<String?>
    fun getConnectionState(): Flow<ConnectionState>
    fun getCanReconnect(): Flow<Boolean>
    fun getSessionDevice(): Flow<ScannedDeviceUiModel?>
    suspend fun startClientMode()
    suspend fun connectToDevice(device: ScannedDeviceUiModel)
    suspend fun submitPassword(password: String)
    suspend fun reconnect()
    fun unregisterReceiver()
    fun clearConnectionError()
}