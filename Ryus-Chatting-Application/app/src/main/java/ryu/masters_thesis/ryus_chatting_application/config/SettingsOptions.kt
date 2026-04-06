package ryu.masters_thesis.ryus_chatting_application.config
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

enum class AppLanguage(val displayName: String) {
    ENGLISH("English"),
    CZECH("Čeština"),
    GERMAN("Deutsch"),
    POLISH("Polski")
}

enum class AppTheme(val displayName: String) {
    SYSTEM("System"),   // ddefault by system
    DARK("Dark"),
    LIGHT("Light"),
}

enum class CryptoAlgorithm(val displayName: String) {
    AES("AES"),
    GAY_AES("GAY-AES"),
    AES_256("AES-256")
}

enum class BluetoothIdentity(val displayName: String) {
    MAC_ADDRESS("MAC Address"),
    DEVICE_NAME("Device Name"),
    RANDOM("Randomized")
}

data class AppSettings(
    val language:            AppLanguage       = AppLanguage.ENGLISH,
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