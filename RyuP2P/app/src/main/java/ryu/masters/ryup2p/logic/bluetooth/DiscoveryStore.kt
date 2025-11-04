package ryu.masters.ryup2p.logic.bluetooth

import android.bluetooth.BluetoothDevice

data class DiscoveryStore(
    val paired: Set<BluetoothDevice> = emptySet(),
    val discovered: Set<BluetoothDevice> = emptySet(),
    val scanning: Boolean = false
)
