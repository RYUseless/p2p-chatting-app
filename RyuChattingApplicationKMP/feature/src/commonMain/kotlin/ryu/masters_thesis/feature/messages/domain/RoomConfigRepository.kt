package ryu.masters_thesis.feature.messages.domain

import kotlinx.coroutines.flow.Flow

interface RoomConfigRepository {
    fun observeColor(roomId: String): Flow<String>
    fun observeBlacklist(roomId: String): Flow<List<String>>
    suspend fun upsertColor(roomId: String, colorHex: String): Result<Unit>
    suspend fun upsertBlacklist(roomId: String, blacklist: List<String>): Result<Unit>
    suspend fun deleteConfig(roomId: String): Result<Unit>
}