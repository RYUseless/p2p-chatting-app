package ryu.masters_thesis.presentation.connect.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

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
    }

    // Jediný vstupní bod pro UI akce
    fun onEvent(event: ConnectEvent) {
        when (event) {
            is ConnectEvent.DeviceClicked     -> selectDevice(event.device)
            is ConnectEvent.PasswordSubmitted -> submitPassword(event.password)
            is ConnectEvent.QrScanned         -> onQrScanned(event.value)
            is ConnectEvent.DialogDismissed   -> _state.update { it.copy(selectedDevice = null) }
            is ConnectEvent.ReconnectClicked  -> screenModelScope.launch { repository.reconnect() }
            is ConnectEvent.DismissClicked    -> {
                repository.unregisterReceiver()
                screenModelScope.launch {
                    _oneTimeEvents.emit(ConnectOneTimeEvent.Dismiss)
                }
            }
        }
    }

    private fun observeBluetoothState() {
        // Hlavní state combine
        screenModelScope.launch {
            combine(
                repository.getScannedDevices(),
                repository.getIsConnected(),
                repository.getIsVerified(),
                repository.getIsSearching(),
                repository.getConnectionState(),
                repository.getCanReconnect(),
                repository.getSessionDevice(),
            ) { values ->
                ConnectState(
                    //TODO: vyresit unchecked resolve
                    scannedDevices   = values[0] as List<ScannedDeviceUiModel>,
                    isConnected      = values[1] as Boolean,
                    isVerified       = values[2] as Boolean,
                    isSearching      = values[3] as Boolean,
                    connectionState  = values[4] as ConnectionState,
                    canReconnect     = values[5] as Boolean,
                    sessionDevice    = values[6] as ScannedDeviceUiModel?,
                    needsPassword    = _state.value.needsPassword,
                    passwordError    = _state.value.passwordError,
                    selectedDevice   = _state.value.selectedDevice,
                    remainingSeconds = _state.value.remainingSeconds,
                )
            }.collect { _state.value = it }
        }

        // Navigation trigger po úspěšném HANDSHAKE
        screenModelScope.launch {
            combine(
                repository.getIsVerified(),
                repository.getCurrentRoomId(),
                repository.getPassword(),
            ) { verified, roomId, password ->
                if (verified && roomId != null && password != null) {
                    _oneTimeEvents.emit(ConnectOneTimeEvent.NavigateToChat(roomId, password))
                }
            }.collect()
        }

        // needsPassword → otevřít dialog
        // při reconnectu použijeme sessionDevice jako selectedDevice
        screenModelScope.launch {
            combine(
                repository.getNeedsPassword(),
                repository.getSessionDevice(),
            ) { needsPassword, sessionDevice ->
                if (needsPassword) {
                    val target = _state.value.selectedDevice ?: sessionDevice
                    if (target != null) {
                        _state.update { it.copy(
                            needsPassword  = true,
                            selectedDevice = target,
                        ) }
                    }
                } else {
                    _state.update { it.copy(needsPassword = false) }
                }
            }.collect()
        }

        screenModelScope.launch {
            repository.getPasswordError().collect { error ->
                _state.update { it.copy(passwordError = error) }
            }
        }

        screenModelScope.launch {
            repository.getConnectionError().collect { error ->
                if (error != null) {
                    repository.clearConnectionError()
                    _oneTimeEvents.emit(ConnectOneTimeEvent.ShowError(error))
                }
            }
        }

        screenModelScope.launch {
            repository.getConnectionState().collect { state ->
                if (state == ConnectionState.DISCONNECTED) {
                    _oneTimeEvents.emit(ConnectOneTimeEvent.Disconnected)
                }
            }
        }
    }

    private fun startCountdown() {
        screenModelScope.launch {
            val timeoutSeconds = (BluetoothConstants.DISCOVERY_TIMEOUT_MS / 1000).toInt()
            for (i in timeoutSeconds downTo 0) {
                _state.update { it.copy(remainingSeconds = i) }
                delay(1000)
            }
        }
    }

    private fun selectDevice(device: ScannedDeviceUiModel) {
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
        val parts    = value.split("|", limit = 2)
        val password = if (parts.size == 2) parts[1] else value
        submitPassword(password)
    }

    fun restartScanning() {
        _state.update { it.copy(selectedDevice = null, needsPassword = false) }
        screenModelScope.launch {
            repository.startClientMode()
        }
        startCountdown()
    }
}