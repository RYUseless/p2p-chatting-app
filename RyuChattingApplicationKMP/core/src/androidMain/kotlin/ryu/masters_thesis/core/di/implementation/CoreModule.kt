package ryu.masters_thesis.core.di.domain

import android.content.Context
import org.koin.dsl.module
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.core.cryptographyUtils.domain.KeyManager
import ryu.masters_thesis.core.cryptographyUtils.implementation.AESCryptoManagerImpl
import ryu.masters_thesis.core.cryptographyUtils.implementation.AesKeyManagerImpl
import ryu.masters_thesis.core.qrCode.domain.QrCodeGenerator
import ryu.masters_thesis.core.qrCode.implementation.QrCodeGeneratorImpl

actual fun corePlatformModule() = module {
    single<KeyManager> { AesKeyManagerImpl(get<Context>()) }
    single<(String) -> CryptoManager> { { roomId: String -> AESCryptoManagerImpl(roomId = roomId, keyManager = get()) } }
    single<QrCodeGenerator> { QrCodeGeneratorImpl() }
}