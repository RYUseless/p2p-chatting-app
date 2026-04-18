package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ryu.masters_thesis.core.cryptographyUtils.domain.DatabaseKeyProvider
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager

class DatabaseKeyManagerImpl(
    private val context:    Context,
    private val keyManager: KeyManager,
) : DatabaseKeyProvider {

    override fun getDatabasePassphrase(): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val secureVault = EncryptedSharedPreferences.create(
            context,
            VAULT_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        val existingKey = secureVault.getString(KEY_DB_PASSPHRASE, null)
        if (existingKey != null) {
            return Base64.decode(existingKey, Base64.NO_WRAP)
        }

        val newKeyBytes   = keyManager.generateSecretKey()
        val newKeyBase64  = Base64.encodeToString(newKeyBytes, Base64.NO_WRAP)
        secureVault.edit().putString(KEY_DB_PASSPHRASE, newKeyBase64).apply()
        return newKeyBytes
    }

    companion object {
        private const val VAULT_NAME        = "secure_db_vault"
        private const val KEY_DB_PASSPHRASE = "db_sqlcipher_passphrase"
    }
}