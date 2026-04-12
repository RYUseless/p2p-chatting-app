package ryu.masters_thesis.presentation.home.domain

// Jednorázové efekty – nezapisují se do HomeState
// UI je konzumuje přes LaunchedEffect na SharedFlow

sealed class HomeOneTimeEvent {
    data class ShowError(val message: String) : HomeOneTimeEvent()
    // Navigační požadavek – HomeScreen ho konzumuje a předá Voyager Navigatoru
    data class Navigate(val event: HomeEvent) : HomeOneTimeEvent()
}