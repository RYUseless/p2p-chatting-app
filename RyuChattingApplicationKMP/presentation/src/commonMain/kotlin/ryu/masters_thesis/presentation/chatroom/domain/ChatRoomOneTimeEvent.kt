package ryu.masters_thesis.presentation.chatroom.domain

// Jednorázové efekty – nezapisují se do ChatRoomState
sealed class ChatRoomOneTimeEvent {
    object NavigateBack : ChatRoomOneTimeEvent()
    data class ShowError(val message: String) : ChatRoomOneTimeEvent()
    // TODO DUMMY: až bude file picker dostupný z :core
    object OpenFilePicker : ChatRoomOneTimeEvent()
}