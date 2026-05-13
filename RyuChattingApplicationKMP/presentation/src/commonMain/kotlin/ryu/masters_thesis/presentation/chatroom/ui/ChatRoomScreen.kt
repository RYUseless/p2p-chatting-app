package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey // <-- PŘIDANÝ IMPORT
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.internal.BackHandler
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothController
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomScreenModel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class ChatRoomScreen(
    val roomName:      String,
    val password:      String,
    val forceIsServer: Boolean? = null,
    val sessionId:     String = kotlin.uuid.Uuid.random().toString(),
) : Screen {

    // TOTO JE TEN KLÍČOVÝ FIX PRO "FREEZNUTOU" OBRAZOVKU:
    // Pokaždé, když se vytvoří ChatRoomScreen, vygeneruje se nové UUID (sessionId).
    // Tím Voyager ztratí možnost podstrčit ti starou cache a vynutí čistý start.
    override val key: ScreenKey = "ChatRoom_${roomName}_${sessionId}"

    @OptIn(InternalVoyagerApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val server = getKoin().get<BluetoothController>(named("server"))
        val client = getKoin().get<BluetoothController>(named("client"))

        val isServerRole = forceIsServer ?: server.isServer.value

        val screenModel = koinScreenModel<ChatRoomScreenModel>(
            parameters = { parametersOf(roomName, password, isServerRole) }
        )
        val state by screenModel.state.collectAsState()
        //new:
        val messageInput by screenModel.messageInput.collectAsState()

        DisposableEffect(isServerRole) {
            onDispose {
                if (!isServerRole) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            client.resetConnection()
                        } catch (e: Exception) {
                            co.touchlab.kermit.Logger.e("ChatRoomScreen") { "Chyba při resetu klienta: ${e.message}" }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ChatRoomOneTimeEvent.NavigateBack   -> navigator.popUntilRoot()

                    is ChatRoomOneTimeEvent.OpenFilePicker -> { }
                    is ChatRoomOneTimeEvent.ShowError      -> { }
                    is ChatRoomOneTimeEvent.ReloadAsServer -> {
                        navigator.replace(
                            ChatRoomScreen(
                                roomName = event.roomId,
                                password = this@ChatRoomScreen.password,
                                forceIsServer = true
                            )
                        )
                    }
                }
            }
        }

        BackHandler(enabled = true) {
            screenModel.onEvent(ChatRoomEvent.BackClicked)
        }

        ChatRoomContent(
            state        = state,
            messageInput = messageInput, // ← ZMĚNA
            onEvent      = screenModel::onEvent,
        )
    }
}