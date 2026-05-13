package ryu.masters_thesis.core.performance_tests

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ryu.masters_thesis.core.cryptographyUtils.domain.SchnorrProtocol
import ryu.masters_thesis.core.cryptographyUtils.implementation.SchnorrProtocolImpl
import java.security.MessageDigest
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SchnorrPerformanceTest {

    private lateinit var schnorr: SchnorrProtocol

    // witness = PBKDF2 výstup – simulujeme jako náhodný 32B klíč
    private val witness  = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private val context  = "test_room_password"

    @Before
    fun setup() {
        schnorr = SchnorrProtocolImpl(
            hashFn     = { data -> MessageDigest.getInstance("SHA-256").digest(data) },
            randomBytes = { size -> ByteArray(size).also { SecureRandom().nextBytes(it) } }
        )
    }

    @Test
    fun benchmark_computeVerifier() {
        val avg = measureMs("computeVerifier", warmups = 2, runs = 5) {
            schnorr.computeVerifier(witness)
        }
        println("[PERF_RESULT] computeVerifier avg: ${avg}ms")
    }

    @Test
    fun benchmark_computeProof() {
        val avg = measureMs("computeProof", warmups = 2, runs = 5) {
            schnorr.computeProof(witness, context)
        }
        println("[PERF_RESULT] computeProof avg: ${avg}ms")
    }

    @Test
    fun benchmark_verifyProof() {
        val verifier = schnorr.computeVerifier(witness)
        val proof    = schnorr.computeProof(witness, context)
        val avg = measureMs("verifyProof", warmups = 2, runs = 10) {
            schnorr.verifyProof(proof, verifier, context)
        }
        println("[PERF_RESULT] verifyProof avg: ${avg}ms")
    }

    @Test
    fun benchmark_fullZkHandshake() {
        val avg = measureMs("fullZkHandshake", warmups = 1, runs = 5) {
            val v = schnorr.computeVerifier(witness)
            val p = schnorr.computeProof(witness, context)
            schnorr.verifyProof(p, v, context)
        }
        println("[PERF_RESULT] fullZkHandshake avg: ${avg}ms")
    }
}