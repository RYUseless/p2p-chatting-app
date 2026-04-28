package ryu.masters_thesis.feature.bluetoothNeighbourTransport.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MSG_NBR_HELLO
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MSG_NBR_LSA
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MSG_NBR_LSA_REQUEST
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MSG_NBR_ROOM_ADV
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MSG_NBR_TUNNEL
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.LinkStateAdvertisement
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RelayPacket
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RoomAdvertisement
import ryu.masters_thesis.feature.bluetoothTransportProtocol.domain.NeighbourTransport

class NeighbourTransportImpl(
    private val controller: BluetoothController,
    private val channelId: String,
) : NeighbourTransport {

    private val _incomingHello      = MutableSharedFlow<Pair<String, String?>>()
    private val _incomingLsa        = MutableSharedFlow<LinkStateAdvertisement>()
    private val _incomingRoomAdv    = MutableSharedFlow<RoomAdvertisement>()
    private val _incomingRelay      = MutableSharedFlow<RelayPacket>()
    private val _incomingLsaRequest = MutableSharedFlow<String>()

    override val incomingHello: Flow<Pair<String, String?>>   = _incomingHello.asSharedFlow()
    override val incomingLsa: Flow<LinkStateAdvertisement>    = _incomingLsa.asSharedFlow()
    override val incomingRoomAdv: Flow<RoomAdvertisement>     = _incomingRoomAdv.asSharedFlow()
    override val incomingRelay: Flow<RelayPacket>             = _incomingRelay.asSharedFlow()
    override val incomingLsaRequest: Flow<String>             = _incomingLsaRequest.asSharedFlow()

    suspend fun onRawMessage(senderAddress: String, raw: String) {
        when {
            raw.startsWith(MSG_NBR_HELLO)       -> parseHello(raw)
            raw.startsWith(MSG_NBR_LSA)         -> parseLsa(raw)
            raw.startsWith(MSG_NBR_LSA_REQUEST) -> parseLsaRequest(raw)
            raw.startsWith(MSG_NBR_ROOM_ADV)    -> parseRoomAdv(raw)
            raw.startsWith(MSG_NBR_TUNNEL)      -> parseRelay(raw)
        }
    }

    override fun sendHello(selfAddress: String, selfName: String?) {
        val name = selfName ?: ""
        controller.sendMessage(channelId, "$MSG_NBR_HELLO:$selfAddress:$name")
    }

    override fun floodLsa(lsa: LinkStateAdvertisement) {
        val neighboursStr = lsa.neighbours.entries
            .joinToString(",") { "${it.key}:${it.value ?: "null"}" }
        controller.sendMessage(
            channelId,
            "$MSG_NBR_LSA:${lsa.sequenceNo}:${lsa.originId}:${lsa.ttl}:$neighboursStr"
        )
    }

    override fun advertiseRoom(adv: RoomAdvertisement) {
        val pw = if (adv.isPasswordProtected) "1" else "0"
        controller.sendMessage(
            channelId,
            "$MSG_NBR_ROOM_ADV:${adv.sequenceNo}:${adv.roomId}:${adv.hostNodeId}:${adv.displayName}:${adv.cost}:$pw:${adv.ttl}"
        )
    }

    override fun sendRelay(packet: RelayPacket, nextHopAddress: String) {
        controller.sendMessage(
            channelId,
            "$MSG_NBR_TUNNEL:${packet.packetId}:${packet.sourceId}:${packet.destinationId}:${packet.hopCount}:${packet.maxHops}:${packet.channelId}:${packet.payload}"
        )
    }

    override fun requestLsa(selfAddress: String) {
        controller.sendMessage(channelId, "$MSG_NBR_LSA_REQUEST:$selfAddress")
    }

    // --- parsers ---

    private suspend fun parseHello(raw: String) {
        val parts = raw.split(":", limit = 3)
        if (parts.size < 2) return
        val address = parts[1]
        val name    = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
        _incomingHello.emit(address to name)
    }

    private suspend fun parseLsa(raw: String) {
        val parts = raw.split(":", limit = 5)
        if (parts.size < 4) return
        val seqNo    = parts[1].toIntOrNull() ?: return
        val originId = parts[2]
        val ttl      = parts[3].toIntOrNull() ?: return
        val neighbours = if (parts.size >= 5 && parts[4].isNotEmpty()) {
            parts[4].split(",").mapNotNull { entry ->
                val kv = entry.split(":")
                if (kv.size < 2) return@mapNotNull null
                kv[0] to kv[1].toIntOrNull()
            }.toMap()
        } else emptyMap()

        _incomingLsa.emit(
            LinkStateAdvertisement(
                originId        = originId,
                sequenceNo      = seqNo,
                neighbours      = neighbours,
                advertisedRooms = emptyList(),
                ttl             = ttl,
            )
        )
    }

    private suspend fun parseLsaRequest(raw: String) {
        val parts = raw.split(":", limit = 2)
        if (parts.size < 2) return
        _incomingLsaRequest.emit(parts[1])
    }

    private suspend fun parseRoomAdv(raw: String) {
        val parts = raw.split(":", limit = 9)
        if (parts.size < 8) return
        _incomingRoomAdv.emit(
            RoomAdvertisement(
                sequenceNo          = parts[1].toIntOrNull() ?: return,
                roomId              = parts[2],
                hostNodeId          = parts[3],
                displayName         = parts[4],
                cost                = parts[5].toIntOrNull() ?: return,
                isPasswordProtected = parts[6] == "1",
                ttl                 = parts[7].toIntOrNull() ?: return,
            )
        )
    }

    private suspend fun parseRelay(raw: String) {
        val parts = raw.split(":", limit = 8)
        if (parts.size < 8) return
        _incomingRelay.emit(
            RelayPacket(
                packetId      = parts[1],
                sourceId      = parts[2],
                destinationId = parts[3],
                hopCount      = parts[4].toIntOrNull() ?: return,
                maxHops       = parts[5].toIntOrNull() ?: return,
                channelId     = parts[6],
                payload       = parts[7],
            )
        )
    }
}