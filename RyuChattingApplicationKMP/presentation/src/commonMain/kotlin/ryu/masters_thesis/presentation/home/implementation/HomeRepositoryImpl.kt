package ryu.masters_thesis.presentation.home.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import ryu.masters_thesis.presentation.component.implementation.ChatRoomUiMapper
import ryu.masters_thesis.presentation.home.domain.HomeRepository

// TODO DUMMY: flowOf s dummy daty
// až bude BluetoothController dostupný z :core, nahradit skutečným zdrojem
class HomeRepositoryImpl : HomeRepository {
    override fun getChatRooms() = flowOf(ChatRoomUiMapper.toDummy())
}