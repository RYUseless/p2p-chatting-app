package ryu.masters_thesis.presentation.chatroom.domain

// UI model pro zprávu v chatu
data class ChatMessage(
    val id: String,
    val text: String,
    val time: String,
    val isMe: Boolean,
    val senderName: String = "",
)