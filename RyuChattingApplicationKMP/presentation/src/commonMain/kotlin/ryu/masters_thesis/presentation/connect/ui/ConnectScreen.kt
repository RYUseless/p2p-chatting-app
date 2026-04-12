package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.connect.domain.ConnectOneTimeEvent
import ryu.masters_thesis.presentation.connect.implementation.ConnectRepositoryImpl
import ryu.masters_thesis.presentation.connect.implementation.ConnectScreenModel

object ConnectScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DUMMY: ConnectRepositoryImpl injektovat přes DI až bude k dispozici
        val screenModel = rememberScreenModel { ConnectScreenModel(ConnectRepositoryImpl()) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is ConnectOneTimeEvent.NavigateToChat -> {
                        // TODO: navigator.push(ChatRoomScreen(event.roomId))
                    }
                    is ConnectOneTimeEvent.Dismiss        -> navigator.pop()
                    is ConnectOneTimeEvent.ShowError      -> Unit
                }
            }
        }

        SwipeableDismissWrapper(
            onDismiss = { navigator.pop() }
        ) {
            ConnectContent(
                state = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}