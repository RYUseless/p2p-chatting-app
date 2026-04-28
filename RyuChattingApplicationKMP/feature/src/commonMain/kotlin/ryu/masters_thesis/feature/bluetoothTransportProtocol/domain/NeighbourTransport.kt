package ryu.masters_thesis.feature.bluetoothTransportProtocol.domain


import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.LinkStateAdvertisement
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RelayPacket
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RoomAdvertisement

interface NeighbourTransport {
    val incomingHello: Flow<Pair<String, String?>>
    val incomingLsa: Flow<LinkStateAdvertisement>
    val incomingRoomAdv: Flow<RoomAdvertisement>
    val incomingRelay: Flow<RelayPacket>
    val incomingLsaRequest: Flow<String>

    fun sendHello(selfAddress: String, selfName: String?)
    fun floodLsa(lsa: LinkStateAdvertisement)
    fun advertiseRoom(adv: RoomAdvertisement)
    fun sendRelay(packet: RelayPacket, nextHopAddress: String)
    fun requestLsa(selfAddress: String)
}