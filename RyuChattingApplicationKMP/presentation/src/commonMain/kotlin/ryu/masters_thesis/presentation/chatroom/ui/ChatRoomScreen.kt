package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.domain.BluetoothControllerSingleton
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomRepositoryImpl
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel

data class ChatRoomScreen(val roomName: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DI: BluetoothController předat přes DI místo singletonu
        val screenModel = rememberScreenModel(tag = roomName) {
            val controller = if (BluetoothControllerSingleton.server.isServer.value)
                BluetoothControllerSingleton.server
            else
                BluetoothControllerSingleton.client

            ChatRoomScreenModel(
                roomName   = roomName,
                repository = ChatRoomRepositoryImpl(
                    controller = controller,
                    channelId  = roomName,
                ),
            )
        }
        val state by screenModel.state.collectAsState()

        // Reset spojení při opuštění ChatRoom
        DisposableEffect(Unit) {
            onDispose {
                if (BluetoothControllerSingleton.server.isServer.value) {
                    BluetoothControllerSingleton.server.cleanup()
                } else {
                    BluetoothControllerSingleton.client.resetConnection()
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    // go back to homescreen, not connectscreen or createscreen
                    is ChatRoomOneTimeEvent.NavigateBack   -> navigator.popUntilRoot()
                    is ChatRoomOneTimeEvent.OpenFilePicker -> {
                        // TODO DUMMY: file picker až bude :core dostupný
                    }
                    is ChatRoomOneTimeEvent.ShowError      -> Unit
                }
            }
        }

        ChatRoomContent(
            state   = state,
            onEvent = screenModel::onEvent,
        )
    }
}