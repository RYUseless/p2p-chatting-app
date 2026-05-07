// presentation/src/commonMain/kotlin/ryu/masters_thesis/presentation/chatroom/implementation/ChatRoomRepositoryImpl.kt

package ryu.masters_thesis.presentation.chatroom.implementation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ChatRoomRepositoryImpl(
    private val controller:        BluetoothController,
    private val serverController:  BluetoothController,
    private val channelId:         String,
    private val messageRepo:       MessageRepository,
) : ChatRoomRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    init {
        observeAndPersistIncoming()
    }

    override fun getMessages(): Flow<List<ChatMessage>> =
        messageRepo.observeHistory(channelId).map { messages ->
            messages.map { msg ->
                ChatMessage(
                    id   = msg.timestamp.toString(),
                    text = msg.content,
                    time = formatTimestamp(msg.timestamp),
                    isMe = msg.sender == "You",
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getIsConnected(): Flow<Boolean> =
        serverController.isServer.flatMapLatest { isServer ->
            if (isServer) serverController.isConnected else controller.isConnected
        }
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getIsVerified(): Flow<Boolean> =
        serverController.isServer.flatMapLatest { isServer ->
            if (isServer) serverController.isVerified else controller.isVerified
        }
    override fun getCurrentRoomId(): Flow<String?> = controller.currentRoomId

    @OptIn(ExperimentalTime::class)
    override suspend fun sendMessage(text: String) {
        controller.sendMessage(channelId, text)
        messageRepo.store(
            roomId    = channelId,
            sender    = "You",
            content   = text,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun sendFile(fileName: String, bytes: ByteArray) {
        // TODO: file transfer -- posilani fotek a dalsich picovin
        // -- nizsi priorita --
    }

    override fun cleanup() {
        scope.cancel()
        controller.cleanup()
    }

    private fun observeAndPersistIncoming() {
        scope.launch {
            var lastKnownCount = 0
            controller.channelMessages.collect { map ->
                val messages = map[channelId] ?: return@collect
                if (messages.size > lastKnownCount) {
                    messages
                        .drop(lastKnownCount)
                        .filter { it.sender != "You" }
                        .forEach { msg ->
                            messageRepo.store(
                                roomId    = channelId,
                                sender    = msg.sender,
                                content   = msg.content,
                                timestamp = msg.timestamp,
                            )
                        }
                    lastKnownCount = messages.size
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val totalSeconds = timestamp / 1000
        val hours        = (totalSeconds / 3600) % 24
        val minutes      = (totalSeconds / 60) % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    override fun getServerHandoffRequired(): Flow<Boolean> =
        controller.serverHandoffRequired

    override suspend fun promoteToServer(channelId: String, password: String) {
        controller.clearServerHandoff()
        serverController.submitServerPassword(channelId, password)
    }
}