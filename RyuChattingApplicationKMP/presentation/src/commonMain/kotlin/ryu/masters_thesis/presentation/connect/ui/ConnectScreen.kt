package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.domain.BluetoothControllerSingleton
import ryu.masters_thesis.presentation.chatroom.ui.ChatRoomScreen
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.implementation.ConnectRepositoryImpl
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel

object ConnectScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        //logísek
        println("CONNECT controller = ${BluetoothControllerSingleton.client::class.qualifiedName}")

        // TODO DI: BluetoothController předat přes DI místo singletonu
        val screenModel = rememberScreenModel {
            ConnectScreenModel(
                ConnectRepositoryImpl(
                    controller = BluetoothControllerSingleton.client,
                )
            )
        }
        val state by screenModel.state.collectAsState()

        //TODO: pak pridat dialog
        LaunchedEffect(Unit) {
            screenModel.restartScanning()
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ConnectOneTimeEvent.NavigateToChat -> navigator.push(ChatRoomScreen(event.roomId, event.password))
                    is ConnectOneTimeEvent.Dismiss        -> navigator.pop()
                    is ConnectOneTimeEvent.ShowError      -> navigator.pop()
                    is ConnectOneTimeEvent.Disconnected   -> { /* zůstaneme na ConnectScreen, UI se aktualizuje přes state */ }
                }
            }
        }

        SwipeableDismissWrapper(
            onDismiss = { navigator.pop() }
        ) {
            ConnectContent(
                state   = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}