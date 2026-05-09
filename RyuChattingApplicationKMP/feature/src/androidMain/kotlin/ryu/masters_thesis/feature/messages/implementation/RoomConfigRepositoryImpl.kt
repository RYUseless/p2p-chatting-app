package ryu.masters_thesis.feature.messages.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.data.vault.domain.RoomConfigVault
import ryu.masters_thesis.feature.messages.domain.RoomConfigRepository

class RoomConfigRepositoryImpl(
    private val vault: RoomConfigVault,
) : RoomConfigRepository {

    override fun observeColor(roomId: String): Flow<String> =
        vault.observeConfig(roomId).map { it.chatColorHex }

    override fun observeBlacklist(roomId: String): Flow<List<String>> =
        vault.observeConfig(roomId).map { it.blacklist }

    override suspend fun upsertColor(roomId: String, colorHex: String): Result<Unit> {
        val current = vault.getConfig(roomId)
        return vault.upsertConfig(roomId, colorHex, current.blacklist)
    }

    override suspend fun upsertBlacklist(roomId: String, blacklist: List<String>): Result<Unit> {
        val current = vault.getConfig(roomId)
        return vault.upsertConfig(roomId, current.chatColorHex, blacklist)
    }

    override suspend fun deleteConfig(roomId: String): Result<Unit> =
        vault.deleteConfig(roomId)
}