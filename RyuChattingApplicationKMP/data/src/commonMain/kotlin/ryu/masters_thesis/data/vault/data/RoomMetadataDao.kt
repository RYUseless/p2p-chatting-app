package ryu.masters_thesis.data.vault.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomMetadataDao {

    @Upsert
    suspend fun upsert(metadata: RoomMetadata)

    @Query("SELECT * FROM room_metadata ORDER BY lastTimestamp DESC")
    fun observeAll(): Flow<List<RoomMetadata>>

    @Query("SELECT * FROM room_metadata ORDER BY lastTimestamp DESC")
    suspend fun getAll(): List<RoomMetadata>

    @Query("DELETE FROM room_metadata WHERE hashedRoomId = :hashedRoomId")
    suspend fun deleteByRoom(hashedRoomId: String)

    @Query("UPDATE room_metadata SET lastTimestamp = :timestamp WHERE hashedRoomId = :hashedRoomId")
    suspend fun updateLastTimestamp(hashedRoomId: String, timestamp: Long)

    @Query("UPDATE room_metadata SET isSaved = :isSaved WHERE hashedRoomId = :hashedRoomId")
    suspend fun updateIsSaved(hashedRoomId: String, isSaved: Boolean)

    @Query("DELETE FROM room_metadata")
    suspend fun clearAll()
}