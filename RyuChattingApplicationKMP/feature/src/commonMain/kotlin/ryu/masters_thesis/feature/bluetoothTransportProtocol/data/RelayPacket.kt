package ryu.masters_thesis.feature.bluetoothTransportProtocol.data

data class RelayPacket(
    val packetId: String,
    val sourceId: String,
    val destinationId: String,
    val hopCount: Int,
    val maxHops: Int,
    val channelId: String,
    val payload: String, //not sure s timto typem
)