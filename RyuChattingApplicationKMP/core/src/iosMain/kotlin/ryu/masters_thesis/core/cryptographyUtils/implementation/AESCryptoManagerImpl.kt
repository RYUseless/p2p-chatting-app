package ryu.masters_thesis.core.cryptographyUtils.implementation

import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager

/*
TODO: implement in the future if I ever have any access on ios device
aka absolute tood freestyle, not sure if this even is correct :)
*/
class AESCryptoManagerImpl(
    private val roomId:     String,
    private val keyManager: KeyManager,
) : CryptoManager {
    override var verifier: String?    = null
    override var witness:  ByteArray? = null
    override fun initializeAsServer(password: String): String          = TODO("iOS: CommonCrypto")
    override fun initializeAsClient(keyExchangeData: String, password: String): Boolean = TODO("iOS: CommonCrypto")
    override fun encrypt(text: String): String?                        = TODO("iOS: CommonCrypto")
    override fun decrypt(encryptedText: String): String?               = TODO("iOS: CommonCrypto")
}