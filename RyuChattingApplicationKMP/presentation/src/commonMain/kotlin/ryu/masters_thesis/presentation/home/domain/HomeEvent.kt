package ryu.masters_thesis.presentation.home.domain

import ryu.masters_thesis.presentation.component.domain.ChatRoomUiModel

// Všechny akce které může uživatel na HomeScreen provést
sealed class HomeEvent {
    object ConnectClicked : HomeEvent()
    object CreateClicked : HomeEvent()
    object SettingsClicked : HomeEvent()
    data class RoomClicked(val room: ChatRoomUiModel) : HomeEvent()
}