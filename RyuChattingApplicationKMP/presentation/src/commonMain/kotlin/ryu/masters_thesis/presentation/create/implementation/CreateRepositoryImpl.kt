package ryu.masters_thesis.presentation.create.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ryu.masters_thesis.presentation.create.domain.CreateRepository

// TODO DUMMY: veškerá data jsou dummy, až bude BluetoothController z :core dostupný nahradit
class CreateRepositoryImpl : CreateRepository {

    private val _currentRoomId = MutableStateFlow<String?>("room-dummy-123")
    private val _passwordError = MutableStateFlow<String?>(null)

    override fun getCurrentRoomId(): Flow<String?> = _currentRoomId
    override fun getPasswordError(): Flow<String?> = _passwordError

    // TODO DUMMY: prázdné implementace, nahradit BluetoothController voláními
    override suspend fun initRoomId() {}
    override suspend fun setRoomId(roomName: String) {}
    override suspend fun createRoom(password: String): String? = "room-dummy-123"
    override fun cleanup() {
        // TODO DUMMY: až bude BluetoothController z :core, zavolat unregisterReceiver() atd.
    }
}