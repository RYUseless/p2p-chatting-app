package ryu.masters_thesis.presentation.create.implementation

// Immutable snapshot – jediný zdroj pravdy pro CreateContent
data class CreateState(
    val roomName: String = "",
    val password: String = "",
    val passwordError: String? = null,
    val serverStarted: Boolean = false,
    val showQrDialog: Boolean = false,
    val currentRoomId: String? = null,
    // TODO DUMMY: hasPermissions vždy true, až bude BluetoothController z :core nahradit
    val hasPermissions: Boolean = true,
)