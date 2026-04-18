package ryu.masters_thesis.core.cryptographyUtils.implementation

import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
/*
TODO: implement in the future if I ever have any access on ios device
aka absolute tood freestyle, not sure if this even is correct :)
 */
class AesKeyManagerImpl : KeyManager {
    override fun generateSecretKey(): ByteArray                                                       = TODO("iOS: Keychain")
    override fun generateIV(): ByteArray                                                              = TODO("iOS: Keychain")
    override fun generateSalt(): ByteArray                                                            = TODO("iOS: Keychain")
    override fun saveRoomAesKey(roomId: String, aesKey: ByteArray, iv: ByteArray)                    = TODO("iOS: Keychain")
    override fun loadRoomAesKey(roomId: String): Pair<ByteArray, ByteArray>?                         = TODO("iOS: Keychain")
    override fun saveSalt(roomId: String, salt: ByteArray)                                            = TODO("iOS: Keychain")
    override fun loadSalt(roomId: String): ByteArray?                                                 = TODO("iOS: Keychain")
    override fun encryptAesKeyWithPassword(aesKey: ByteArray, password: String, salt: ByteArray): String = TODO("iOS: Keychain")
    override fun decryptAesKeyWithPassword(encryptedKeyData: String, password: String, salt: ByteArray): ByteArray = TODO("iOS: Keychain")
}