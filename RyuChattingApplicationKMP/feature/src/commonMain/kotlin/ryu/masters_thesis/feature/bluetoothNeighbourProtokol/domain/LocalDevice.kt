package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain

interface LocalDevice {
    fun getDeviceName(): String?
    fun getBluetoothAddress(): String?
    fun isDiscoverable(): Boolean
}