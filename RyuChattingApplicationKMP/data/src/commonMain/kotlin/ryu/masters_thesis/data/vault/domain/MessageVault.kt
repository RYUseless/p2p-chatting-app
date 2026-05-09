package ryu.masters_thesis.data.vault.domain

import kotlinx.coroutines.flow.Flow

interface MessageVault {

    suspend fun store(
        roomId:    String,
        sender:    String,
        content:   String,
        timestamp: Long,
    ): Result<Unit>

    fun observeByRoom(roomId: String): Flow<List<MessageEntry>>

    suspend fun queryByRoom(roomId: String): Result<List<MessageEntry>>

    suspend fun storeRoomMetadata(
        roomId:      String,
        roomName:    String,
        password:    String,
        timestamp:   Long,
        peerAddress: String?  = null,
        isSaved:     Boolean  = false,
    ): Result<Unit>

    suspend fun setIsSaved(roomId: String, isSaved: Boolean): Result<Unit>

    suspend fun getRooms(): Result<List<RoomSummary>>

    suspend fun deleteRoom(roomId: String): Result<Unit>

    suspend fun clearAll(): Result<Unit>

    fun observeRooms(): Flow<List<RoomSummary>>

    //new
    fun observeIsSaved(roomId: String): Flow<Boolean>

    suspend fun updatePeerAddress(roomId: String, peerAddress: String): Result<Unit>

    suspend fun getIsSaved(roomId: String): Boolean

}