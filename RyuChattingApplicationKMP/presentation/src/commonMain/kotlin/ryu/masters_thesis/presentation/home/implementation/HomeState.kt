package ryu.masters_thesis.presentation.home.implementation

import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

// Immutable snapshot – jediný zdroj pravdy pro HomeScreen
data class HomeState(
    val chatRooms: List<ChatRoomUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)