package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.util.Base64
import android.util.Log
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESCryptoManagerImpl(
    private val roomId:     String,
    private val keyManager: KeyManager,
) : CryptoManager {

    private var secretKey:  ByteArray? = null
    private var isUnlocked: Boolean    = false

    override var verifier: String?    = null
        private set
    override var witness:  ByteArray? = null
        private set

    override fun initializeAsServer(password: String): String {
        Log.d(TAG, "initializeAsServer: room=$roomId")
        val saltAuth = keyManager.generateSalt()
        val saltAes  = keyManager.generateSalt()

        secretKey = keyManager.deriveAesKey(password, saltAes)
        verifier  = keyManager.computeVerifier(password, saltAuth)

        keyManager.saveSalt(roomId, saltAuth)
        isUnlocked = true

        val saltAuthB64 = Base64.encodeToString(saltAuth, Base64.NO_WRAP)
        val saltAesB64  = Base64.encodeToString(saltAes,  Base64.NO_WRAP)
        Log.d(TAG, "initializeAsServer: payload=$saltAuthB64:$saltAesB64")
        return "$saltAuthB64:$saltAesB64"
    }

    override fun initializeAsClient(keyExchangeData: String, password: String): Boolean {
        Log.d(TAG, "initializeAsClient: raw='$keyExchangeData'")
        return try {
            val parts = keyExchangeData.split(":", limit = 2)
            require(parts.size == 2) { "Expected 2 parts, got ${parts.size}" }

            val saltAuth = Base64.decode(parts[0], Base64.NO_WRAP)
            val saltAes  = Base64.decode(parts[1], Base64.NO_WRAP)

            secretKey = keyManager.deriveAesKey(password, saltAes)
            //witness   = keyManager.deriveAesKey(password, saltAuth)
            witness = keyManager.deriveWitness(password, saltAuth)

            keyManager.saveSalt(roomId, saltAuth)
            isUnlocked = true

            Log.d(TAG, "initializeAsClient: SUCCESS")
            true
        } catch (e: Exception) {
            Log.e(TAG, "initializeAsClient FAILED: ${e.message}", e)
            false
        }
    }

    override fun encrypt(text: String): String? {
        if (!isUnlocked || secretKey == null) {
            Log.e(TAG, "encrypt failed: unlocked=$isUnlocked")
            return null
        }
        return try {
            val nonce  = ByteArray(CryptoConstants.GCM_NONCE_SIZE).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(CryptoConstants.AES_CIPHER)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(secretKey, CryptoConstants.AES_ALGORITHM),
                GCMParameterSpec(CryptoConstants.GCM_TAG_SIZE, nonce)
            )
            Base64.encodeToString(nonce + cipher.doFinal(text.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "encrypt exception: ${e.message}", e)
            null
        }
    }

    override fun decrypt(encryptedText: String): String? {
        if (!isUnlocked || secretKey == null) {
            Log.e(TAG, "decrypt failed: unlocked=$isUnlocked")
            return null
        }
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val cipher   = Cipher.getInstance(CryptoConstants.AES_CIPHER)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(secretKey, CryptoConstants.AES_ALGORITHM),
                GCMParameterSpec(CryptoConstants.GCM_TAG_SIZE, combined.copyOfRange(0, CryptoConstants.GCM_NONCE_SIZE))
            )
            String(cipher.doFinal(combined.copyOfRange(CryptoConstants.GCM_NONCE_SIZE, combined.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "decrypt exception: ${e.message}", e)
            null
        }
    }

    companion object {
        private const val TAG = "AESCryptoManagerImpl"
    }
}