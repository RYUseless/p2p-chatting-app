package ryu.masters_thesis.feature.bluetooth.domain

object BluetoothConstants {
    const val APP_IDENTIFIER              = "RyuAPP:"
    const val UUID_STRING                 = "f6c44fea-9edb-5e04-a1b1-e733b049a26e"
    const val TAG_CONTROLLER              = "BTController"
    const val TAG_CONNECTION              = "BluetoothConnMgr"
    const val DISCOVERY_TIMEOUT_MS        = 30_000L
    const val SERVER_TIMEOUT_MS           = 300_000L

    const val MSG_KEY_EXCHANGE            = "KEY_EXCHANGE"
    const val MSG_HANDSHAKE               = "HANDSHAKE"
    const val MSG_DATA                    = "MSG"

    const val HANDSHAKE_CLIENT_READY      = "CLIENT_READY"
    const val HANDSHAKE_CONFIRMED         = "CONFIRMED"
}