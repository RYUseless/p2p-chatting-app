package ryu.masters_thesis.feature.bluetooth.domain

object BluetoothConstants {
    const val APP_IDENTIFIER             = "Ryuovo_Appec"
    const val UUID_STRING                = "f6c44fea-9edb-5e04-a1b1-e733b049a26e"
    const val DISCOVERY_TIMEOUT_MS       = 30_000L
    const val SERVER_TIMEOUT_MS          = 300_000L

    const val MSG_KEY_EXCHANGE           = "KEY_EXCHANGE"
    const val MSG_HANDSHAKE              = "HANDSHAKE"
    const val MSG_DATA                   = "MSG"
    const val MSG_DISCONNECT             = "DISCONNECT"

    const val HANDSHAKE_CLIENT_READY     = "CLIENT_READY"
    const val HANDSHAKE_CONFIRMED        = "CONFIRMED"
    const val DISCONNECT_SERVER_CLOSED   = "SERVER_CLOSED"

    const val TAG_BASE                   = "BT/Base"
    const val TAG_SERVER                 = "BT/Server"
    const val TAG_CLIENT                 = "BT/Client"
    const val TAG_CONNECTION             = "BT/Connection"
    const val TAG_CLEANUP                = "BT/Cleanup"

    // value
    const val MSG_ROOM_MEMBERS = "ROOM_MEMBERS"
    const val RECONNECT_TIMEOUT_MS = 10_000L
}