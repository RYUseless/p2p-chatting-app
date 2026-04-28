package ryu.masters_thesis.feature.bluetoothNeighbourProtocol.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.TunnelSession
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RelayPacket
import ryu.masters_thesis.feature.bluetoothTransportProtocol.domain.NeighbourTransport
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.RoutingTable
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MAX_TUNNEL_HOPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class TunnelSessionImpl(
    private val destinationAddress: String,
    private val channelId: String,
    private val transport: NeighbourTransport,
    private val routingTable: StateFlow<RoutingTable>,
    private val selfAddress: String,
) : TunnelSession {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _incomingMessages = MutableSharedFlow<String>()
    private val _isActive         = MutableStateFlow(true)

    override val incomingMessages: Flow<String>    = _incomingMessages.asSharedFlow()
    override val isActive: StateFlow<Boolean>      = _isActive.asStateFlow()

    init {
        scope.launch {
            transport.incomingRelay.collect { packet ->
                if (packet.destinationId == selfAddress && packet.channelId == channelId) {
                    _incomingMessages.emit(packet.payload)
                }
            }
        }
    }

    override fun send(payload: String) {
        val nextHop = routingTable.value.getNextHop(destinationAddress) ?: return
        transport.sendRelay(
            packet = RelayPacket(
                packetId      = UUID.randomUUID().toString(),
                sourceId      = selfAddress,
                destinationId = destinationAddress,
                hopCount      = 0,
                maxHops       = MAX_TUNNEL_HOPS,
                channelId     = channelId,
                payload       = payload,
            ),
            nextHopAddress = nextHop,
        )
    }

    override fun close() {
        _isActive.value = false
    }
}