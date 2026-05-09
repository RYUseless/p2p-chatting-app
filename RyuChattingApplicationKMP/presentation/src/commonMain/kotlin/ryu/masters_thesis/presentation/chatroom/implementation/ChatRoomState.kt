package ryu.masters_thesis.presentation.chatroom.implementation

import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage

data class ChatRoomState(
    val roomName:         String              = "",
    val roomPassword:     String              = "",
    val messages:         List<ChatMessage>   = emptyList(),
    val messageInput:     String              = "",
    val isConnected:      Boolean             = false,
    val isVerified:       Boolean             = false,
    val showEmojiMenu:    Boolean             = false,
    val showInfoSheet:    Boolean             = false,
    val showQrDialog:     Boolean             = false,
    val chatColorHex:     String              = "#9E9E9E",
    val nicknames:        Map<String, String> = emptyMap(),
    val blacklist:        List<String>        = emptyList(),
    val currentRoomId:    String?             = null,
    val isSaved:          Boolean             = false,
    val isServer:         Boolean             = false,
    val connectedUserIds: List<String>        = emptyList(),
    val isLeaving: Boolean = false,
)