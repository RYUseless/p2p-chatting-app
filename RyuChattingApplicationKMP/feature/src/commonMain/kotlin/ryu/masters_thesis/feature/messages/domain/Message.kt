package ryu.masters_thesis.feature.messages.domain

data class Message(
    val sender:    String,
    val content:   String,
    val timestamp: Long = 0L,
)