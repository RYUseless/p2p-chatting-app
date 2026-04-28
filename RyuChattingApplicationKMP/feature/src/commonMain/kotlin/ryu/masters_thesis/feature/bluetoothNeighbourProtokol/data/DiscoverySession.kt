package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class DiscoverySession(
    val isScanning: Boolean = false,
    val scanStartedAtMs: Long? = null,
    val lastScanFinishedAtMs: Long? = null,
)