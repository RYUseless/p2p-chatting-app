package ryu.masters_thesis.presentation.chatroom.domain

import kotlinx.coroutines.flow.Flow

interface ChatRoomRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    fun getIsConnected(): Flow<Boolean>
    fun getIsVerified(): Flow<Boolean>
    fun getCurrentRoomId(): Flow<String?>
    fun getServerHandoffRequired(): Flow<Boolean>
    suspend fun promoteToServer(channelId: String, password: String)
    suspend fun sendMessage(text: String)
    suspend fun sendFile(fileName: String, bytes: ByteArray)
    fun cleanup()
}