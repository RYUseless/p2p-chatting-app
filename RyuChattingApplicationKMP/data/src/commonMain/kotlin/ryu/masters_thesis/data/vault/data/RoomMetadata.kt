package ryu.masters_thesis.data.vault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_metadata")
data class RoomMetadata(
    @PrimaryKey
    val hashedRoomId:         String,
    val encryptedRoomName:    String,
    val encryptedPassword:    String,
    val lastTimestamp:        Long,
    val peerBluetoothAddress: String?  = null,
    val isSaved:              Boolean  = false,
)