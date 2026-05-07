package ryu.masters_thesis.data.vault.domain

data class MessageEntry(
    val id:        Long,
    val sender:    String,
    val content:   String,
    val timestamp: Long,
)