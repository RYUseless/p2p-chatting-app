package ryu.masters_thesis.ryus_chatting_application.logic.bluetooth

data class BluetoothDevice(
    val name: String?,
    val address: String,
    val roomId: String? = null
)