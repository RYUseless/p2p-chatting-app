package ryu.masters_thesis.presentation.home.domain

import ryu.masters_thesis.data.vault.domain.RoomSummary
//TODO: DELETE IN THE NEXT COMMIT


sealed class ReconnectOneTimeEvent {
    data class NavigateToChat(val roomId: String, val password: String) : ReconnectOneTimeEvent()
    data class NavigateToCreate(val room: RoomSummary)                  : ReconnectOneTimeEvent()
    data class ShowError(val message: String)                           : ReconnectOneTimeEvent()
}