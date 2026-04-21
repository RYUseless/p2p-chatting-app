package ryu.masters_thesis.feature.bluetooth.domain

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    FAILED,
}