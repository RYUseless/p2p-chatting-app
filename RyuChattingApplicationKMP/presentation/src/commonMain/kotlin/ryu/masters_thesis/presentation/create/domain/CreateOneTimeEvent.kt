package ryu.masters_thesis.presentation.create.domain

// Jednorázové efekty – nezapisují se do CreateState
sealed class CreateOneTimeEvent {
    data class NavigateToChat(val roomId: String) : CreateOneTimeEvent()
    data class ShowError(val message: String) : CreateOneTimeEvent()
    object Dismiss : CreateOneTimeEvent()
}