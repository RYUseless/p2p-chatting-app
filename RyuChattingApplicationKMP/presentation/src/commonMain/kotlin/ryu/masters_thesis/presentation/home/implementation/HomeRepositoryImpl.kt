package ryu.masters_thesis.presentation.home.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.presentation.home.domain.HomeRepository
// pokus o navazani z data: storage logiky

class HomeRepositoryImpl(
    private val messageRepository: MessageRepository,
) : HomeRepository {

    override fun observeRooms(): Flow<List<RoomSummary>> =
        messageRepository.observeRoomsFlow()

    override suspend fun deleteRoom(roomId: String): Result<Unit> =
        messageRepository.deleteRoom(roomId)
}