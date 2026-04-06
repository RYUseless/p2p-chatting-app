package ryu.masters_thesis.ryus_chatting_application.logic.Messages

data class Message(
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)