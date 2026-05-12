package ryu.masters_thesis.core.di.domain

import android.content.Context
import org.koin.dsl.module
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import ryu.masters_thesis.core.cryptographyUtils.domain.SchnorrProtocol
import ryu.masters_thesis.core.cryptographyUtils.implementation.AESCryptoManagerImpl
import ryu.masters_thesis.core.cryptographyUtils.implementation.AesKeyManagerImpl
import ryu.masters_thesis.core.cryptographyUtils.implementation.SchnorrProtocolImpl
import ryu.masters_thesis.core.qrCode.domain.QrCodeGenerator
import ryu.masters_thesis.core.qrCode.implementation.QrCodeGeneratorImpl
import java.security.MessageDigest
import java.security.SecureRandom

actual fun corePlatformModule() = module {
    single<SchnorrProtocol> {
        SchnorrProtocolImpl(
            hashFn      = { data -> MessageDigest.getInstance("SHA-256").digest(data) },
            randomBytes = { n -> ByteArray(n).also { SecureRandom().nextBytes(it) } }
        )
    }
    single<KeyManager> { AesKeyManagerImpl(context = get<Context>(), schnorr = get()) }
    single<(String) -> CryptoManager> { { roomId: String -> AESCryptoManagerImpl(roomId = roomId, keyManager = get()) } }
    single<QrCodeGenerator> { QrCodeGeneratorImpl() }
}