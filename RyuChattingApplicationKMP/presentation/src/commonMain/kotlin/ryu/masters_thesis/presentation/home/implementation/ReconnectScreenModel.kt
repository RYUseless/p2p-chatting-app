package ryu.masters_thesis.presentation.home.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.data.vault.domain.RoomRole
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel
import ryu.masters_thesis.presentation.home.domain.ReconnectOneTimeEvent
import ryu.masters_thesis.presentation.home.domain.ScanCoordinator
import ryu.masters_thesis.presentation.home.domain.ScanOwner

//TODO: DELETE IN THE NEXT COMMIT

class ReconnectScreenModel(
    private val room             : RoomSummary,
    private val connectRepository: ConnectRepository,
    private val scanCoordinator  : ScanCoordinator,
) : ScreenModel {

    private val _state = MutableStateFlow<ReconnectState>(ReconnectState.Connecting)
    val state: StateFlow<ReconnectState> = _state.asStateFlow()

    private val _oneTimeEvents = MutableSharedFlow<ReconnectOneTimeEvent>()
    val oneTimeEvents: SharedFlow<ReconnectOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        startReconnect()
    }

    private fun startReconnect() {
        screenModelScope.launch {
            scanCoordinator.acquire(ScanOwner.RECONNECT) {
                _state.value = ReconnectState.Failed
            }

            when (room.role) {
                RoomRole.SERVER -> {
                    // SERVER znovu vytvoří místnost – přesměrujeme na CreateScreen prefilled
                    scanCoordinator.release(ScanOwner.RECONNECT)
                    _oneTimeEvents.emit(ReconnectOneTimeEvent.NavigateToCreate(room))
                }
                RoomRole.CLIENT -> {
                    val address = room.peerBluetoothAddress
                    if (address == null) {
                        scanCoordinator.release(ScanOwner.RECONNECT)
                        _oneTimeEvents.emit(ReconnectOneTimeEvent.ShowError("Adresa peera není uložena"))
                        return@launch
                    }
                    connectRepository.connectToDevice(
                        ScannedDeviceUiModel(address = address, name = null, roomId = room.hashedRoomId)
                    )
                    observeConnection()
                }
            }
        }
    }

    private fun observeConnection() {
        screenModelScope.launch {
            connectRepository.getConnectionState().collect { state ->
                if (state == ConnectionState.DISCONNECTED) {
                    scanCoordinator.release(ScanOwner.RECONNECT)
                    _state.value = ReconnectState.Failed
                    _oneTimeEvents.emit(ReconnectOneTimeEvent.ShowError("Nepodařilo se připojit"))
                }
            }
        }
        screenModelScope.launch {
            combine(
                connectRepository.getIsVerified(),
                connectRepository.getCurrentRoomId(),
                connectRepository.getPassword(),
            ) { verified, roomId, password ->
                if (verified && roomId != null && password != null) {
                    scanCoordinator.release(ScanOwner.RECONNECT)
                    _oneTimeEvents.emit(ReconnectOneTimeEvent.NavigateToChat(roomId, password))
                }
            }.collect()
        }
    }

    override fun onDispose() {
        scanCoordinator.release(ScanOwner.RECONNECT)
    }
}