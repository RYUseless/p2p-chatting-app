package ryu.masters_thesis.presentation.home.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.data.vault.domain.RoomSummary

interface HomeRepository {
    fun observeRooms(): Flow<List<RoomSummary>>
    suspend fun deleteRoom(roomId: String): Result<Unit>
    suspend fun reconnectToRoom(room: RoomSummary): ReconnectResult  // ← NEW
}