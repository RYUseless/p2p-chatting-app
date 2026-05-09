package ryu.masters_thesis.presentation.home.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.ui.ChatRoomScreen
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel
import ryu.masters_thesis.presentation.connect.ui.JoinRoomDialog

data class ReconnectScreen(val roomName: String, val peerAddress: String) : Screen {

    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConnectScreenModel>()
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.onEvent(
                ConnectEvent.DirectConnect(
                    ScannedDeviceUiModel(
                        address = peerAddress,
                        name    = roomName,
                        roomId  = roomName,
                    )
                )
            )
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    //is ConnectOneTimeEvent.NavigateToChat -> navigator.replace(ChatRoomScreen(event.roomId, event.password))
                    is ConnectOneTimeEvent.NavigateToChat -> navigator.replace(
                        ChatRoomScreen(
                            roomName = event.roomId,
                            password = event.password,
                            forceIsServer = false
                        )
                    )
                    is ConnectOneTimeEvent.Dismiss        -> navigator.pop()
                    is ConnectOneTimeEvent.ShowError      -> navigator.pop()
                    is ConnectOneTimeEvent.Disconnected   -> navigator.pop()
                    is ConnectOneTimeEvent.ShowRelayInfo  -> navigator.pop()
                }
            }
        }

        SwipeableDismissWrapper(onDismiss = { navigator.pop() }) {
            state.selectedDevice?.let { device ->
                JoinRoomDialog(
                    device  = device,
                    state   = state,
                    onEvent = screenModel::onEvent,
                )
            }
        }
    }
}