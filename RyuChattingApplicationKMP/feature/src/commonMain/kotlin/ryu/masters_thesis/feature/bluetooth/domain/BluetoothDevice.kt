package ryu.masters_thesis.feature.bluetooth.domain

data class BluetoothDevice(
    val name:    String?,
    val address: String,
    val roomId:  String? = null,
)