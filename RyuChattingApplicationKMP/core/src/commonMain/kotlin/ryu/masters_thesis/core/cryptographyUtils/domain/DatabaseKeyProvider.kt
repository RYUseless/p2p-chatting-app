package ryu.masters_thesis.core.cryptographyUtils.domain

interface DatabaseKeyProvider {
    /**
     * Vrací passphrase pro SQLCipher databázi jako ByteArray
     * První volání vygeneruje a uloží nový klíč, další volání vrátí uložený
     */
    fun getDatabasePassphrase(): ByteArray
}