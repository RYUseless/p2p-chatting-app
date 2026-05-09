package ryu.masters_thesis.presentation.home.implementation

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.presentation.home.domain.HomeRepository
import ryu.masters_thesis.presentation.home.domain.ReconnectResult

class HomeRepositoryImpl(
    private val messageRepository : MessageRepository,
    private val finderProtocol    : FinderProtocol,
) : HomeRepository {

    override fun observeRooms(): Flow<List<RoomSummary>> =
        messageRepository.observeRoomsFlow()

    override suspend fun deleteRoom(roomId: String): Result<Unit> =
        messageRepository.deleteRoom(roomId)

    //aktualizace, bughunting
    override suspend fun reconnectToRoom(room: RoomSummary): ReconnectResult {
        val knownPeers  = listOfNotNull(room.peerBluetoothAddress)
        val hostAddress = finderProtocol.findHost(
            roomId     = room.roomName,
            knownPeers = knownPeers,
        )
        return if (hostAddress != null) ReconnectResult.HostFound(hostAddress)
        else                            ReconnectResult.BecomeServer
    }
}