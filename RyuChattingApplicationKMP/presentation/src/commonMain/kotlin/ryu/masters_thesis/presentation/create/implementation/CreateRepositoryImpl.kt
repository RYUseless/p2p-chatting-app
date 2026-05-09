package ryu.masters_thesis.presentation.create.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.create.domain.CreateRepository
import kotlin.uuid.ExperimentalUuidApi

class CreateRepositoryImpl(
    private val controller: BluetoothController,
) : CreateRepository {

    private val _currentRoomId = MutableStateFlow<String?>(null)

    // vrací lokální flow, ne controller.currentRoomId — ten může mít stale data z předchozí session
    override fun getCurrentRoomId(): Flow<String?> = _currentRoomId.asStateFlow()
    override fun getPasswordError(): Flow<String?> = controller.passwordError

    override suspend fun initRoomId() {
        _currentRoomId.value = null
        controller.cleanup()  // possibly navíc, ale jako pojistka good enough za mě
    }

    override suspend fun setRoomId(roomName: String) {
        _currentRoomId.value = roomName
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createRoom(password: String): String {
        val channelId = _currentRoomId.value
            ?: kotlin.uuid.Uuid.random().toString().substring(0, 8)
        controller.submitServerPassword(channelId, password)
        return channelId
    }

    override fun cleanup() {
        controller.cleanup()
    }

    override fun getIsConnected(): Flow<Boolean> = controller.isConnected
    override fun getIsVerified(): Flow<Boolean>  = controller.isVerified

}