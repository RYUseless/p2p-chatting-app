package ryu.masters_thesis.presentation.chatroom.domain

// Všechny akce které může uživatel na ChatRoomScreen provést
sealed class ChatRoomEvent {
    // Bottom bar
    data class MessageInputChanged(val text: String) : ChatRoomEvent()
    object SendMessageClicked : ChatRoomEvent()
    object EmojiMenuToggled : ChatRoomEvent()
    data class EmojiSelected(val emoji: String) : ChatRoomEvent()
    object AttachFileClicked : ChatRoomEvent()

    // Top bar
    object BackClicked : ChatRoomEvent()
    object InfoClicked : ChatRoomEvent()
    object SearchClicked : ChatRoomEvent()

    // Info sheet
    object InfoSheetDismissed : ChatRoomEvent()
    object ShowQrClicked : ChatRoomEvent()
    object QrDialogDismissed : ChatRoomEvent()
    data class ChatColorChanged(val colorHex: String) : ChatRoomEvent()
    data class NicknameChanged(val userId: String, val nickname: String) : ChatRoomEvent()
    data class WhitelistToggled(val userId: String) : ChatRoomEvent()
}