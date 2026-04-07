package ryu.masters_thesis.ryus_chatting_application.logic.Messages


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class BluetoothChatManager {
    //Message → MessagesData.kt dataclass
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    fun addMessage(sender: String, content: String) {
        _messages.value += Message(sender, content)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}