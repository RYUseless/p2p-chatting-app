package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class RelayPath(
    val destinationNodeName: String?,
    val destinationBluetoothAddress: String,
    val hops: List<RelayHop>,
    val totalHopCount: Int,
    val totalCost: Int,
    val isPathAlive: Boolean,
)