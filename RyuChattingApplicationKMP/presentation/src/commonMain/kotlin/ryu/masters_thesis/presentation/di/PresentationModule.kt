package ryu.masters_thesis.presentation.di

import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderProtocol
import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderResponder
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol
import ryu.masters_thesis.feature.messages.domain.MessageRepository
import ryu.masters_thesis.feature.messages.domain.RoomConfigRepository
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomRepositoryImpl
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel
import ryu.masters_thesis.presentation.connect.domain.ConnectRepository
import ryu.masters_thesis.presentation.connect.implementation.ConnectRepositoryImpl
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel
import ryu.masters_thesis.presentation.create.domain.CreateRepository
import ryu.masters_thesis.presentation.create.implementation.CreateRepositoryImpl
import ryu.masters_thesis.presentation.create.implementation.CreateScreenModel
import ryu.masters_thesis.presentation.home.domain.HomeRepository
import ryu.masters_thesis.presentation.home.domain.ScanCoordinator
import ryu.masters_thesis.presentation.home.implementation.HomeRepositoryImpl
import ryu.masters_thesis.presentation.home.implementation.HomeScreenModel
import ryu.masters_thesis.presentation.home.implementation.ScanCoordinatorImpl
import ryu.masters_thesis.presentation.settings.domain.SettingsRepository
import ryu.masters_thesis.presentation.settings.implementation.SettingsRepositoryImpl
import ryu.masters_thesis.presentation.settings.implementation.SettingsScreenModel

fun presentationModule() = module {
    // Repositories

    // ## CONNECT ##
    single<ConnectRepository> {
        ConnectRepositoryImpl(
            controller       = get<BluetoothController>(named("client")),
        )
    }

    // ## CREATE ##
    single<CreateRepository> {
        CreateRepositoryImpl(
            controller       = get<BluetoothController>(named("server")),
        )
    }

    // ## HOME ##
    single<HomeRepository> {
        HomeRepositoryImpl(
            messageRepository = get<MessageRepository>(),
            finderProtocol    = get<FinderProtocol>(),
        )
    }

    // ## SETTINGS ##
    //todo: optimisation
    single<SettingsRepository> { SettingsRepositoryImpl() }

    // ScreenModels
    single<ScanCoordinator> { ScanCoordinatorImpl() }

    factory {
        ConnectScreenModel(
            get(),
            get<NeighbourProtocol> { parametersOf(get<BluetoothController>(named("client"))) },
            get<MessageRepository>(),
            get<ScanCoordinator>(),
        )
    }

    factory {
        CreateScreenModel(
            get(),
            get<NeighbourProtocol> { parametersOf(get<BluetoothController>(named("server"))) },
            get<MessageRepository>(),
        )
    }

    factory {
        HomeScreenModel(
            repository     = get<HomeRepository>(),
        )
    }

    factory { SettingsScreenModel(get()) }

    factory { (roomName: String, password: String, isServer: Boolean) ->
        val controller = if (isServer) get<BluetoothController>(named("server"))
        else          get<BluetoothController>(named("client"))
        ChatRoomScreenModel(
            roomName          = roomName,
            password          = password,
            isServer          = isServer,
            repository        = ChatRoomRepositoryImpl(
                controller       = controller,
                serverController = get(named("server")),
                channelId        = roomName,
                password         = password,
                messageRepo      = get(),
                roomConfigRepo   = get<RoomConfigRepository>(),
            ),
            neighbourProtocol = get<NeighbourProtocol> { parametersOf(controller) },
        )
    }
}