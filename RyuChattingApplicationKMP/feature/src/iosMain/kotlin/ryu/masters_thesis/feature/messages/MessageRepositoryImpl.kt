package ryu.masters_thesis.feature.messages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ryu.masters_thesis.data.vault.domain.RoomRole
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.feature.messages.domain.Message
import ryu.masters_thesis.feature.messages.domain.MessageRepository

class MessageRepositoryImpl : MessageRepository {

    override suspend fun store(
        roomId:    String,
        sender:    String,
        content:   String,
        timestamp: Long,
    ): Result<Unit> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageRepository not implemented on iOS"))
    }

    override fun observeHistory(roomId: String): Flow<List<Message>> {
        // TODO: iOS vault implementation
        return emptyFlow()
    }

    override suspend fun storeRoomMetadata(
        roomId:      String,
        roomName:    String,
        password:    String,
        timestamp:   Long,
        peerAddress: String?,
        isSaved:     Boolean,
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun setIsSaved(
        roomId: String,
        isSaved: Boolean
    ): Result<Unit> {
        TODO("Not yet implemented")
    }


    override suspend fun getRooms(): Result<List<RoomSummary>> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageRepository not implemented on iOS"))
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageRepository not implemented on iOS"))
    }

    override fun observeRoomsFlow(): Flow<List<RoomSummary>> = emptyFlow()

    override fun observeIsSaved(roomId: String): Flow<Boolean> = emptyFlow()

    override suspend fun updatePeerAddress(
        roomId: String,
        peerAddress: String,
    ): Result<Unit> = Result.failure(NotImplementedError("Not implemented on iOS"))

    override suspend fun getIsSaved(roomId: String): Boolean = false
}