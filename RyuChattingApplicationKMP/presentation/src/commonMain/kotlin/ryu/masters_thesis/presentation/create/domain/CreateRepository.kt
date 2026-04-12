package ryu.masters_thesis.presentation.create.domain

import kotlinx.coroutines.flow.Flow

interface CreateRepository {
    fun getCurrentRoomId(): Flow<String?>
    fun getPasswordError(): Flow<String?>
    suspend fun initRoomId()
    suspend fun setRoomId(roomName: String)
    // TODO DUMMY: až bude BluetoothController z :core dostupný, nahradit skutečnou implementací
    suspend fun createRoom(password: String): String?
    //cleanup
    fun cleanup()
}