package ryu.masters_thesis.core.cryptographyUtils.domain

import ryu.masters_thesis.core.cryptographyUtils.data.SchnorrProof

interface SchnorrProtocol {
    /** X = g^x mod p, kde x = PBKDF2(heslo, salt) */
    fun computeVerifier(witness: ByteArray): String

    /** Fiat-Shamir Schnorr proof znalosti x */
    fun computeProof(witness: ByteArray, context: String): SchnorrProof

    /** Ověří proof proti uloženému verifieru */
    fun verifyProof(proof: SchnorrProof, verifier: String, context: String): Boolean
}