package ryu.masters_thesis.presentation.settings.ui

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ryu.masters_thesis.presentation.component.ui.SwipeableDismissWrapper
import ryu.masters_thesis.presentation.settings.domain.SettingsOneTimeEvent
import ryu.masters_thesis.presentation.settings.implementation.SettingsRepositoryImpl
import ryu.masters_thesis.presentation.settings.implementation.SettingsScreenModel

// Lehký entry point – žádné UI, pouze definice screenu a propojení závislostí
object SettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DUMMY: SettingsRepositoryImpl injektovat přes DI až bude k dispozici
        val screenModel = rememberScreenModel { SettingsScreenModel(SettingsRepositoryImpl()) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is SettingsOneTimeEvent.Dismiss -> navigator.pop()
                }
            }
        }

        SwipeableDismissWrapper(
            onDismiss = { navigator.pop() }
        ) {
            SettingsContent(
                state = state,
                onEvent = screenModel::onEvent,
            )
        }
    }
}