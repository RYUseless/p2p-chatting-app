package ryu.masters_thesis.data.vault.implementation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ryu.masters_thesis.data.vault.data.AppDatabase
import ryu.masters_thesis.data.vault.data.RoomMetadata
import ryu.masters_thesis.data.vault.data.VaultEntry
import ryu.masters_thesis.data.vault.domain.MAX_CONTENT_LEN
import ryu.masters_thesis.data.vault.domain.MAX_ROOM_ID_LEN
import ryu.masters_thesis.data.vault.domain.MAX_SENDER_LEN
import ryu.masters_thesis.data.vault.domain.MessageEntry
import ryu.masters_thesis.data.vault.domain.MessageVault
import ryu.masters_thesis.data.vault.domain.RoomSummary
import java.util.concurrent.ConcurrentHashMap

class MessageVaultImpl(
    private val db:     AppDatabase,
    private val cipher: VaultCipher,
) : MessageVault {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val hashedIdCache = ConcurrentHashMap<String, String>()
    private val messageCache  = ConcurrentHashMap<Long, MessageEntry>()

    init {
        scope.launch { sweepOrphanKeys() }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun cachedHmacRoomId(roomId: String): Result<String> {
        hashedIdCache[roomId]?.let { return Result.success(it) }
        return cipher.hmacRoomId(roomId).also { result ->
            result.getOrNull()?.let { hashedIdCache[roomId] = it }
        }
    }

    private fun VaultEntry.toMessageEntry(hashedRoomId: String): MessageEntry? {
        val senderBytes    = cipher.decrypt(encryptedSender,    hashedRoomId).getOrNull() ?: return null
        val contentBytes   = cipher.decrypt(encryptedContent,   hashedRoomId).getOrNull() ?: return null
        val timestampBytes = cipher.decrypt(encryptedTimestamp, hashedRoomId).getOrNull() ?: return null

        val sender    = String(senderBytes,    Charsets.UTF_8)
        val content   = String(contentBytes,   Charsets.UTF_8)
        val timestamp = String(timestampBytes, Charsets.UTF_8).toLongOrNull() ?: 0L

        senderBytes.fill(0)
        contentBytes.fill(0)
        timestampBytes.fill(0)

        return MessageEntry(id = id, sender = sender, content = content, timestamp = timestamp)
    }

    private suspend fun sweepOrphanKeys() = withContext(Dispatchers.IO) {
        val keystoreIds = cipher.listRoomKeyHashedIds().toSet()
        val dbIds       = db.messageDao().getDistinctHashedRoomIds().toSet()
        val orphans     = keystoreIds - dbIds
        orphans.forEach { cipher.deleteRoomKey(it) }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    override suspend fun store(
        roomId:    String,
        sender:    String,
        content:   String,
        timestamp: Long,
    ): Result<Unit> = runCatching {
        require(roomId.isNotBlank()  && roomId.length  <= MAX_ROOM_ID_LEN) { "Invalid roomId"  }
        require(sender.isNotBlank()  && sender.length  <= MAX_SENDER_LEN)  { "Invalid sender"  }
        require(content.isNotBlank() && content.length <= MAX_CONTENT_LEN) { "Invalid content" }

        val hashedRoomId   = cachedHmacRoomId(roomId).getOrThrow()
        val senderBytes    = sender.toByteArray(Charsets.UTF_8)
        val contentBytes   = content.toByteArray(Charsets.UTF_8)
        val timestampBytes = timestamp.toString().toByteArray(Charsets.UTF_8)

        val encSender    = cipher.encrypt(senderBytes,    hashedRoomId).getOrThrow()
        val encContent   = cipher.encrypt(contentBytes,   hashedRoomId).getOrThrow()
        val encTimestamp = cipher.encrypt(timestampBytes, hashedRoomId).getOrThrow()

        db.messageDao().insert(
            VaultEntry(
                hashedRoomId       = hashedRoomId,
                encryptedSender    = encSender,
                encryptedContent   = encContent,
                encryptedTimestamp = encTimestamp,
            )
        )

        db.roomMetadataDao().updateLastTimestamp(hashedRoomId, timestamp)
    }

    override fun observeByRoom(roomId: String): Flow<List<MessageEntry>> {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrElse { return emptyFlow() }
        return db.messageDao().observeByRoom(hashedRoomId)
            .map { entries ->
                entries.mapNotNull { entry ->
                    messageCache[entry.id] ?: entry.toMessageEntry(hashedRoomId)?.also {
                        messageCache[entry.id] = it
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun queryByRoom(roomId: String): Result<List<MessageEntry>> = runCatching {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
        db.messageDao().queryByRoom(hashedRoomId).mapNotNull { entry ->
            messageCache[entry.id] ?: entry.toMessageEntry(hashedRoomId)?.also {
                messageCache[entry.id] = it
            }
        }
    }

    override suspend fun storeRoomMetadata(
        roomId:      String,
        roomName:    String,
        password:    String,
        timestamp:   Long,
        peerAddress: String?,
        isSaved:     Boolean,
    ): Result<Unit> = runCatching {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
        val encName      = cipher.encrypt(roomName.toByteArray(Charsets.UTF_8), hashedRoomId).getOrThrow()
        val encPass      = cipher.encrypt(password.toByteArray(Charsets.UTF_8), hashedRoomId).getOrThrow()

        db.roomMetadataDao().upsert(
            RoomMetadata(
                hashedRoomId         = hashedRoomId,
                encryptedRoomName    = encName,
                encryptedPassword    = encPass,
                lastTimestamp        = timestamp,
                peerBluetoothAddress = peerAddress,
                isSaved              = isSaved,
            )
        )
    }

    override suspend fun setIsSaved(roomId: String, isSaved: Boolean): Result<Unit> = runCatching {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
        db.roomMetadataDao().updateIsSaved(hashedRoomId, isSaved)
    }

    override suspend fun getRooms(): Result<List<RoomSummary>> = runCatching {
        val lastEntriesMap = db.messageDao().getLastEntries()
            .associateBy { it.hashedRoomId }

        db.roomMetadataDao().getAll().mapNotNull { metadata ->
            val nameBytes = cipher.decrypt(metadata.encryptedRoomName, metadata.hashedRoomId)
                .getOrNull() ?: return@mapNotNull null
            val roomName = String(nameBytes, Charsets.UTF_8)
            nameBytes.fill(0)

            val lastMessage = lastEntriesMap[metadata.hashedRoomId]?.let { entry ->
                messageCache[entry.id]?.content ?: run {
                    val bytes = cipher.decrypt(entry.encryptedContent, metadata.hashedRoomId).getOrNull()
                    val text  = bytes?.let { String(it, Charsets.UTF_8) }
                    bytes?.fill(0)
                    text
                }
            }

            RoomSummary(
                hashedRoomId         = metadata.hashedRoomId,
                roomName             = roomName,
                lastMessage          = lastMessage,
                lastTimestamp        = metadata.lastTimestamp,
                peerBluetoothAddress = metadata.peerBluetoothAddress,
                isSaved              = metadata.isSaved,
            )
        }
    }

    override fun observeRooms(): Flow<List<RoomSummary>> =
        db.roomMetadataDao().observeAll()
            .map { list ->
                val lastEntriesMap = db.messageDao().getLastEntries()
                    .associateBy { it.hashedRoomId }

                list.mapNotNull { metadata ->
                    val nameBytes = cipher.decrypt(metadata.encryptedRoomName, metadata.hashedRoomId)
                        .getOrNull() ?: return@mapNotNull null
                    val roomName = String(nameBytes, Charsets.UTF_8)
                    nameBytes.fill(0)

                    val lastMessage = lastEntriesMap[metadata.hashedRoomId]?.let { entry ->
                        messageCache[entry.id]?.content ?: run {
                            val bytes = cipher.decrypt(entry.encryptedContent, metadata.hashedRoomId).getOrNull()
                            val text  = bytes?.let { String(it, Charsets.UTF_8) }
                            bytes?.fill(0)
                            text
                        }
                    }

                    RoomSummary(
                        hashedRoomId         = metadata.hashedRoomId,
                        roomName             = roomName,
                        lastMessage          = lastMessage,
                        lastTimestamp        = metadata.lastTimestamp,
                        peerBluetoothAddress = metadata.peerBluetoothAddress,
                        isSaved              = metadata.isSaved,
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun deleteRoom(roomId: String): Result<Unit> = runCatching {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
        db.messageDao().queryByRoom(hashedRoomId).forEach { messageCache.remove(it.id) }
        hashedIdCache.remove(roomId)
        db.messageDao().deleteByRoom(hashedRoomId)
        db.roomMetadataDao().deleteByRoom(hashedRoomId)
        cipher.deleteRoomKey(hashedRoomId)
    }

    override suspend fun clearAll(): Result<Unit> = runCatching {
        messageCache.clear()
        hashedIdCache.clear()
        db.messageDao().clearAll()
        db.roomMetadataDao().clearAll()
        cipher.listRoomKeyHashedIds().forEach { cipher.deleteRoomKey(it) }
    }

    override fun observeIsSaved(roomId: String): Flow<Boolean> = flow {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrElse {
            emit(false)
            return@flow
        }
        emitAll(db.roomMetadataDao().observeIsSaved(hashedRoomId))
    }

    override suspend fun updatePeerAddress(roomId: String, peerAddress: String): Result<Unit> =
        runCatching {
            val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
            db.roomMetadataDao().updatePeerAddress(hashedRoomId, peerAddress)
        }

    override suspend fun getIsSaved(roomId: String): Boolean = runCatching {
        val hashedRoomId = cachedHmacRoomId(roomId).getOrThrow()
        db.roomMetadataDao().getIsSaved(hashedRoomId) ?: false
    }.getOrDefault(false)
}