package ryu.masters_thesis.presentation.connect.domain

// Jednorázové efekty – nezapisují se do ConnectState
// UI je konzumuje přes LaunchedEffect na SharedFlow
sealed class ConnectOneTimeEvent {
    //data class NavigateToChat(val roomId: String) : ConnectOneTimeEvent()
    data class ShowError(val message: String) : ConnectOneTimeEvent()
    object Dismiss : ConnectOneTimeEvent()

    data class NavigateToChat(val roomId: String, val password: String) : ConnectOneTimeEvent()

    object Disconnected : ConnectOneTimeEvent()
}