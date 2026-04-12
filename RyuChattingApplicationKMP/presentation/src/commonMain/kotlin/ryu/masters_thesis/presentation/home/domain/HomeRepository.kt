package ryu.masters_thesis.presentation.home.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

interface HomeRepository {
    fun getChatRooms(): Flow<List<ChatRoomUiModel>>
}