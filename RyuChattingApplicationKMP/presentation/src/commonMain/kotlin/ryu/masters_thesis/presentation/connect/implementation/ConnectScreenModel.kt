package ryu.masters_thesis.presentation.connect.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository

class ConnectScreenModel(
    private val repository: ConnectRepository,
) : ScreenModel {

    // Stav UI – StateFlow, ConnectContent collectuje přes collectAsState()
    private val _state = MutableStateFlow(ConnectState())
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    // Jednorázové eventy – SharedFlow, UI poslouchá přes LaunchedEffect
    private val _oneTimeEvents = MutableSharedFlow<ConnectOneTimeEvent>()
    val oneTimeEvents: SharedFlow<ConnectOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        observeBluetoothState()
        startCountdown()
    }

    // Jediný vstupní bod pro UI akce
    fun onEvent(event: ConnectEvent) {
        when (event) {
            is ConnectEvent.DeviceClicked     -> selectDevice(event.device)
            is ConnectEvent.PasswordSubmitted -> submitPassword(event.password)
            is ConnectEvent.QrScanned         -> onQrScanned(event.value)
            is ConnectEvent.DialogDismissed   -> _state.update { it.copy(selectedDevice = null) }
            is ConnectEvent.DismissClicked    -> {
                repository.unregisterReceiver()
                screenModelScope.launch {
                    _oneTimeEvents.emit(ConnectOneTimeEvent.Dismiss)
                }
            }
        }
    }

    private fun observeBluetoothState() {
        screenModelScope.launch {
            combine(
                repository.getScannedDevices(),
                repository.getIsConnected(),
                repository.getIsVerified(),
                repository.getIsSearching(),
                repository.getCurrentRoomId(),
            ) { devices, connected, verified, searching, roomId ->
                // Navigace do chatu po úspěšném ověření
                if (verified && roomId != null) {
                    _oneTimeEvents.emit(ConnectOneTimeEvent.NavigateToChat(roomId))
                }
                _state.value.copy(
                    scannedDevices = devices,
                    isConnected = connected,
                    isVerified = verified,
                    isSearching = searching,
                )
            }.collect { newState ->
                _state.value = newState
            }
        }

        screenModelScope.launch {
            repository.getNeedsPassword().collect { needs ->
                _state.update { it.copy(needsPassword = needs) }
            }
        }

        screenModelScope.launch {
            repository.getPasswordError().collect { error ->
                _state.update { it.copy(passwordError = error) }
            }
        }
    }

    private fun startCountdown() {
        screenModelScope.launch {
            // TODO DUMMY: 30s hardcoded, nahradit config hodnotou až bude dostupná
            for (i in 30 downTo 0) {
                _state.update { it.copy(remainingSeconds = i) }
                delay(1000)
            }
        }
    }

    private fun selectDevice(device: ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel) {
        _state.update { it.copy(selectedDevice = device) }
        screenModelScope.launch {
            repository.connectToDevice(device)
        }
    }

    private fun submitPassword(password: String) {
        screenModelScope.launch {
            repository.submitPassword(password)
        }
    }

    private fun onQrScanned(value: String) {
        // QR hodnota se použije jako password
        submitPassword(value)
    }
}