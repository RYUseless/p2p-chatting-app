package ryu.masters_thesis.feature.bluetooth.domain

sealed class HandoffData {
    data class PromoteToServer(val roomId: String) : HandoffData()
    data class SearchNewServer(val roomId: String) : HandoffData()
}