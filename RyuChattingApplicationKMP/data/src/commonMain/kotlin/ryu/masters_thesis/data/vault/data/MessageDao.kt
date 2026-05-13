package ryu.masters_thesis.data.vault.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: VaultEntry)

    @Query("SELECT * FROM messages WHERE hashedRoomId = :hashedRoomId ORDER BY id ASC")
    fun observeByRoom(hashedRoomId: String): Flow<List<VaultEntry>>

    @Query("SELECT * FROM messages WHERE hashedRoomId = :hashedRoomId ORDER BY id ASC")
    suspend fun queryByRoom(hashedRoomId: String): List<VaultEntry>

    @Query("DELETE FROM messages WHERE hashedRoomId = :hashedRoomId")
    suspend fun deleteByRoom(hashedRoomId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("SELECT DISTINCT hashedRoomId FROM messages")
    suspend fun getDistinctHashedRoomIds(): List<String>

    @Query("SELECT * FROM messages WHERE hashedRoomId = :hashedRoomId ORDER BY id DESC LIMIT 1")
    suspend fun getLastEntry(hashedRoomId: String): VaultEntry?

    //optimalizace vykonu
    @Query("SELECT * FROM messages WHERE id IN (SELECT MAX(id) FROM messages GROUP BY hashedRoomId)")
    suspend fun getLastEntries(): List<VaultEntry>

}