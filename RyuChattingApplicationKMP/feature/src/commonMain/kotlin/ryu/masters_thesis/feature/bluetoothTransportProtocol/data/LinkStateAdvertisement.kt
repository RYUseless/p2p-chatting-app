package ryu.masters_thesis.feature.bluetoothTransportProtocol.data

data class LinkStateAdvertisement(
    val originId: String,
    val sequenceNo: Int,
    val neighbours: Map<String, Int?>,      // neighbourAddress → rssi
    val advertisedRooms: List<RoomAdvertisement>,
    val ttl: Int,
)