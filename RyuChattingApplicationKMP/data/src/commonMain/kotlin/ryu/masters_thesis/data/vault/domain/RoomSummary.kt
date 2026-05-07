package ryu.masters_thesis.data.vault.domain

data class RoomSummary(
    val hashedRoomId:  String,
    val roomName:      String,
    val role:          RoomRole,
    val lastMessage:   String?,
    val lastTimestamp: Long,
    val peerBluetoothAddress: String? = null,
    val isSaved:              Boolean  = false,
)