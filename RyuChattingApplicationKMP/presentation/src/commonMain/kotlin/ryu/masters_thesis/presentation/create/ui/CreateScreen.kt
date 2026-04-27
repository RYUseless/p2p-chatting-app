package ryu.masters_thesis.presentation.create.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.chatroom.ui.ChatRoomScreen
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.create.domain.CreateOneTimeEvent
import ryu.masters_thesis.presentation.create.implementation.CreateScreenModel

object CreateScreen : Screen {

    @Composable
    override fun Content() {
        val navigator   = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CreateScreenModel>()
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is CreateOneTimeEvent.NavigateToChat -> navigator.push(ChatRoomScreen(event.roomId, event.password))
                    is CreateOneTimeEvent.Dismiss        -> navigator.pop()
                    is CreateOneTimeEvent.ShowError      -> Unit
                }
            }
        }

        SwipeableDismissWrapper(onDismiss = { navigator.pop() }) {
            CreateContent(
                state   = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}