package ryu.masters_thesis.presentation.connect.implementation

import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel

// Immutable snapshot – jediný zdroj pravdy pro ConnectContent
data class ConnectState(
    val scannedDevices: List<ScannedDeviceUiModel> = emptyList(),
    val isConnected: Boolean = false,
    val isVerified: Boolean = false,
    val isSearching: Boolean = false,
    val needsPassword: Boolean = false,
    val passwordError: String? = null,
    val selectedDevice: ScannedDeviceUiModel? = null,
    val remainingSeconds: Int = 30,
)