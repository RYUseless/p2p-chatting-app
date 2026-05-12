package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.util.Base64
import android.util.Log
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESCryptoManagerImpl(
    private val roomId:     String,
    private val keyManager: KeyManager,
) : CryptoManager {

    private var secretKey:  ByteArray? = null
    private var iv:         ByteArray? = null
    private var isUnlocked: Boolean    = false

    // Server si uloží verifier pro pozdější ověření Schnorr proofu
    override var verifier: String?    = null
        private set

    // Klient si uloží witness – Controller z něj spočítá Schnorr proof
    override var witness:  ByteArray? = null
        private set

    override fun initializeAsServer(password: String): String {
        Log.d(TAG, "initializeAsServer: room=$roomId")
        val saltAuth = keyManager.generateSalt()
        val saltAes  = keyManager.generateSalt()
        iv           = keyManager.generateIV()

        secretKey = keyManager.deriveAesKey(password, saltAes)
        verifier  = keyManager.computeVerifier(password, saltAuth)

        keyManager.saveSalt(roomId, saltAuth)
        keyManager.saveRoomAesKey(roomId, secretKey!!, iv!!)
        isUnlocked = true

        val saltAuthB64 = Base64.encodeToString(saltAuth, Base64.NO_WRAP)
        val saltAesB64  = Base64.encodeToString(saltAes,  Base64.NO_WRAP)
        val ivB64       = Base64.encodeToString(iv!!,     Base64.NO_WRAP)
        Log.d(TAG, "initializeAsServer: payload=$saltAuthB64:$saltAesB64:$ivB64")
        return "$saltAuthB64:$saltAesB64:$ivB64"
    }

    override fun initializeAsClient(keyExchangeData: String, password: String): Boolean {
        Log.d(TAG, "initializeAsClient: raw='$keyExchangeData'")
        return try {
            val parts = keyExchangeData.split(":", limit = 3)
            require(parts.size == 3) { "Expected 3 parts, got ${parts.size}" }

            val saltAuth = Base64.decode(parts[0], Base64.NO_WRAP)
            val saltAes  = Base64.decode(parts[1], Base64.NO_WRAP)
            val ivBytes  = Base64.decode(parts[2], Base64.NO_WRAP)

            iv        = ivBytes
            secretKey = keyManager.deriveAesKey(password, saltAes)
            witness   = keyManager.deriveAesKey(password, saltAuth) // stejný KDF, jiný salt

            keyManager.saveSalt(roomId, saltAuth)
            keyManager.saveRoomAesKey(roomId, secretKey!!, iv!!)
            isUnlocked = true

            Log.d(TAG, "initializeAsClient: SUCCESS")
            true
        } catch (e: Exception) {
            Log.e(TAG, "initializeAsClient FAILED: ${e.message}", e)
            false
        }
    }

    override fun encrypt(text: String): String? {
        if (!isUnlocked || secretKey == null || iv == null) {
            Log.e(TAG, "encrypt failed: unlocked=$isUnlocked")
            return null
        }
        return try {
            val cipher = Cipher.getInstance(CryptoConstants.AES_CIPHER)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, CryptoConstants.AES_ALGORITHM), IvParameterSpec(iv))
            Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "encrypt exception: ${e.message}", e)
            null
        }
    }

    override fun decrypt(encryptedText: String): String? {
        if (!isUnlocked || secretKey == null || iv == null) {
            Log.e(TAG, "decrypt failed: unlocked=$isUnlocked")
            return null
        }
        return try {
            val cipher = Cipher.getInstance(CryptoConstants.AES_CIPHER)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, CryptoConstants.AES_ALGORITHM), IvParameterSpec(iv))
            String(cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP)))
        } catch (e: Exception) {
            Log.e(TAG, "decrypt exception: ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "AESCryptoManagerImpl"
    }
}