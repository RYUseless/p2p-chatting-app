package ryu.masters_thesis.feature.bluetoothNeighbourProtocol.implementation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.DiscoverySession
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.MeshRoom
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.NeighbouringDevice
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.NeighbourList
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.RelayHop
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.RelayPath
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.RoutingTable
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.HELLO_INTERVAL_MS
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.LSA_INTERVAL_MS
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.LocalDevice
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MAX_LSA_TTL
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.MAX_ROOM_ADV_HOPS
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NEIGHBOUR_DEAD_MS
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.TunnelSession
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.LinkStateAdvertisement
import ryu.masters_thesis.feature.bluetoothTransportProtocol.data.RoomAdvertisement
import ryu.masters_thesis.feature.bluetoothTransportProtocol.domain.NeighbourTransport
import kotlin.collections.get



class NeighbourProtocolImpl(
    private val transport: NeighbourTransport,
    private val localDevice: LocalDevice,
) : NeighbourProtocol {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val selfAddress: String get() = localDevice.getBluetoothAddress() ?: ""
    private val selfName: String    get() = localDevice.getDeviceName() ?: ""

    private val lsdb     = mutableMapOf<String, Map<String, Int?>>()
    private val nameMap  = mutableMapOf<String, String?>()
    private val seqNoMap = mutableMapOf<String, Int>()

    private val _discoverySession = MutableStateFlow(DiscoverySession())
    private val _visibleNodes     = MutableStateFlow(NeighbourList())
    private val _routingTable     = MutableStateFlow(RoutingTable())
    private val _meshRooms        = MutableStateFlow<List<MeshRoom>>(emptyList())

    override val discoverySession: StateFlow<DiscoverySession> = _discoverySession.asStateFlow()
    override val visibleNodes: StateFlow<NeighbourList>        = _visibleNodes.asStateFlow()
    override val routingTable: StateFlow<RoutingTable>         = _routingTable.asStateFlow()
    override val meshRooms: StateFlow<List<MeshRoom>>          = _meshRooms.asStateFlow()

    private var helloJob: Job? = null
    private var lsaJob: Job?   = null
    private var deadJob: Job?  = null

    override fun startDiscovery() {
        val self = selfAddress
        val name = selfName
        _discoverySession.update { it.copy(isScanning = true, scanStartedAtMs = now()) }
        collectIncoming()

        helloJob = scope.launch {
            while (true) {
                transport.sendHello(self, name)
                delay(HELLO_INTERVAL_MS)
            }
        }

        lsaJob = scope.launch {
            while (true) {
                floodLsa()
                delay(LSA_INTERVAL_MS)
            }
        }

        deadJob = scope.launch {
            while (true) {
                delay(HELLO_INTERVAL_MS)
                pruneDeadNeighbours()
            }
        }
    }

    override fun stopDiscovery() {
        helloJob?.cancel()
        lsaJob?.cancel()
        deadJob?.cancel()
        _discoverySession.update {
            it.copy(isScanning = false, lastScanFinishedAtMs = now())
        }
    }

    override fun requestLsaUpdate() {
        val self = selfAddress
        transport.requestLsa(self)
    }

    override fun openTunnel(destinationAddress: String, channelId: String): TunnelSession {
        val self = selfAddress
        if (_routingTable.value.isStale(now())) requestLsaUpdate()
        return TunnelSessionImpl(
            destinationAddress = destinationAddress,
            channelId          = channelId,
            transport          = transport,
            routingTable       = _routingTable,
            selfAddress        = self,
        )
    }

    private fun collectIncoming() {
        scope.launch {
            transport.incomingHello.collect { (address, name) ->
                onHello(address, name)
            }
        }
        scope.launch {
            transport.incomingLsa.collect { lsa ->
                onLsa(lsa)
            }
        }
        scope.launch {
            transport.incomingRoomAdv.collect { adv ->
                onRoomAdv(adv)
            }
        }
        scope.launch {
            transport.incomingLsaRequest.collect {
                floodLsa()
            }
        }
    }

    private fun onHello(address: String, name: String?) {
        nameMap[address] = name
        val existing = _visibleNodes.value.neighbours.find {
            it.neighbourBluetoothAddress == address
        }
        val updated = existing?.copy(
            isNeighbourAlive    = true,
            lastSeenTimestampMs = now(),
            neighbourNodeName   = name,
        ) ?: NeighbouringDevice(
            neighbourNodeName         = name,
            neighbourBluetoothAddress = address,
            isNeighbourAlive          = true,
            rssi                      = null,
            lastSeenTimestampMs       = now(),
        )
        val wasNew = existing == null
        updateNeighbourList(updated)
        if (wasNew) floodLsa()
    }

    private fun onLsa(lsa: LinkStateAdvertisement) {
        val lastSeq = seqNoMap[lsa.originId] ?: -1
        if (lsa.sequenceNo <= lastSeq) return
        seqNoMap[lsa.originId] = lsa.sequenceNo

        lsdb[lsa.originId] = lsa.neighbours
        lsa.neighbours.keys.forEach { addr ->
            if (!nameMap.containsKey(addr)) nameMap[addr] = null
        }

        rebuildRoutingTable()

        if (lsa.ttl > 0) {
            transport.floodLsa(lsa.copy(ttl = lsa.ttl - 1))
        }
    }

    private fun onRoomAdv(adv: RoomAdvertisement) {
        val lastSeq = seqNoMap["room_${adv.roomId}"] ?: -1
        if (adv.sequenceNo <= lastSeq) return
        seqNoMap["room_${adv.roomId}"] = adv.sequenceNo

        if (adv.cost > MAX_ROOM_ADV_HOPS) return

        val room = MeshRoom(
            roomId              = adv.roomId,
            hostNodeAddress     = adv.hostNodeId,
            displayName         = adv.displayName,
            cost                = adv.cost,
            isPasswordProtected = adv.isPasswordProtected,
        )
        _meshRooms.update { current ->
            current.filter { it.roomId != room.roomId } + room
        }

        if (adv.ttl > 0) {
            transport.advertiseRoom(adv.copy(ttl = adv.ttl - 1, cost = adv.cost + 1))
        }
    }

    private fun floodLsa() {
        val self = selfAddress
        val currentNeighbours = _visibleNodes.value.neighbours
            .filter { it.isNeighbourAlive }
            .associate { it.neighbourBluetoothAddress to it.rssi }

        lsdb[self] = currentNeighbours

        val seqNo = (seqNoMap[self] ?: 0) + 1
        seqNoMap[self] = seqNo

        transport.floodLsa(
            LinkStateAdvertisement(
                originId        = self,
                sequenceNo      = seqNo,
                neighbours      = currentNeighbours,
                advertisedRooms = emptyList(),
                ttl             = MAX_LSA_TTL,
            )
        )

        rebuildRoutingTable()
    }

    private fun pruneDeadNeighbours() {
        val threshold = now() - NEIGHBOUR_DEAD_MS
        val hadChanges = _visibleNodes.value.neighbours.any {
            it.isNeighbourAlive && it.lastSeenTimestampMs < threshold
        }
        if (!hadChanges) return

        _visibleNodes.update { current ->
            current.copy(
                neighbours = current.neighbours.map {
                    if (it.isNeighbourAlive && it.lastSeenTimestampMs < threshold)
                        it.copy(isNeighbourAlive = false)
                    else it
                },
                lastUpdatedMs = now(),
            )
        }
        floodLsa()
    }

    private fun updateNeighbourList(device: NeighbouringDevice) {
        _visibleNodes.update { current ->
            val updated = current.neighbours
                .filter { it.neighbourBluetoothAddress != device.neighbourBluetoothAddress } + device
            current.copy(neighbours = updated, lastUpdatedMs = now())
        }
    }

    private fun rebuildRoutingTable() {
        val self     = selfAddress
        val nowMs    = now()
        val allNodes = (lsdb.keys + lsdb.values.flatMap { it.keys }).toSet()
        val dist     = allNodes.associateWith { Int.MAX_VALUE }.toMutableMap()
        val prev     = mutableMapOf<String, String>()
        val visited  = mutableSetOf<String>()

        dist[self] = 0

        repeat(allNodes.size) {
            val node = dist.entries
                .filter { it.key !in visited }
                .minByOrNull { it.value }
                ?.key ?: return@repeat

            if (dist[node] == Int.MAX_VALUE) return@repeat
            visited.add(node)

            lsdb[node]?.forEach { (neighbour, rssi) ->
                val newCost = dist[node]!! + linkCost(rssi)
                if (newCost < (dist[neighbour] ?: Int.MAX_VALUE)) {
                    dist[neighbour] = newCost
                    prev[neighbour] = node
                }
            }
        }

        val routes = mutableMapOf<String, RelayPath>()
        dist.entries
            .filter { it.key != self && it.value < Int.MAX_VALUE }
            .forEach { (dest, cost) ->
                val path = mutableListOf<String>()
                var cur: String? = dest
                while (cur != null && cur != self) {
                    val step: String = cur
                    path.add(0, step)
                    cur = prev[step]
                }
                if (path.isNotEmpty()) {
                    routes[dest] = RelayPath(
                        destinationNodeName         = nameMap[dest],
                        destinationBluetoothAddress = dest,
                        hops = path.mapIndexed { i, addr ->
                            RelayHop(
                                hopNodeName         = nameMap[addr],
                                hopBluetoothAddress = addr,
                                hopIndex            = i,
                            )
                        },
                        totalHopCount = path.size,
                        totalCost     = cost,
                        isPathAlive   = true,
                    )
                }
            }

        _routingTable.update {
            RoutingTable(routes = routes, lastUpdatedMs = nowMs)
        }
    }

    private fun linkCost(rssi: Int?): Int =
        1 + if (rssi != null) ((-rssi - 40).coerceAtLeast(0) / 10) else RSSI_UNKNOWN_PENALTY

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        private const val RSSI_UNKNOWN_PENALTY = 5
    }
}