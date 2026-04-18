package ryu.masters_thesis.presentation.chatroom.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository

class ChatRoomRepositoryImpl(
    private val controller: BluetoothController,
    private val channelId:  String,
) : ChatRoomRepository {

    override fun getMessages(): Flow<List<ChatMessage>> =
        controller.channelMessages.map { map ->
            map[channelId]?.mapIndexed { index, msg ->
                ChatMessage(
                    id   = index.toString(),
                    text = msg.content,
                    time = formatTimestamp(msg.timestamp),
                    isMe = msg.sender == "You",
                )
            } ?: emptyList()
        }

    override fun getIsConnected(): Flow<Boolean>   = controller.isConnected
    override fun getIsVerified(): Flow<Boolean>    = controller.isVerified
    override fun getCurrentRoomId(): Flow<String?> = controller.currentRoomId

    override suspend fun sendMessage(text: String) {
        controller.sendMessage(channelId, text)
    }

    override suspend fun sendFile(fileName: String, bytes: ByteArray) {
        // TODO: file transfer -- posilani fotek a dalsich picovin
        // -- nizsi priorita --
    }

    override fun cleanup() {
        controller.cleanup()
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val totalSeconds = timestamp / 1000
        val hours   = (totalSeconds / 3600) % 24
        val minutes = (totalSeconds / 60) % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }
}