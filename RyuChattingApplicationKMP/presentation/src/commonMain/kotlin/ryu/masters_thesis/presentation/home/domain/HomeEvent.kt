package ryu.masters_thesis.presentation.home.domain

import ryu.masters_thesis.data.vault.domain.RoomSummary

// Všechny akce které může uživatel na HomeScreen provést
sealed class HomeEvent {
    object ConnectClicked                        : HomeEvent()
    object CreateClicked                         : HomeEvent()
    object SettingsClicked                       : HomeEvent()
    data class RoomClicked(val room: RoomSummary)        : HomeEvent()
    data class DeleteRoomClicked(val roomId: String)     : HomeEvent()
}