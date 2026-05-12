package ryu.masters_thesis.core.cryptographyUtils.domain

interface CryptoManager {

    /** Server: Schnorr verifier X = g^x·G, dostupný po initializeAsServer */
    val verifier: String?

    /** Klient: witness pro Schnorr proof, dostupný po initializeAsClient */
    val witness: ByteArray?

    /**
     * Inicializace jako server — generuje AES klíč, IV, salt_auth, salt_aes
     * Vrací: "$saltAuthB64:$saltAesB64:$ivB64" pro odeslání klientovi
     */
    fun initializeAsServer(password: String): String

    /**
     * Inicializace jako klient — derivuje AES klíč a witness z hesla
     * Vrací: true pokud derivace proběhla úspěšně
     */
    fun initializeAsClient(keyExchangeData: String, password: String): Boolean

    /** Zašifruje zprávu — vrací Base64 string nebo null při chybě */
    fun encrypt(text: String): String?

    /** Dešifruje zprávu — vrací plaintext nebo null při chybě */
    fun decrypt(encryptedText: String): String?
}