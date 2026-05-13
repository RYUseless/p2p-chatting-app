package ryu.masters_thesis.core.cryptographyUtils.implementation

internal object CryptoConstants {
    const val AES_KEY_SIZE        = 256
    const val IV_SIZE             = 16
    const val SALT_SIZE           = 16
    const val PBKDF2_ITERATIONS   = 50_000
    const val AES_ALGORITHM       = "AES"
    const val AES_CIPHER          = "AES/GCM/NoPadding"
    const val PBKDF2_ALGORITHM    = "PBKDF2WithHmacSHA256"
    const val GCM_NONCE_SIZE      = 12
    const val GCM_TAG_SIZE        = 128
}