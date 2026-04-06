package ryu.masters_thesis.ryus_chatting_application.logic.BluetoothNeighbourProtocol

data class LocalDevice(
    // zde vse nahradit za funkce, co primo ziskaji tyto hodnoty
    val deviceName: String?,
    val bluetoothAddress: String?,
    val isDiscoverable: Boolean?,
)

data class DiscoverySession(
    val isScanning: Boolean?,
    val scanStartedAtMs: Long?,
    val lastScanFinishedAtMs: Long?,
)

data class NeighbouringDevice(
    val neighbourNodeName: String?,
    val neighbourBluetoothAddress: String?,
    val isNeighbourAlive: Boolean?,
    val rssi: Int?,
    val lastSeenTimestampMs: Long?,
)

data class NeighbourList(
    val neighbours: List<NeighbouringDevice>?,
    val lastUpdatedMs: Long?,
)

data class RelayHop(
    val hopNodeName: String?,
    val hopBluetoothAddress: String?,
    val hopIndex: Int?,
)

data class RelayPath(
    val destinationNodeName: String?,
    val destinationBluetoothAddress: String?,
    val hops: List<RelayHop>?,
    val totalHopCount: Int?,
    val isPathAlive: Boolean?,
)