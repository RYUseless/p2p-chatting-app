package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.domain.BluetoothControllerSingleton
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.domain.NoopBluetoothController
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomRepositoryImpl
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel

data class ChatRoomScreen(val roomName: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DI: BluetoothController předat přes DI místo singletonu
        val screenModel = rememberScreenModel(tag = roomName) {
            ChatRoomScreenModel(
                roomName   = roomName,
                repository = ChatRoomRepositoryImpl(
                    controller = BluetoothControllerSingleton.instance ?: NoopBluetoothController,
                    channelId  = roomName,
                ),
            )
        }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ChatRoomOneTimeEvent.NavigateBack   -> navigator.pop()
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