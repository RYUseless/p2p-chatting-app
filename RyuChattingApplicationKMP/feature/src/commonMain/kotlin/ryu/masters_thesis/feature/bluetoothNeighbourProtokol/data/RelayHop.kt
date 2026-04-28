package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class RelayHop(
    val hopNodeName: String?,
    val hopBluetoothAddress: String,
    val hopIndex: Int,
)