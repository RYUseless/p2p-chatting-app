package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel

data class ChatRoomScreen(
    val roomName: String,
    val password: String,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope     = rememberCoroutineScope()

        val server = getKoin().get<BluetoothController>(named("server"))
        val client = getKoin().get<BluetoothController>(named("client"))

        val screenModel = koinScreenModel<ChatRoomScreenModel>(
            parameters = { parametersOf(roomName, password, server.isServer.value) }
        )
        val state by screenModel.state.collectAsState()

        DisposableEffect(Unit) {
            onDispose {
                scope.launch(Dispatchers.IO) {
                    if (server.isServer.value) server.cleanup()
                    else client.resetConnection()
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ChatRoomOneTimeEvent.NavigateBack   -> navigator.popUntilRoot()
                    is ChatRoomOneTimeEvent.OpenFilePicker -> { }
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