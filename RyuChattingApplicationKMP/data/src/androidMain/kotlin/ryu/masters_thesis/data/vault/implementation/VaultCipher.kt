package ryu.masters_thesis.data.vault.implementation

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER  = "AndroidKeyStore"
private const val HMAC_KEY_ALIAS     = "ryu_vault_hmac_key"
private const val ROOM_KEY_PREFIX    = "ryu_vault_room_"
private const val AES_KEY_SIZE       = 256
private const val GCM_TAG_LENGTH     = 128
private const val IV_LENGTH          = 12
private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
private const val HMAC_ALGORITHM     = "HmacSHA256"

class VaultCipher(
    private val requireUserAuthentication: Boolean = false,
) {

    fun hmacRoomId(roomId: String): Result<String> = runCatching {
        val input  = roomId.toByteArray(Charsets.UTF_8)
        val result = Mac.getInstance(HMAC_ALGORITHM).apply {
            init(getOrCreateHmacKey())
        }.doFinal(input)
        input.fill(0)
        Base64.encodeToString(result, Base64.NO_WRAP)
    }

    fun encrypt(plaintext: ByteArray, hashedRoomId: String): Result<String> = runCatching {
        val cipher     = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateRoomKey(hashedRoomId))
        val combined   = cipher.iv + cipher.doFinal(plaintext)
        plaintext.fill(0)
        val encoded    = Base64.encodeToString(combined, Base64.NO_WRAP)
        combined.fill(0)
        encoded
    }

    fun decrypt(encoded: String, hashedRoomId: String): Result<ByteArray> = runCatching {
        val combined   = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH) { "Ciphertext too short" }
        val iv         = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher     = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateRoomKey(hashedRoomId), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext  = cipher.doFinal(ciphertext)
        combined.fill(0)
        ciphertext.fill(0)
        plaintext
    }

    fun deleteRoomKey(hashedRoomId: String) {
        val alias = "$ROOM_KEY_PREFIX$hashedRoomId"
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            .takeIf { it.containsAlias(alias) }
            ?.deleteEntry(alias)
    }

    fun listRoomKeyHashedIds(): List<String> =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            .aliases().toList()
            .filter  { it.startsWith(ROOM_KEY_PREFIX) }
            .map     { it.removePrefix(ROOM_KEY_PREFIX) }

    private fun getOrCreateHmacKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!ks.containsAlias(HMAC_KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_PROVIDER)
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(HMAC_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                            .build()
                    )
                }.generateKey()
        }
        return (ks.getEntry(HMAC_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun getOrCreateRoomKey(hashedRoomId: String): SecretKey {
        val alias = "$ROOM_KEY_PREFIX$hashedRoomId"
        val ks    = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!ks.containsAlias(alias)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(
                            alias,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(AES_KEY_SIZE)
                            .setUserAuthenticationRequired(requireUserAuthentication)
                            .build()
                    )
                }.generateKey()
        }
        return (ks.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
    }
}