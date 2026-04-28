package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class NeighbourList(
    val neighbours: List<NeighbouringDevice> = emptyList(),
    val lastUpdatedMs: Long = 0L,
)