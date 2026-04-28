package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class NeighbouringDevice(
    val neighbourNodeName: String?,
    val neighbourBluetoothAddress: String,
    val isNeighbourAlive: Boolean,
    val rssi: Int?,
    val lastSeenTimestampMs: Long,
)