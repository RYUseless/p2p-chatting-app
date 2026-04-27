package ryu.masters_thesis.feature.di.domain

import android.content.Context
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerClient
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerServer
import ryu.masters_thesis.feature.messages.domain.ChatManager
import ryu.masters_thesis.feature.messages.implementation.ChatManagerImpl

actual fun featurePlatformModule() = module {
    single<BluetoothController>(named("client")) {
        BluetoothControllerClient(get<Context>(), get<(String) -> CryptoManager>())
    }
    single<BluetoothController>(named("server")) {
        BluetoothControllerServer(get<Context>(), get<(String) -> CryptoManager>())
    }
    single<ChatManager> { ChatManagerImpl() }
}