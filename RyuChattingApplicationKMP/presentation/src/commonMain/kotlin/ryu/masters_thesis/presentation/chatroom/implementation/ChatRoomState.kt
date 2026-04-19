package ryu.masters_thesis.presentation.chatroom.implementation

import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage

// Immutable snapshot – jediný zdroj pravdy pro ChatRoomContent
data class ChatRoomState(
    val roomName: String = "",
    val roomPassword: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val isConnected: Boolean = false,
    val isVerified: Boolean = false,
    val showEmojiMenu: Boolean = false,
    val showInfoSheet: Boolean = false,
    val showQrDialog: Boolean = false,
    // TODO DUMMY: chatColorHex z AppSettings/Theme až bude dostupné
    val chatColorHex: String = "#9E9E9E",
    // userId -> nickname
    val nicknames: Map<String, String> = emptyMap(),
    // whitelisted userIds
    val whitelist: List<String> = emptyList(),
    val currentRoomId: String? = null,
)