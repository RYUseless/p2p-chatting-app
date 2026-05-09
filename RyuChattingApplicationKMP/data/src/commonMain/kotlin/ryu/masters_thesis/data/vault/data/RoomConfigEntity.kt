package ryu.masters_thesis.data.vault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_config")
data class RoomConfigEntity(
    @PrimaryKey
    val hashedRoomId: String,
    val chatColorHex: String = "#9E9E9E",
    // Comma-separated MAC addresses — plaintext, UI config není sensitive
    val blacklist:    String = "",
)