package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AesKeyManagerImpl(private val context: Context) : KeyManager {

    private val prefs by lazy {
        context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun generateSecretKey(): ByteArray {
        val keyGenerator = KeyGenerator.getInstance(CryptoConstants.AES_ALGORITHM)
        keyGenerator.init(CryptoConstants.AES_KEY_SIZE)
        return keyGenerator.generateKey().encoded
    }

    override fun generateIV(): ByteArray {
        val iv = ByteArray(CryptoConstants.IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
    }

    override fun generateSalt(): ByteArray {
        val salt = ByteArray(CryptoConstants.SALT_SIZE)
        SecureRandom().nextBytes(salt)
        return salt
    }

    override fun saveRoomAesKey(roomId: String, aesKey: ByteArray, iv: ByteArray) {
        prefs.edit().apply {
            putString("${ROOM_AES_KEY_PREF}_$roomId", Base64.encodeToString(aesKey, Base64.NO_WRAP))
            putString("${ROOM_IV_PREF}_$roomId",      Base64.encodeToString(iv, Base64.NO_WRAP))
            apply()
        }
    }

    override fun loadRoomAesKey(roomId: String): Pair<ByteArray, ByteArray>? {
        val keyString = prefs.getString("${ROOM_AES_KEY_PREF}_$roomId", null) ?: return null
        val ivString  = prefs.getString("${ROOM_IV_PREF}_$roomId",      null) ?: return null
        return Pair(
            Base64.decode(keyString, Base64.NO_WRAP),
            Base64.decode(ivString,  Base64.NO_WRAP),
        )
    }

    override fun saveSalt(roomId: String, salt: ByteArray) {
        prefs.edit()
            .putString("${SALT_PREF}_$roomId", Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
    }

    override fun loadSalt(roomId: String): ByteArray? {
        val saltString = prefs.getString("${SALT_PREF}_$roomId", null) ?: return null
        return Base64.decode(saltString, Base64.NO_WRAP)
    }

    override fun encryptAesKeyWithPassword(aesKey: ByteArray, password: String, salt: ByteArray): String {
        Log.d(TAG, "encryptAesKeyWithPassword: key=${aesKey.size}B pwd=${password.length}ch")
        val derivedKey = deriveKeyFromPassword(password.trim(), salt)
        val iv         = generateIV()
        val cipher     = Cipher.getInstance(CryptoConstants.AES_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, IvParameterSpec(iv))
        val encryptedKeyBytes = cipher.doFinal(aesKey)
        val combined = iv + encryptedKeyBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decryptAesKeyWithPassword(encryptedKeyData: String, password: String, salt: ByteArray): ByteArray {
        Log.d(TAG, "decryptAesKeyWithPassword: data=${encryptedKeyData.length}ch pwd=${password.length}ch")
        val combined = Base64.decode(encryptedKeyData, Base64.NO_WRAP)
        require(combined.size >= CryptoConstants.IV_SIZE) {
            "Invalid encrypted data: ${combined.size}B"
        }
        val iv           = combined.copyOfRange(0, CryptoConstants.IV_SIZE)
        val encryptedKey = combined.copyOfRange(CryptoConstants.IV_SIZE, combined.size)
        val derivedKey   = deriveKeyFromPassword(password.trim(), salt)
        val cipher       = Cipher.getInstance(CryptoConstants.AES_CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, IvParameterSpec(iv))
        return cipher.doFinal(encryptedKey)
    }

    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec    = PBEKeySpec(password.toCharArray(), salt, CryptoConstants.PBKDF2_ITERATIONS, CryptoConstants.AES_KEY_SIZE)
        val factory = SecretKeyFactory.getInstance(CryptoConstants.PBKDF2_ALGORITHM)
        return SecretKeySpec(factory.generateSecret(spec).encoded, CryptoConstants.AES_ALGORITHM)
    }

    companion object {
        private const val TAG               = "AesKeyManagerImpl"
        private const val SHARED_PREFS_NAME = "encrypted_keys"
        private const val SALT_PREF         = "pbkdf2_salt"
        private const val ROOM_AES_KEY_PREF = "room_aes_key"
        private const val ROOM_IV_PREF      = "room_iv"
    }
}