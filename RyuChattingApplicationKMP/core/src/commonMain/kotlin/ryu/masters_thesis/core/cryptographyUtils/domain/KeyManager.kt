package ryu.masters_thesis.core.cryptographyUtils.domain

interface KeyManager {
    /** Generuje nový AES-256 klíč jako ByteArray */
    fun generateSecretKey(): ByteArray

    /** Generuje náhodné 16B IV */
    fun generateIV(): ByteArray

    /** Generuje náhodný 16B salt pro PBKDF2 */
    fun generateSalt(): ByteArray

    /** Uloží AES klíč + IV pro danou místnost */
    fun saveRoomAesKey(roomId: String, aesKey: ByteArray, iv: ByteArray)

    /** Načte AES klíč + IV pro danou místnost, nebo null */
    fun loadRoomAesKey(roomId: String): Pair<ByteArray, ByteArray>?

    /** Uloží PBKDF2 salt pro danou místnost */
    fun saveSalt(roomId: String, salt: ByteArray)

    /** Načte PBKDF2 salt pro danou místnost, nebo null */
    fun loadSalt(roomId: String): ByteArray?

    /** Zašifruje AES klíč heslem (pro přenos klientovi) */
    fun encryptAesKeyWithPassword(aesKey: ByteArray, password: String, salt: ByteArray): String

    /** Dešifruje přijatý AES klíč heslem */
    fun decryptAesKeyWithPassword(encryptedKeyData: String, password: String, salt: ByteArray): ByteArray
}