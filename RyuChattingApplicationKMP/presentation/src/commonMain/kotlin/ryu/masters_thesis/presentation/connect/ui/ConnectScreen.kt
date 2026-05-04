package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import co.touchlab.kermit.Logger
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
                    is ConnectOneTimeEvent.ShowRelayInfo -> {
                        Logger.d("BNP") { "Relay to ${event.name}@${event.destinationAddress} via ${event.hopCount} hops" }
                        navigator.pop()
                    }
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