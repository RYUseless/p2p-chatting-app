package ryu.masters_thesis.presentation.home.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.domain.HomeOneTimeEvent
import ryu.masters_thesis.presentation.home.domain.HomeRepository

class HomeScreenModel(
    private val repository: HomeRepository,
) : ScreenModel {

    // Stav UI – StateFlow, HomeContent collectuje přes collectAsState()
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Jednorázové eventy – SharedFlow, UI poslouchá přes LaunchedEffect
    private val _oneTimeEvents = MutableSharedFlow<HomeOneTimeEvent>()
    val oneTimeEvents: SharedFlow<HomeOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        loadChatRooms()
    }

    // Jediný vstupní bod pro UI akce
    fun onEvent(event: HomeEvent) {
        when (event) {
            // Navigace jde přes HomeScreen – ten drží referenci na Voyager Navigator
            is HomeEvent.ConnectClicked  -> emitNavigation(event)
            is HomeEvent.CreateClicked   -> emitNavigation(event)
            is HomeEvent.SettingsClicked -> emitNavigation(event)
            is HomeEvent.RoomClicked     -> {
                if (!event.room.isActive) {
                    emitError("Místnost ${event.room.name} je offline.")
                    return
                }
                emitNavigation(event)
            }
        }
    }

    private fun emitNavigation(event: HomeEvent) {
        screenModelScope.launch {
            // Navigační eventy jdou jako OneTimeEvent – HomeScreen je konzumuje
            _oneTimeEvents.emit(HomeOneTimeEvent.Navigate(event))
        }
    }

    private fun loadChatRooms() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getChatRooms()
                .catch { e -> emitError(e.message ?: "Neznámá chyba") }
                .collect { rooms ->
                    _state.update { it.copy(chatRooms = rooms, isLoading = false) }
                }
        }
    }

    private fun emitError(message: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = false) }
            _oneTimeEvents.emit(HomeOneTimeEvent.ShowError(message))
        }
    }
}