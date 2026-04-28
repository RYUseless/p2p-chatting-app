package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain

import kotlinx.coroutines.flow.StateFlow
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.DiscoverySession
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.MeshRoom
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.NeighbourList
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.RoutingTable

interface NeighbourProtocol {
    val discoverySession: StateFlow<DiscoverySession>
    val visibleNodes: StateFlow<NeighbourList>
    val routingTable: StateFlow<RoutingTable>
    val meshRooms: StateFlow<List<MeshRoom>>

    fun startDiscovery()
    fun stopDiscovery()
    fun requestLsaUpdate()
    fun openTunnel(destinationAddress: String, channelId: String): TunnelSession
}