package ryu.masters_thesis.presentation.connect.domain

// Všechny akce které může uživatel na ConnectScreen provést
sealed class ConnectEvent {
    data class DeviceClicked(val device: ScannedDeviceUiModel) : ConnectEvent()
    data class PasswordSubmitted(val password: String) : ConnectEvent()
    data class QrScanned(val value: String) : ConnectEvent()
    object DialogDismissed : ConnectEvent()
    object DismissClicked : ConnectEvent()

    object ReconnectClicked : ConnectEvent()
}