package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.content.Context
import android.util.Base64
import android.util.Log
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import ryu.masters_thesis.core.cryptographyUtils.domain.SchnorrProtocol
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AesKeyManagerImpl(
    private val context: Context,
    private val schnorr: SchnorrProtocol
) : KeyManager {

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
            Base64.decode(ivString,  Base64.NO_WRAP)
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

    /*
    override fun computeVerifier(password: String, salt: ByteArray): String {
        Log.d(TAG, "computeVerifier")
        val witness = deriveWithContext(password.trim(), salt)
        return schnorr.computeVerifier(witness)
    }

    //override fun deriveAesKey(password: String, salt: ByteArray): ByteArray {
    //    Log.d(TAG, "deriveAesKey")
    //    return deriveWithContext(password.trim(), salt)
    //}

     */

    override fun computeVerifier(password: String, salt: ByteArray): String {
        val witness = deriveWitness(password.trim(), salt)
        return schnorr.computeVerifier(witness)
    }

    override fun deriveAesKey(password: String, salt: ByteArray)  = deriveWithContext(password.trim(), salt, "enc")


    // ── private ───────────────────────────────────────────────────────────────
    /** PBKDF2(password, salt) → 32 raw bytes (witness i AES klíč sdílí stejný KDF) */
    private fun deriveWithContext(password: String, salt: ByteArray, context: String): ByteArray {
        val contextualSalt = salt + context.toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(password.toCharArray(), contextualSalt, CryptoConstants.PBKDF2_ITERATIONS, CryptoConstants.AES_KEY_SIZE)
        val factory = SecretKeyFactory.getInstance(CryptoConstants.PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    override fun deriveWitness(password: String, salt: ByteArray) = deriveWithContext(password.trim(), salt, "auth")

    // computeVerifier beze změny — interně zavolá deriveWitness přes Schnorr

    companion object {
        private const val TAG               = "AesKeyManagerImpl"
        private const val SHARED_PREFS_NAME = "encrypted_keys"
        private const val SALT_PREF         = "pbkdf2_salt"
        private const val ROOM_AES_KEY_PREF = "room_aes_key"
        private const val ROOM_IV_PREF      = "room_iv"
    }
}