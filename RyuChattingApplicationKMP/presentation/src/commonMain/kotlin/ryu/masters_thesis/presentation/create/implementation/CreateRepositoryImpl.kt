package ryu.masters_thesis.presentation.create.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.create.domain.CreateRepository
import kotlin.uuid.ExperimentalUuidApi

class CreateRepositoryImpl(
    private val controller: BluetoothController,
) : CreateRepository {

    // roomId generujeme lokálně před předáním controlleru
    private val _currentRoomId = MutableStateFlow<String?>(null)

    override fun getCurrentRoomId(): Flow<String?> = controller.currentRoomId
    override fun getPasswordError(): Flow<String?> = controller.passwordError

    override suspend fun initRoomId() {
        // roomId generuje controller interně při submitServerPassword
        // zde jen resetujeme lokální stav
        _currentRoomId.value = null
    }

    override suspend fun setRoomId(roomName: String) {
        _currentRoomId.value = roomName
    }

    // kekel dostupný od 2.0 kotlinu, verze je 2.3.x, clearly furt experimental :)
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