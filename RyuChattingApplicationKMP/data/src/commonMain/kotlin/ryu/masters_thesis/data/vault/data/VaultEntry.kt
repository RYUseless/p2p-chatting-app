package ryu.masters_thesis.data.vault.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices   = [Index(value = ["hashedRoomId"])],
)
data class VaultEntry(
    @PrimaryKey(autoGenerate = true)
    val id:                 Long   = 0L,
    val hashedRoomId:       String,
    val encryptedSender:    String,
    val encryptedContent:   String,
    val encryptedTimestamp: String,
)