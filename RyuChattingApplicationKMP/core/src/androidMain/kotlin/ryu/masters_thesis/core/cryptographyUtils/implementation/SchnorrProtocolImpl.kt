package ryu.masters_thesis.core.cryptographyUtils.implementation

import android.util.Base64
import org.bouncycastle.crypto.ec.CustomNamedCurves
import ryu.masters_thesis.core.cryptographyUtils.data.SchnorrProof
import ryu.masters_thesis.core.cryptographyUtils.domain.SchnorrProtocol
import java.math.BigInteger

class SchnorrProtocolImpl(
    private val hashFn: (ByteArray) -> ByteArray,
    private val randomBytes: (Int) -> ByteArray
) : SchnorrProtocol {

    private val params = CustomNamedCurves.getByName("secp256k1")
    private val curve  = params.curve
    private val G      = params.g
    private val n      = params.n

    override fun computeVerifier(witness: ByteArray): String {
        val x = BigInteger(1, witness).mod(n)
        return G.multiply(x).normalize().getEncoded(false).b64()
    }

    override fun computeProof(witness: ByteArray, context: String): SchnorrProof {
        val x = BigInteger(1, witness).mod(n)
        val X = G.multiply(x).normalize()
        val r = BigInteger(1, randomBytes(32)).mod(n - BigInteger.ONE) + BigInteger.ONE
        val R = G.multiply(r).normalize()
        val c = BigInteger(1, hashFn(R.affineXCoord.encoded + X.affineXCoord.encoded + context.toByteArray())).mod(n)
        val s = r.add(c.multiply(x)).mod(n)
        return SchnorrProof(rBase64 = R.getEncoded(false).b64(), sBase64 = s.b32().b64())
    }

    override fun verifyProof(proof: SchnorrProof, verifier: String, context: String): Boolean = try {
        val R = curve.decodePoint(proof.rBase64.db64()).normalize()
        val s = BigInteger(1, proof.sBase64.db64())
        val X = curve.decodePoint(verifier.db64()).normalize()
        val c = BigInteger(1, hashFn(R.affineXCoord.encoded + X.affineXCoord.encoded + context.toByteArray())).mod(n)
        G.multiply(s).normalize().getEncoded(false)
            .contentEquals(R.add(X.multiply(c)).normalize().getEncoded(false))
    } catch (_: Exception) { false }

    private fun BigInteger.b32(): ByteArray {
        val b = toByteArray()
        return when {
            b.size == 33 && b[0] == 0.toByte() -> b.copyOfRange(1, 33)
            b.size == 32 -> b
            b.size > 32  -> b.takeLast(32).toByteArray()
            else         -> ByteArray(32 - b.size) + b
        }
    }
    private fun ByteArray.b64() = Base64.encodeToString(this, Base64.NO_WRAP)!!
    private fun String.db64() = Base64.decode(this, Base64.NO_WRAP)!!
}