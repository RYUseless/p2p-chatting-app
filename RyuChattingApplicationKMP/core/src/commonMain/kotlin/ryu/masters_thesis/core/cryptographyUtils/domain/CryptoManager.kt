package ryu.masters_thesis.core.cryptographyUtils.domain

interface CryptoManager {
    /**
     * Inicializace jako server (tvůrce místnosti) — generuje AES klíč, IV, salt
     * Vrací: "$saltB64:$ivB64:$encryptedKeyData" pro odeslání klientovi
     */
    fun initializeAsServer(password: String): String

    /**
     * Inicializace jako klient (připojující se) — přijímá šifrovaný klíč od serveru
     * Vrací: true pokud se dešifrování AES klíče zdařilo
     */
    fun initializeAsClient(keyExchangeData: String, password: String): Boolean

    /**
     * Zašifruje zprávu — vrací Base64 string nebo null při chybě
     */
    fun encrypt(text: String): String?

    /**
     * Dešifruje zprávu — vrací plaintext nebo null při chybě
     */
    fun decrypt(encryptedText: String): String?
}