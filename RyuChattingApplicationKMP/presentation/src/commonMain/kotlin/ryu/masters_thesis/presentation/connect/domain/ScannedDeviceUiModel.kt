package ryu.masters_thesis.presentation.connect.domain

// UI model pro naskenované Bluetooth zařízení
// TODO DUMMY: až bude BluetoothDevice z :core dostupný, nahradit mapperem
data class ScannedDeviceUiModel(
    val address: String,
    val name: String?,
    val roomId: String?,
) {
    // Zobrazovaný název – preferuje roomId, pak name, pak address
    val displayName: String get() = roomId ?: name ?: address
}