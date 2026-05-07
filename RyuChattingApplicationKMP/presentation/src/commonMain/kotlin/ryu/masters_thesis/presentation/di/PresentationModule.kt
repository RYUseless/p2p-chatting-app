package ryu.masters_thesis.presentation.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomRepositoryImpl
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.implementation.ConnectRepositoryImpl
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel
import ryu.masters_thesis.presentation.create.domain.CreateRepository
import ryu.masters_thesis.presentation.create.implementation.CreateRepositoryImpl
import ryu.masters_thesis.presentation.create.implementation.CreateScreenModel
import ryu.masters_thesis.presentation.home.domain.HomeRepository
import ryu.masters_thesis.presentation.home.implementation.HomeRepositoryImpl
import ryu.masters_thesis.presentation.home.implementation.HomeScreenModel
import ryu.masters_thesis.presentation.settings.domain.SettingsRepository
import ryu.masters_thesis.presentation.settings.implementation.SettingsRepositoryImpl
import ryu.masters_thesis.presentation.settings.implementation.SettingsScreenModel
//new imports:
import org.koin.core.parameter.parametersOf
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.presentation.home.domain.ScanCoordinator
import ryu.masters_thesis.presentation.home.implementation.ScanCoordinatorImpl
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol


fun presentationModule() = module {
    // Repositories
    single<ConnectRepository>  { ConnectRepositoryImpl(get(named("client"))) }
    single<CreateRepository>   { CreateRepositoryImpl(get(named("server"))) }
    single<HomeRepository> { HomeRepositoryImpl(get<MessageRepository>()) }
    single<SettingsRepository> { SettingsRepositoryImpl() }

    // ScreenModels
    //factory { ConnectScreenModel(get()) }
    single<ScanCoordinator> { ScanCoordinatorImpl() }

    factory {
        ConnectScreenModel(
            get(),
            get<NeighbourProtocol> { parametersOf(get<BluetoothController>(named("client"))) },
            get<MessageRepository>(),
            get<ScanCoordinator>(),
        )
    }

    //factory { CreateScreenModel(get()) }
    factory {
        CreateScreenModel(
            get(),
            get<NeighbourProtocol> { parametersOf(get<BluetoothController>(named("server"))) },
            get<MessageRepository>(),
        )
    }

    //factory { HomeScreenModel(get()) }
    factory {
        HomeScreenModel(
            repository        = get<HomeRepository>(),
            finderProtocol    = get<FinderProtocol>(),
        )
    }

    factory { SettingsScreenModel(get()) }

    factory { (roomName: String, password: String, isServer: Boolean) ->
        val serverController = get<BluetoothController>(named("server"))
        val clientController = get<BluetoothController>(named("client"))
        val activeController = if (isServer) serverController else clientController
        ChatRoomScreenModel(
            roomName   = roomName,
            password   = password,
            repository = ChatRoomRepositoryImpl(
                controller       = activeController,
                serverController = serverController,
                channelId        = roomName,
                messageRepo      = get<MessageRepository>(),
            ),
        )
    }
}