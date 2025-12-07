package ryu.masters.ryup2p.logic.cryptUtils

import android.content.Context
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesKeyManager {
    private lateinit var secretKey: SecretKey

    // AES 256 key generation
    fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256) // You can also use 128 or 192 bits
        return keyGenerator.generateKey()
    }
    // IV for AES
    fun generateIV(): IvParameterSpec {
        val iv = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        return IvParameterSpec(iv)
    }

    fun deriveKeyFromPassword(password: String): SecretKey {
        val secretKey =
            SecretKeySpec("TO BE IMPLEMENTED".toByteArray(), "AES")
        return secretKey
    }
    fun saveKey(context: Context) { }

    fun loadKey(context: Context): SecretKey {
        val secretKey = SecretKeySpec("TO BE IMPLEMENTED".toByteArray(), "AES")
        return secretKey
    }



}