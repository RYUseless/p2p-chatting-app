package ryu.masters_thesis.presentation.create.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.domain.BluetoothControllerSingleton
import ryu.masters_thesis.presentation.chatroom.domain.NoopBluetoothController
import ryu.masters_thesis.presentation.chatroom.ui.ChatRoomScreen
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.create.domain.CreateOneTimeEvent
import ryu.masters_thesis.presentation.create.implementation.CreateRepositoryImpl
import ryu.masters_thesis.presentation.create.implementation.CreateScreenModel

object CreateScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DI: BluetoothController předat přes DI místo singletonu
        val screenModel = rememberScreenModel {
            CreateScreenModel(
                CreateRepositoryImpl(
                    controller = BluetoothControllerSingleton.instance ?: NoopBluetoothController,
                )
            )
        }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is CreateOneTimeEvent.NavigateToChat -> navigator.push(ChatRoomScreen(event.roomId))
                    is CreateOneTimeEvent.Dismiss        -> navigator.pop()
                    is CreateOneTimeEvent.ShowError      -> Unit
                }
            }
        }

        SwipeableDismissWrapper(
            onDismiss = { navigator.pop() }
        ) {
            CreateContent(
                state   = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}