package ryu.masters_thesis.presentation.home.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.domain.HomeOneTimeEvent
import ryu.masters_thesis.presentation.home.domain.HomeRepository
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol

import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol

class HomeScreenModel(
    private val repository     : HomeRepository,
    private val finderProtocol : FinderProtocol,
) : ScreenModel {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _oneTimeEvents = MutableSharedFlow<HomeOneTimeEvent>()
    val oneTimeEvents: SharedFlow<HomeOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        loadRooms()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ConnectClicked      -> emitNavigation(event)
            is HomeEvent.CreateClicked       -> emitNavigation(event)
            is HomeEvent.SettingsClicked     -> emitNavigation(event)
            is HomeEvent.DeleteRoomClicked   -> deleteRoom(event.roomId)
            is HomeEvent.RoomClicked -> screenModelScope.launch {
                _state.update { it.copy(isLoading = true) }

                val knownPeers  = listOfNotNull(event.room.peerBluetoothAddress)
                val hostAddress = finderProtocol.findHost(
                    roomId     = event.room.roomName,
                    knownPeers = knownPeers,
                )

                _state.update { it.copy(isLoading = false) }

                if (hostAddress != null) {
                    _oneTimeEvents.emit(
                        HomeOneTimeEvent.NavigateToReconnect(
                            roomName    = event.room.roomName,
                            peerAddress = hostAddress,
                        )
                    )
                } else {
                    _oneTimeEvents.emit(HomeOneTimeEvent.NavigateToCreate(roomName = event.room.roomName))
                }
            }
        }
    }

    fun refresh() {
        loadRooms()
    }

    private fun loadRooms() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.observeRooms()
                .catch { e -> emitError(e.message ?: "Neznámá chyba") }
                .collect { rooms ->
                    _state.update { it.copy(rooms = rooms, isLoading = false) }
                }
        }
    }

    private fun deleteRoom(roomId: String) {
        screenModelScope.launch {
            repository.deleteRoom(roomId)
                .onFailure { emitError("Nepodařilo se smazat místnost") }
        }
    }

    private fun emitNavigation(event: HomeEvent) {
        screenModelScope.launch {
            _oneTimeEvents.emit(HomeOneTimeEvent.Navigate(event))
        }
    }

    private fun emitError(message: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = false) }
            _oneTimeEvents.emit(HomeOneTimeEvent.ShowError(message))
        }
    }
}