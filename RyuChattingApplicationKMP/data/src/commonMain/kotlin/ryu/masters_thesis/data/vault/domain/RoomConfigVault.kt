package ryu.masters_thesis.data.vault.domain

import kotlinx.coroutines.flow.Flow

interface RoomConfigVault {
    fun observeConfig(roomId: String): Flow<RoomConfig>
    suspend fun getConfig(roomId: String): RoomConfig
    suspend fun upsertConfig(
        roomId:       String,
        chatColorHex: String,
        blacklist:    List<String>,
    ): Result<Unit>
    suspend fun deleteConfig(roomId: String): Result<Unit>
}