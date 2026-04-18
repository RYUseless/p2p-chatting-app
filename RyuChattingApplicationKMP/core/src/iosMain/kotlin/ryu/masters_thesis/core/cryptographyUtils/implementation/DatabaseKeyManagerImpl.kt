package ryu.masters_thesis.core.cryptographyUtils.implementation

import ryu.masters_thesis.core.cryptographyUtils.domain.DatabaseKeyProvider

/*
TODO: implement in the future if I ever have any access on ios device
aka absolute tood freestyle, not sure if this even is correct :)
 */

class DatabaseKeyManagerImpl(
    private val keyManager: ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager,
) : DatabaseKeyProvider {
    override fun getDatabasePassphrase(): ByteArray = TODO("iOS: Keychain")
}