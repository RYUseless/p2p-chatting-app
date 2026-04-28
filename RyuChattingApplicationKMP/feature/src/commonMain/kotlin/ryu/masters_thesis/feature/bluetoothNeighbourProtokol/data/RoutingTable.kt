package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.LSA_INTERVAL_MS

data class RoutingTable(
    val routes: Map<String, RelayPath> = emptyMap(),
    val lastUpdatedMs: Long = 0L,
) {
    fun getBestPath(destinationAddress: String): RelayPath? =
        routes[destinationAddress]

    fun getNextHop(destinationAddress: String): String? =
        routes[destinationAddress]?.hops?.firstOrNull()?.hopBluetoothAddress

    fun isReachable(destinationAddress: String): Boolean =
        routes.containsKey(destinationAddress)

    fun isStale(nowMs: Long): Boolean =
        nowMs - lastUpdatedMs > LSA_INTERVAL_MS
}