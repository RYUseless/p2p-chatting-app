package ryu.masters.ryup2p.logic.cryptUtils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object AesKeyManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ryup2p_master_key"
    private const val SHARED_PREFS_NAME = "encrypted_keys"
    private const val SALT_PREF = "pbkdf2_salt"
    private const val ROOM_AES_KEY_PREF = "room_aes_key"
    private const val ROOM_IV_PREF = "room_iv"

    private const val PBKDF2_ITERATIONS = 100000

    // AES 256 key generation
    fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    // IV for AES
    fun generateIV(): IvParameterSpec {
        val iv = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(iv)
        return IvParameterSpec(iv)
    }

    /**
     * Derive AES-256 key from password using PBKDF2WithHmacSHA256
     */
    private fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Get or create master key in Android KeyStore (hardware-backed)
     */
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Save salt for room-specific PBKDF2 derivation
     */
    fun saveSalt(context: Context, roomId: String, salt: ByteArray) {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("${SALT_PREF}_$roomId", Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
    }

    /**
     * Load salt for room-specific PBKDF2 derivation
     */
    fun loadSalt(context: Context, roomId: String): ByteArray? {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val saltString = prefs.getString("${SALT_PREF}_$roomId", null) ?: return null
        return Base64.decode(saltString, Base64.NO_WRAP)
    }

    /**
     * Save room-specific AES key (raw key bytes) for this room
     */
    fun saveRoomAesKey(context: Context, roomId: String, aesKey: SecretKey, iv: IvParameterSpec) {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("${ROOM_AES_KEY_PREF}_$roomId", Base64.encodeToString(aesKey.encoded, Base64.NO_WRAP))
            putString("${ROOM_IV_PREF}_$roomId", Base64.encodeToString(iv.iv, Base64.NO_WRAP))
            apply()
        }
    }

    /**
     * Load room-specific AES key for decryption
     */
    fun loadRoomAesKey(context: Context, roomId: String): Pair<SecretKey, IvParameterSpec>? {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val keyString = prefs.getString("${ROOM_AES_KEY_PREF}_$roomId", null) ?: return null
        val ivString = prefs.getString("${ROOM_IV_PREF}_$roomId", null) ?: return null

        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivString, Base64.NO_WRAP)

        return Pair(SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    }

    /**
     * Encrypt AES key with password-derived key for transmission
     */
    fun encryptAesKeyWithPassword(aesKey: SecretKey, password: String, salt: ByteArray): String {
        val derivedKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = generateIV()
        cipher.init(Cipher.ENCRYPT_MODE, derivedKey, iv)
        val encryptedKeyBytes = cipher.doFinal(aesKey.encoded)

        // Format: IV (16 bytes) + encrypted key
        val combined = iv.iv + encryptedKeyBytes
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt received AES key with password
     */
    fun decryptAesKeyWithPassword(encryptedKeyData: String, password: String, salt: ByteArray): SecretKey {
        val combined = Base64.decode(encryptedKeyData, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 16)
        val encryptedKey = combined.copyOfRange(16, combined.size)

        val derivedKey = deriveKeyFromPassword(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, derivedKey, IvParameterSpec(iv))
        val decryptedKeyBytes = cipher.doFinal(encryptedKey)

        return SecretKeySpec(decryptedKeyBytes, "AES")
    }
}

