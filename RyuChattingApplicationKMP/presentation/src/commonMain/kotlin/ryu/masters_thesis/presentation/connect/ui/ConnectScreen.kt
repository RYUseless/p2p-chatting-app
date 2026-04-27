package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.ui.ChatRoomScreen
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel

object ConnectScreen : Screen {

    @Composable
    override fun Content() {
        val navigator  = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ConnectScreenModel>()
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.restartScanning()
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ConnectOneTimeEvent.NavigateToChat -> navigator.push(ChatRoomScreen(event.roomId, event.password))
                    is ConnectOneTimeEvent.Dismiss        -> navigator.pop()
                    is ConnectOneTimeEvent.ShowError      -> navigator.pop()
                    is ConnectOneTimeEvent.Disconnected   -> { }
                }
            }
        }

        SwipeableDismissWrapper(onDismiss = { navigator.pop() }) {
            ConnectContent(
                state   = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}