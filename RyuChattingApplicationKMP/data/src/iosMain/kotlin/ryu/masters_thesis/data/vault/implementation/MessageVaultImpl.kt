package ryu.masters_thesis.data.vault.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ryu.masters_thesis.data.vault.domain.RoomConfig
import ryu.masters_thesis.data.vault.domain.RoomConfigVault

class RoomConfigVaultImpl : RoomConfigVault {

    override fun observeConfig(roomId: String): Flow<RoomConfig> =
        flowOf(RoomConfig(hashedRoomId = ""))

    override suspend fun getConfig(roomId: String): RoomConfig =
        RoomConfig(hashedRoomId = "")

    override suspend fun upsertConfig(
        roomId:       String,
        chatColorHex: String,
        blacklist:    List<String>,
    ): Result<Unit> = Result.failure(NotImplementedError("RoomConfigVault not implemented on iOS"))

    override suspend fun deleteConfig(roomId: String): Result<Unit> =
        Result.failure(NotImplementedError("RoomConfigVault not implemented on iOS"))
}