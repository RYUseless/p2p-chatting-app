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

fun presentationModule() = module {
    // Repositories
    single<ConnectRepository>  { ConnectRepositoryImpl(get(named("client"))) }
    single<CreateRepository>   { CreateRepositoryImpl(get(named("server"))) }
    single<HomeRepository>     { HomeRepositoryImpl() }
    single<SettingsRepository> { SettingsRepositoryImpl() }

    // ScreenModels
    factory { ConnectScreenModel(get()) }
    factory { CreateScreenModel(get()) }
    factory { HomeScreenModel(get()) }
    factory { SettingsScreenModel(get()) }
    factory { (roomName: String, password: String, isServer: Boolean) ->
        val controller = if (isServer) get<BluetoothController>(named("server"))
        else get<BluetoothController>(named("client"))
        ChatRoomScreenModel(
            roomName   = roomName,
            password   = password,
            repository = ChatRoomRepositoryImpl(
                controller = controller,
                channelId  = roomName,
            ),
        )
    }
}