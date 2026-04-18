package ryu.masters_thesis.feature.messages.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import ryu.masters_thesis.feature.messages.domain.ChatManager
import ryu.masters_thesis.feature.messages.domain.Message

class ChatManagerImpl : ChatManager {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages

    override fun addMessage(sender: String, content: String) {
        _messages.value += Message(
            sender    = sender,
            content   = content,
            timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong(),
        )
    }

    override fun clearMessages() {
        _messages.value = emptyList()
    }
}