package ryu.masters_thesis.feature.messages.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.data.vault.domain.RoomSummary

interface MessageRepository {

    suspend fun store(
        roomId:    String,
        sender:    String,
        content:   String,
        timestamp: Long,
    ): Result<Unit>

    fun observeHistory(roomId: String): Flow<List<Message>>

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

    fun observeRoomsFlow(): Flow<List<RoomSummary>>

    fun observeIsSaved(roomId: String): Flow<Boolean>

    suspend fun updatePeerAddress(roomId: String, peerAddress: String): Result<Unit>

    suspend fun getIsSaved(roomId: String): Boolean
}