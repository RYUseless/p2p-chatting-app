package ryu.masters.ryup2p.logic.cryptUtils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec


class MessageCryptoUtils(
    // zde se pokaždé generuje klíč, potřeba změnit na check, if key exists
    private val secretKey: SecretKey = AesKeyManager.generateSecretKey(),
    private val iv: IvParameterSpec = AesKeyManager.generateIV()
){

    private fun encryptLogic(plainText: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)

        val encryptedBytes = cipher.doFinal(plainText)
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    // public
    fun encrypt(textToEncrypt: String): String {
        return encryptLogic(textToEncrypt.toByteArray())
    }

    private fun decryptInternal(encryptedText: String): String {
        val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }

    // Public API for decryption
    fun decrypt(encryptedText: String): String {
        return decryptInternal(encryptedText)
    }

    private fun exchange_keys(): Void? {
        // placeholder and shit
        return null
    }
}