package ryu.masters_thesis.core.configuration

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

data class AppSettings(
    val language:            AppLanguage       = detectDefaultLanguage(),
    val theme:               AppTheme          = AppTheme.SYSTEM,
    val postQuantumReady:    Boolean           = false,
    val cryptoAlgorithm:     CryptoAlgorithm   = CryptoAlgorithm.AES_256,
    val bluetoothIdentity:   BluetoothIdentity = BluetoothIdentity.MAC_ADDRESS,
    val bluetoothDeviceName: String            = "P2P-Device",
    val connectionTimeout:   Int               = 30,
    val maxPeers:            Int               = 8,
)

@Composable
fun AppSettings.isDarkTheme(): Boolean = when (theme) {
    AppTheme.SYSTEM -> isSystemInDarkTheme()
    AppTheme.DARK   -> true
    AppTheme.LIGHT  -> false
}

val DefaultSettings = AppSettings()