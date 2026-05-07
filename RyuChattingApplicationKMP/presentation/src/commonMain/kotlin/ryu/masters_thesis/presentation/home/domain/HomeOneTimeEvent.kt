package ryu.masters_thesis.presentation.home.domain

import ryu.masters_thesis.data.vault.domain.RoomSummary

// Jednorázové efekty – nezapisují se do HomeState
// UI je konzumuje přes LaunchedEffect na SharedFlow

sealed class HomeOneTimeEvent {
    data class Navigate(val event: HomeEvent)                                          : HomeOneTimeEvent()
    data class NavigateToReconnect(val roomName: String, val peerAddress: String)      : HomeOneTimeEvent()
    data class NavigateToCreate(val roomName: String? = null)                          : HomeOneTimeEvent()
    data class ShowError(val message: String)                                          : HomeOneTimeEvent()
}