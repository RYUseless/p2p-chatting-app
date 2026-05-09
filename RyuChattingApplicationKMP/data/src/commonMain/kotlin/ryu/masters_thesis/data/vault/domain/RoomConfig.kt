package ryu.masters_thesis.data.vault.domain

data class RoomConfig(
    val hashedRoomId: String,
    val chatColorHex: String       = "#9E9E9E",
    val blacklist:    List<String> = emptyList(),
)