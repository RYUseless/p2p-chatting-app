package ryu.masters_thesis.feature.messages.domain

import kotlinx.coroutines.flow.StateFlow

interface ChatManager {
    val messages: StateFlow<List<Message>>
    fun addMessage(sender: String, content: String)
    fun clearMessages()
}