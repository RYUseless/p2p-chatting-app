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
//nove importy
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.BNP_CHANNEL_ID
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol
//logísci, KMP perhaps
import co.touchlab.kermit.Logger

class ConnectScreenModel(
    private val repository        : ConnectRepository,
    private val neighbourProtocol : NeighbourProtocol,
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
            is ConnectEvent.MeshPeerClicked   -> onMeshPeerClicked(event.address, event.name)
            is ConnectEvent.DismissClicked    -> {
                repository.unregisterReceiver()
                neighbourProtocol.stopDiscovery()
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
        //new sekce:
        screenModelScope.launch {
            neighbourProtocol.visibleNodes.collect { list ->
                _state.update { it.copy(meshNodes = list.neighbours) }
            }
        }
        screenModelScope.launch {
            neighbourProtocol.routingTable.collect { table ->
                _state.update { it.copy(meshRoutes = table) }
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
        neighbourProtocol.startDiscovery()   // ← přidat
        _state.update { it.copy(selectedDevice = null, needsPassword = false) }
        screenModelScope.launch {
            repository.startClientMode()
        }
        startCountdown()
    }
    //nova funkce
    private fun onMeshPeerClicked(address: String, name: String?) {
        val isDirectNeighbour = _state.value.meshNodes
            .any { it.neighbourBluetoothAddress == address && it.isNeighbourAlive }

        if (isDirectNeighbour) {
            val device = ScannedDeviceUiModel(address = address, name = name, roomId = null)
            selectDevice(device)
        } else {
            val path = _state.value.meshRoutes.getBestPath(address)
            Logger.d("BNP") { "openTunnel to=$address via ${path?.hops?.map { it.hopBluetoothAddress }}" }
            neighbourProtocol.openTunnel(address, BNP_CHANNEL_ID)
            screenModelScope.launch {
                _oneTimeEvents.emit(
                    ConnectOneTimeEvent.ShowRelayInfo(
                        destinationAddress = address,
                        name               = name,
                        hopCount           = path?.totalHopCount ?: -1,
                    )
                )
            }
        }
    }
}