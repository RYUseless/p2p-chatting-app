package ryu.masters_thesis.core.configuration

enum class AppLanguage(val displayName: String) {
    ENGLISH("English"),
    CZECH("Čeština"),
    GERMAN("Deutsch"),
    POLISH("Polski")
}

enum class AppTheme(val displayName: String) {
    SYSTEM("System"),
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