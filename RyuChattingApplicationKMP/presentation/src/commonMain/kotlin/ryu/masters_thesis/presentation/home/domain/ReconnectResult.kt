package ryu.masters_thesis.presentation.home.domain

sealed class ReconnectResult {
    data class HostFound(val peerAddress: String) : ReconnectResult()
    object BecomeServer                           : ReconnectResult()
}