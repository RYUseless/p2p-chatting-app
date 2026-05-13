package ryu.masters_thesis.core.performance_tests

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import ryu.masters_thesis.core.cryptographyUtils.implementation.AesKeyManagerImpl
import ryu.masters_thesis.core.cryptographyUtils.implementation.SchnorrProtocolImpl
import java.security.MessageDigest
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
class AesPerformanceTest {

    private lateinit var keyManager: KeyManager

    private val password = "testPassword123"
    private lateinit var saltAuth: ByteArray
    private lateinit var saltAes: ByteArray

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val schnorr = SchnorrProtocolImpl(
            hashFn      = { data -> MessageDigest.getInstance("SHA-256").digest(data) },
            randomBytes = { size -> ByteArray(size).also { SecureRandom().nextBytes(it) } }
        )
        keyManager = AesKeyManagerImpl(context, schnorr)
        saltAuth   = keyManager.generateSalt()
        saltAes    = keyManager.generateSalt()
    }

    @Test
    fun benchmark_deriveAesKey() {
        val avg = measureMs("deriveAesKey_PBKDF2", warmups = 1, runs = 5) {
            keyManager.deriveAesKey(password, saltAes)
        }
        println("[PERF_RESULT] deriveAesKey avg: ${avg}ms")
    }

    @Test
    fun benchmark_computeVerifier() {
        val avg = measureMs("computeVerifier_PBKDF2", warmups = 1, runs = 5) {
            keyManager.computeVerifier(password, saltAuth)
        }
        println("[PERF_RESULT] computeVerifier (KeyManager) avg: ${avg}ms")
    }

    @Test
    fun benchmark_bothDerivations_serverInit() {
        val avg = measureMs("serverInit_bothPBKDF2", warmups = 1, runs = 5) {
            keyManager.computeVerifier(password, saltAuth)
            keyManager.deriveAesKey(password, saltAes)
        }
        println("[PERF_RESULT] server PBKDF2×2 avg: ${avg}ms")
    }

    @Test
    fun benchmark_generateSalt() {
        val avg = measureMs("generateSalt", runs = 100) {
            keyManager.generateSalt()
        }
        println("[PERF_RESULT] generateSalt avg: ${avg}ms")
    }
}