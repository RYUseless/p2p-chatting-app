package ryu.masters_thesis.presentation.component.implementation

import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

// TODO DUMMY: až bude doménový ChatRoom z :core dostupný, nahradit skutečným mapperem
object ChatRoomUiMapper {
    fun toDummy(): List<ChatRoomUiModel> = listOf(
        ChatRoomUiModel(id = "1", name = "Room Alpha", isActive = true),
        ChatRoomUiModel(id = "2", name = "Room Beta",  isActive = false),
        ChatRoomUiModel(id = "3", name = "Room Gamma", isActive = true),
    )
}