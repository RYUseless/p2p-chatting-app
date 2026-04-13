package ryu.masters_thesis.presentation.chatroom.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository

// TODO DUMMY: veškerá data jsou dummy, až bude BluetoothController z :core dostupný nahradit
class ChatRoomRepositoryImpl : ChatRoomRepository {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(id = "1", text = "Bogos Binted?", time = "12:00", isMe = false),
            ChatMessage(id = "2", text = "huh?",          time = "12:01", isMe = true),
            ChatMessage(id = "3", text = "worp?",          time = "12:02", isMe = false),
            ChatMessage(id = "4", text = "👽",             time = "12:03", isMe = true),
        )
    )

    override fun getMessages(): Flow<List<ChatMessage>>  = _messages
    override fun getIsConnected(): Flow<Boolean>         = flowOf(true)
    override fun getIsVerified(): Flow<Boolean>          = flowOf(true)
    override fun getCurrentRoomId(): Flow<String?>       = flowOf("room-dummy-123")

    // TODO DUMMY: prázdné implementace, nahradit BluetoothController voláními
    override suspend fun sendMessage(text: String) {
        val new = ChatMessage(
            id     = (_messages.value.size + 1).toString(),
            text   = text,
            time   = "now",
            isMe   = true,
        )
        _messages.value = _messages.value + new
    }
    override suspend fun sendFile(fileName: String, bytes: ByteArray) {}
    override fun cleanup() {}
}