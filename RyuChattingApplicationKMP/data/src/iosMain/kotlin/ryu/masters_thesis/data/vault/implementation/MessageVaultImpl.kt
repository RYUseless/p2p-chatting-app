// data/src/iosMain/kotlin/ryu/masters_thesis/data/vault/implementation/MessageVaultImpl.kt

package ryu.masters_thesis.data.vault.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ryu.masters_thesis.data.vault.domain.MessageEntry
import ryu.masters_thesis.data.vault.domain.MessageVault
import ryu.masters_thesis.data.vault.domain.RoomRole
import ryu.masters_thesis.data.vault.domain.RoomSummary

internal class MessageVaultImpl : MessageVault {

    override suspend fun store(
        roomId: String, sender: String, content: String, timestamp: Long,
    ): Result<Unit> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override fun observeByRoom(roomId: String): Flow<List<MessageEntry>> {
        // TODO: iOS vault implementation
        return emptyFlow()
    }

    override suspend fun queryByRoom(roomId: String): Result<List<MessageEntry>> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override suspend fun storeRoomMetadata(
        roomId:      String,
        roomName:    String,
        password:    String,
        role:        RoomRole,
        timestamp:   Long,
        peerAddress: String?,
        isSaved:     Boolean,
    ): Result<Unit> {
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override suspend fun setIsSaved(roomId: String, isSaved: Boolean): Result<Unit> {
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override suspend fun getRooms(): Result<List<RoomSummary>> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override suspend fun deleteRoom(roomId: String): Result<Unit> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override suspend fun clearAll(): Result<Unit> {
        // TODO: iOS vault implementation
        return Result.failure(NotImplementedError("MessageVault not implemented on iOS"))
    }

    override fun observeRooms(): Flow<List<RoomSummary>> = emptyFlow()
}