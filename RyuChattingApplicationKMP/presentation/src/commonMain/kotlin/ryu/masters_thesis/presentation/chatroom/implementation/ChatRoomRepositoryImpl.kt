package ryu.masters_thesis.presentation.chatroom.implementation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.domain.HandoffData
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.feature.messages.domain.RoomConfigRepository
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ChatRoomRepositoryImpl(
    private val controller:       BluetoothController,
    private val serverController: BluetoothController,
    private val channelId:        String,
    private val password:         String,
    private val messageRepo:      MessageRepository,
    private val roomConfigRepo:   RoomConfigRepository,
) : ChatRoomRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())

    //private val _optimisticSent = MutableStateFlow<List<ChatMessage>>(emptyList()) // ← ZMĚNA

    private val _optimisticMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    init {
        observeAndPersistIncoming()
        observeIncomingRoomConfig()
        observeIncomingNicknames()
        //cleanupOptimisticMessages()
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    override fun getMessages(): Flow<List<ChatMessage>> =
        combine(
            messageRepo.observeHistory(channelId),
            _optimisticMessages,
        ) { persisted, optimistic ->
            val persistedMapped = persisted.map { msg ->
                ChatMessage(
                    id   = msg.timestamp.toString(),
                    text = msg.content,
                    time = formatTimestamp(msg.timestamp),
                    isMe = msg.sender == "You",
                )
            }
            val persistedIds = persistedMapped.mapTo(HashSet()) { it.id }

            val toRemove = optimistic.filter { it.id in persistedIds }
            if (toRemove.isNotEmpty()) {
                scope.launch {
                    _optimisticMessages.update { list -> list.filter { it.id !in persistedIds } }
                }
            }

            persistedMapped + optimistic.filter { it.id !in persistedIds }
        }
            .flowOn(Dispatchers.Default)

    @OptIn(ExperimentalTime::class)
    override suspend fun sendMessage(text: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val optimisticMsg = ChatMessage(
            id   = timestamp.toString(),
            text = text,
            time = formatTimestamp(timestamp),
            isMe = true,
        )
        _optimisticMessages.update { it + optimisticMsg }
        controller.sendMessage(channelId, text)
        scope.launch {
            messageRepo.store(
                roomId    = channelId,
                sender    = "You",
                content   = text,
                timestamp = timestamp,
            )
        }
    }

    override suspend fun sendFile(fileName: String, bytes: ByteArray) {
        // TODO: file transfer
    }

    // ── Connection ────────────────────────────────────────────────────────────

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

    override fun getServerHandoffRequired(): Flow<Boolean> = controller.serverHandoffRequired

    override suspend fun promoteToServer(channelId: String, password: String) {
        controller.clearServerHandoff()
        serverController.submitServerPassword(channelId, password)
        val blacklist = roomConfigRepo.observeBlacklist(channelId).first()
        serverController.setBlacklist(channelId, blacklist.toSet())
    }

    // ── Room config ───────────────────────────────────────────────────────────

    override fun getChatColor(): Flow<String> =
        roomConfigRepo.observeColor(channelId)

    override suspend fun saveChatColor(colorHex: String) {
        roomConfigRepo.upsertColor(channelId, colorHex)
        if (serverController.isServer.value) {
            serverController.sendRoomConfig(channelId, _isSaved)
        }
    }

    override fun getBlacklist(): Flow<List<String>> =
        roomConfigRepo.observeBlacklist(channelId)

    override suspend fun saveBlacklist(blacklist: List<String>) {
        roomConfigRepo.upsertBlacklist(channelId, blacklist)
        serverController.setBlacklist(channelId, blacklist.toSet())
    }

    // ── isSaved ───────────────────────────────────────────────────────────────

    private var _isSaved: Boolean = false

    override fun getIsSaved(): Flow<Boolean> =
        messageRepo.observeIsSaved(channelId)

    override suspend fun markRoomAsSaved() {
        messageRepo.storeRoomMetadata(
            roomId      = channelId,
            roomName    = channelId,
            password    = password,
            timestamp   = Clock.System.now().toEpochMilliseconds(),
            peerAddress = resolvePeerAddress(),
            isSaved     = true,
        )
        messageRepo.setIsSaved(channelId, true)
        _isSaved = true
        if (serverController.isServer.value) {
            serverController.sendRoomConfig(channelId, isSaved = true)
        }
    }

    // ── Nicknames ─────────────────────────────────────────────────────────────

    override fun getNicknames(): Flow<Map<String, String>> =
        _nicknames.asStateFlow()

    override suspend fun sendNickname(nickname: String) {
        controller.sendNickname(channelId, nickname)
    }

    // ── Connected peers ───────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getConnectedUserIds(): Flow<List<String>> =
        serverController.isServer.flatMapLatest { isServer ->
            if (isServer) {
                serverController.connectedUserIds
            } else {
                combine(
                    controller.sessionDevice,
                    controller.isVerified,
                ) { device, verified ->
                    if (device != null && verified) listOf(device.address) else emptyList()
                }
            }
        }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun cleanup() {
        scope.cancel()
        _nicknames.value = emptyMap()
        controller.cleanup()
        serverController.cleanup()
        co.touchlab.kermit.Logger.d("ChatRoomRepo") { "Repository cleanup finished for $channelId" }
    }

    // ── Private ───────────────────────────────────────────────────────────────

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
                            val optimisticMsg = ChatMessage(
                                id   = msg.timestamp.toString(),
                                text = msg.content,
                                time = formatTimestamp(msg.timestamp),
                                isMe = false,
                            )
                            _optimisticMessages.update { it + optimisticMsg } // ← ZMĚNA
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

    private fun observeIncomingRoomConfig() {
        scope.launch {
            controller.incomingRoomConfig.collect { packet ->
                if (packet.isSaved) {
                    messageRepo.storeRoomMetadata(
                        roomId      = channelId,
                        roomName    = channelId,
                        password    = password,
                        timestamp   = Clock.System.now().toEpochMilliseconds(),
                        peerAddress = resolvePeerAddress(),
                        isSaved     = true,
                    )
                }
                messageRepo.setIsSaved(channelId, packet.isSaved)
            }
        }
    }

    private fun observeIncomingNicknames() {
        scope.launch {
            controller.incomingNicknames.collect { (mac, nickname) ->
                if (mac.isNotBlank() && nickname.isNotBlank()) {
                    _nicknames.update { it + (mac to nickname) }
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

    private fun resolvePeerAddress(): String? =
        if (serverController.isServer.value)
            serverController.connectedUserIds.value.firstOrNull()
        else
            controller.sessionDevice.value?.address

    override fun getHandoffEvent(): Flow<HandoffData> = controller.handoffEvent

    override suspend fun triggerHandoffAndShutdown(successorMac: String) {
        controller.triggerHandoffAndShutdown(successorMac)
    }

}