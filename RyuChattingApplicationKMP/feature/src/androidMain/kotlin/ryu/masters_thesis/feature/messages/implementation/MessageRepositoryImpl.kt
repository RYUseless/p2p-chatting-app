package ryu.masters_thesis.feature.messages.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.data.vault.domain.MessageVault
import ryu.masters_thesis.data.vault.domain.RoomRole
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.feature.messages.domain.Message
import ryu.masters_thesis.feature.messages.domain.MessageRepository

class MessageRepositoryImpl(
    private val vault: MessageVault,
) : MessageRepository {

    override suspend fun store(
        roomId:    String,
        sender:    String,
        content:   String,
        timestamp: Long,
    ): Result<Unit> = vault.store(roomId, sender, content, timestamp)

    override fun observeHistory(roomId: String): Flow<List<Message>> =
        vault.observeByRoom(roomId).map { entries ->
            entries.map {
                Message(
                    sender    = it.sender,
                    content   = it.content,
                    timestamp = it.timestamp,
                )
            }
        }

    override suspend fun storeRoomMetadata(
        roomId:      String,
        roomName:    String,
        password:    String,
        role:        RoomRole,
        timestamp:   Long,
        peerAddress: String?,
        isSaved:     Boolean,
    ): Result<Unit> = vault.storeRoomMetadata(roomId, roomName, password, role, timestamp, peerAddress, isSaved)

    override suspend fun setIsSaved(roomId: String, isSaved: Boolean): Result<Unit> =
        vault.setIsSaved(roomId, isSaved)

    override suspend fun getRooms(): Result<List<RoomSummary>> = vault.getRooms()

    override suspend fun deleteRoom(roomId: String): Result<Unit> = vault.deleteRoom(roomId)

    override fun observeRoomsFlow(): Flow<List<RoomSummary>> =
        vault.observeRooms()

}