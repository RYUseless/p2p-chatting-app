package ryu.masters_thesis.data.vault.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomConfigDao {

    @Upsert
    suspend fun upsert(config: RoomConfigEntity)

    @Query("SELECT * FROM room_config WHERE hashedRoomId = :hashedRoomId")
    fun observe(hashedRoomId: String): Flow<RoomConfigEntity?>

    @Query("SELECT * FROM room_config WHERE hashedRoomId = :hashedRoomId")
    suspend fun get(hashedRoomId: String): RoomConfigEntity?

    @Query("DELETE FROM room_config WHERE hashedRoomId = :hashedRoomId")
    suspend fun deleteByRoom(hashedRoomId: String)
}