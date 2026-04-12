package ryu.masters_thesis.ryus_chatting_application.logic.CryptUtils

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object DatabaseKeyManager {
    private const val VAULT_NAME = "secure_db_vault"
    private const val KEY_DB_PASSPHRASE = "db_sqlcipher_passphrase"

    fun getDatabasePassphrase(context: Context): ByteArray {
        // 1. Získání MasterKey z hardwarového Android Keystore (hlavní klíč k trezoru)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // 2. Vytvoření "Vaultu" (EncryptedSharedPreferences)
        // Vše, co se sem uloží, je automaticky šifrováno Master klíčem
        val secureVault = EncryptedSharedPreferences.create(
            context,
            VAULT_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // 3. Zkusíme načíst už existující klíč k databázi
        val existingKeyBase64 = secureVault.getString(KEY_DB_PASSPHRASE, null)
        if (existingKeyBase64 != null) {
            // Pokud ho máme, vrátíme ho dekódovaný jako pole bajtů
            return Base64.decode(existingKeyBase64, Base64.NO_WRAP)
        }

        // 4. Pokud databáze heslo ještě nemá (první spuštění), VYGENERUJEME HO TVOJÍ FUNKCÍ
        val newSecretKey = AesKeyManager.generateSecretKey()

        // Z AES SecretKey objektu si vytáhneme čistých 32 bajtů (raw bytes)
        val rawKeyBytes = newSecretKey.encoded

        // Uložíme do našeho trezoru zakódované v Base64
        val newKeyBase64 = Base64.encodeToString(rawKeyBytes, Base64.NO_WRAP)
        secureVault.edit().putString(KEY_DB_PASSPHRASE, newKeyBase64).apply()

        // SQLCipher přijímá raw pole bajtů jako heslo
        return rawKeyBytes
    }
}