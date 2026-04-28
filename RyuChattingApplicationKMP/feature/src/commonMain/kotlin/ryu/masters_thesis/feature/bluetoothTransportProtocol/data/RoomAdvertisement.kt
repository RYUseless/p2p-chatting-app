package ryu.masters_thesis.feature.bluetoothTransportProtocol.data

data class RoomAdvertisement(
    val roomId: String,
    val hostNodeId: String,
    val displayName: String,
    val cost: Int,
    val isPasswordProtected: Boolean,
    val sequenceNo: Int,
    val ttl: Int,
)