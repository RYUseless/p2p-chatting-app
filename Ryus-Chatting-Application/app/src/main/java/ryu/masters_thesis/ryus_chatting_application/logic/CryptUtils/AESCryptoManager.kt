package ryu.masters_thesis.ryus_chatting_application.logic.CryptUtils

import android.content.Context
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AESCryptoManager (private val context: Context,
                        private val roomId: String
) {

    private var secretKey: SecretKey? = null
    private var iv: IvParameterSpec? = null
    private var isUnlocked: Boolean = false

    /**
     * Initialize crypto for room creator (server)
     * Generates new AES key and encrypts it with password
     */
    fun initializeAsServer(password: String): String {
        Log.d("MessageCryptoUtils", "🔑 Server init: room=$roomId password=${password.length}ch")
        // Generate new AES key and IV for this room
        secretKey = AesKeyManager.generateSecretKey()
        iv = AesKeyManager.generateIV()


        Log.d("MessageCryptoUtils", "Server: Generated AES key and IV")

        // Generate salt for PBKDF2
        val salt = AesKeyManager.generateSalt()

        // Save salt for this room
        AesKeyManager.saveSalt(context, roomId, salt)

        // Save the AES key locally
        AesKeyManager.saveRoomAesKey(context, roomId, secretKey!!, iv!!)

        // Encrypt AES key with password for transmission
        val encryptedKeyData = AesKeyManager.encryptAesKeyWithPassword(secretKey!!, password, salt)

        // Mark as unlocked
        isUnlocked = true

        // Return: salt + IV + encrypted AES key (for client to decrypt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv!!.iv, Base64.NO_WRAP)

        Log.d("MessageCryptoUtils", "📤 Server SENDS: $saltB64:$ivB64:${encryptedKeyData.take(20)}...")
        return "$saltB64:$ivB64:$encryptedKeyData"
    }

    /**
     * Initialize crypto for room joiner (client)
     * Receives encrypted AES key and decrypts with password
     */
    fun initializeAsClient(keyExchangeData: String, password: String): Boolean {
        Log.d("MessageCryptoUtils", "Client: RAW keyExchangeData='$keyExchangeData'")

        return try {
            val parts = keyExchangeData.split(":", limit = 3)
            if (parts.size != 3) {
                Log.e("MessageCryptoUtils", "Client: Invalid format, expected 3 parts got ${parts.size}: $keyExchangeData")
                return false
            }

            val saltB64 = parts[0]
            val ivB64 = parts[1]
            val encryptedKeyData = parts[2]

            Log.d("MessageCryptoUtils", "Client: saltB64=${saltB64.take(20)}... ivB64=${ivB64.take(20)}...")

            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)

            Log.d("MessageCryptoUtils", "Client: salt=${salt.size}B iv=${ivBytes.size}B password=${password.length}ch")

            iv = IvParameterSpec(ivBytes)
            AesKeyManager.saveSalt(context, roomId, salt)

            // ← ZDE SELHÁVA decryptAesKeyWithPassword()
            secretKey = AesKeyManager.decryptAesKeyWithPassword(encryptedKeyData, password, salt)

            AesKeyManager.saveRoomAesKey(context, roomId, secretKey!!, iv!!)
            isUnlocked = true

            Log.d("MessageCryptoUtils", "✅ Client: AES KEY DECRYPTED SUCCESS!")
            true
        } catch (e: Exception) {
            Log.e("MessageCryptoUtils", "❌ Client decrypt FAILED: ${e.message}", e)
            false
        }
    }

    /**
     * Encrypt message for transmission
     */
    fun encrypt(textToEncrypt: String): String? {
        if (!isUnlocked || secretKey == null || iv == null) {
            Log.e("MessageCryptoUtils", "Encrypt failed: unlocked=$isUnlocked key=${secretKey!=null} iv=${iv!=null}")
            return null
        }

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        val encryptedBytes = cipher.doFinal(textToEncrypt.toByteArray())
        val result = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        Log.d("MessageCryptoUtils", "Encrypted: '$textToEncrypt' -> ${result.take(20)}...")
        return result
    }

    /**
     * Decrypt received message
     */
    fun decrypt(encryptedText: String): String? {
        if (!isUnlocked || secretKey == null || iv == null) {
            Log.e("MessageCryptoUtils", "Decrypt failed: unlocked=$isUnlocked key=${secretKey!=null} iv=${iv!=null}")
            return null
        }

        return try {
            val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val result = String(decryptedBytes)

            Log.d("MessageCryptoUtils", "Decrypted: ${encryptedText.take(20)}... -> '$result'")
            result
        } catch (e: Exception) {
            Log.e("MessageCryptoUtils", "Decrypt exception: ${e.message}", e)
            null
        }
    }
}