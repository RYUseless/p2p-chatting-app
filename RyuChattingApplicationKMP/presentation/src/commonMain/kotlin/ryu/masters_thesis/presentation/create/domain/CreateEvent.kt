package ryu.masters_thesis.presentation.create.domain

// Všechny akce které může uživatel na CreateScreen provést
sealed class CreateEvent {
    data class RoomNameChanged(val name: String) : CreateEvent()
    data class PasswordChanged(val password: String) : CreateEvent()
    object CreateRoomClicked : CreateEvent()
    object ShowQrClicked : CreateEvent()
    object QrDialogDismissed : CreateEvent()
    object DismissClicked : CreateEvent()
}