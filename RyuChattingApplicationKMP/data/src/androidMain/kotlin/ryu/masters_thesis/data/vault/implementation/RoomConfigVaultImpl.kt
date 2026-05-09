package ryu.masters_thesis.data.vault.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.data.vault.data.AppDatabase
import ryu.masters_thesis.data.vault.data.RoomConfigEntity
import ryu.masters_thesis.data.vault.domain.RoomConfig
import ryu.masters_thesis.data.vault.domain.RoomConfigVault

class RoomConfigVaultImpl(
    private val db:     AppDatabase,
    private val cipher: VaultCipher,
) : RoomConfigVault {

    override fun observeConfig(roomId: String): Flow<RoomConfig> {
        val hashedRoomId = cipher.hmacRoomId(roomId).getOrElse {
            return flowOf(RoomConfig(hashedRoomId = ""))
        }
        return db.roomConfigDao().observe(hashedRoomId).map { entity ->
            entity?.toDomain() ?: RoomConfig(hashedRoomId = hashedRoomId)
        }
    }

    override suspend fun getConfig(roomId: String): RoomConfig {
        val hashedRoomId = cipher.hmacRoomId(roomId).getOrElse {
            return RoomConfig(hashedRoomId = "")
        }
        return db.roomConfigDao().get(hashedRoomId)?.toDomain()
            ?: RoomConfig(hashedRoomId = hashedRoomId)
    }

    override suspend fun upsertConfig(
        roomId:       String,
        chatColorHex: String,
        blacklist:    List<String>,
    ): Result<Unit> = runCatching {
        val hashedRoomId = cipher.hmacRoomId(roomId).getOrThrow()
        db.roomConfigDao().upsert(
            RoomConfigEntity(
                hashedRoomId = hashedRoomId,
                chatColorHex = chatColorHex,
                blacklist    = blacklist.joinToString(","),
            )
        )
    }

    override suspend fun deleteConfig(roomId: String): Result<Unit> = runCatching {
        val hashedRoomId = cipher.hmacRoomId(roomId).getOrThrow()
        db.roomConfigDao().deleteByRoom(hashedRoomId)
    }

    private fun RoomConfigEntity.toDomain() = RoomConfig(
        hashedRoomId = hashedRoomId,
        chatColorHex = chatColorHex,
        blacklist    = if (blacklist.isBlank()) emptyList()
        else blacklist.split(",").map { it.trim() }.filter { it.isNotBlank() },
    )
}