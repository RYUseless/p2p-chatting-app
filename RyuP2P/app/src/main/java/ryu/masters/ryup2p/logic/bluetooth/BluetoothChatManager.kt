package ryu.masters.ryup2p.logic.bluetooth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Message(
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BluetoothChatManager {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    fun addMessage(sender: String, content: String) {
        _messages.value = _messages.value + Message(sender, content)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
