package ryu.masters_thesis.presentation.chatroom.domain

sealed class ChatRoomEvent {
    data class MessageInputChanged(val text: String)                       : ChatRoomEvent()
    object SendMessageClicked                                              : ChatRoomEvent()
    object EmojiMenuToggled                                                : ChatRoomEvent()
    data class EmojiSelected(val emoji: String)                            : ChatRoomEvent()
    object AttachFileClicked                                               : ChatRoomEvent()
    object BackClicked                                                     : ChatRoomEvent()
    object InfoClicked                                                     : ChatRoomEvent()
    object SearchClicked                                                   : ChatRoomEvent()
    object InfoSheetDismissed                                              : ChatRoomEvent()
    object ShowQrClicked                                                   : ChatRoomEvent()
    object QrDialogDismissed                                               : ChatRoomEvent()
    data class ChatColorChanged(val colorHex: String)                      : ChatRoomEvent()
    data class NicknameChanged(val userId: String, val nickname: String)   : ChatRoomEvent()
    data class BlacklistToggled(val userId: String)                        : ChatRoomEvent()
    object MarkRoomSaved                                                   : ChatRoomEvent()
}