package ryu.masters_thesis.presentation.connect.implementation

import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

// src pro connect content
data class ConnectState(
    val scannedDevices:  List<ScannedDeviceUiModel> = emptyList(),
    val isConnected:     Boolean                    = false,
    val isVerified:      Boolean                    = false,
    val isSearching:     Boolean                    = false,
    val needsPassword:   Boolean                    = false,
    val passwordError:   String?                    = null,
    val selectedDevice:  ScannedDeviceUiModel?      = null,
    val remainingSeconds: Int                       = 30,
    val connectionState: ConnectionState = ConnectionState.IDLE,
    val canReconnect:    Boolean                    = false,
)