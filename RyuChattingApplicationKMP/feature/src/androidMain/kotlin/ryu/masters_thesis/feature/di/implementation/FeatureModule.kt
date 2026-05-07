// feature/src/androidMain/kotlin/ryu/masters_thesis/feature/di/implementation/FeatureModule.kt

package ryu.masters_thesis.feature.di.domain
// why yapi yapi -- fix later

import android.content.Context
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.data.vault.domain.MessageVault
import ryu.masters_thesis.data.vault.implementation.MessageVaultImpl
import ryu.masters_thesis.data.vault.implementation.VaultCipher
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerClient
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerServer
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.BNP_CHANNEL_ID
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.LocalDevice
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol
import ryu.masters_thesis.feature.bluetoothNeighbourProtocol.implementation.LocalDeviceImpl
import ryu.masters_thesis.feature.bluetoothNeighbourProtocol.implementation.NeighbourProtocolImpl
import ryu.masters_thesis.feature.bluetoothNeighbourTransport.implementation.NeighbourTransportImpl
import ryu.masters_thesis.feature.bluetoothTransportProtocol.domain.NeighbourTransport
import ryu.masters_thesis.feature.messages.domain.ChatManager
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.feature.messages.implementation.ChatManagerImpl
import ryu.masters_thesis.feature.messages.implementation.MessageRepositoryImpl
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderResponder
import ryu.masters_thesis.feature.bluetoothFinderProtocol.implementation.FinderProtocolImpl
import ryu.masters_thesis.feature.bluetoothFinderProtocol.implementation.FinderResponderImpl

actual fun featurePlatformModule() = module {
    single<BluetoothController>(named("client")) {
        BluetoothControllerClient(get<Context>(), get<(String) -> CryptoManager>())
    }
    single<BluetoothController>(named("server")) {
        BluetoothControllerServer(get<Context>(), get<(String) -> CryptoManager>())
    }
    single<ChatManager> { ChatManagerImpl() }

    // --- Vault ---
    single { VaultCipher() }
    single<MessageVault> { MessageVaultImpl(get<Context>(), get()) }
    single<MessageRepository> { MessageRepositoryImpl(get()) }

    // --- BNP ---
    single<LocalDevice> {
        val adapter = get<Context>()
            .getSystemService(Context.BLUETOOTH_SERVICE)
            .let { it as android.bluetooth.BluetoothManager }
            .adapter
        LocalDeviceImpl(adapter = adapter, context = get<Context>())
    }

    // --- BFP ---
    single<FinderProtocol> { FinderProtocolImpl(get<Context>()) }

    single<FinderResponder> {
        FinderResponderImpl(
            context   = get<Context>(),
            isHosting = {
                val serverController = get<BluetoothController>(named("server"))
                if (serverController.isServer.value) serverController.currentRoomId.value
                else null
            },
        ).also { it.start() }
    }

    factory<NeighbourTransport> { (controller: BluetoothController) ->
        NeighbourTransportImpl(
            controller = controller,
            channelId  = BNP_CHANNEL_ID,
        )
    }
    factory<NeighbourProtocol> { (controller: BluetoothController) ->
        NeighbourProtocolImpl(
            transport   = get { parametersOf(controller) },
            localDevice = get(),
        )
    }
}