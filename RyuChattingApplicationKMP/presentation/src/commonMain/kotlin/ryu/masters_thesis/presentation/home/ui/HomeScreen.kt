package ryu.masters_thesis.presentation.home.ui

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.*
//Ryuoviny
import ryu.masters_thesis.presentation.connect.ui.ConnectScreen
import ryu.masters_thesis.presentation.create.ui.CreateScreen
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.domain.HomeOneTimeEvent
import ryu.masters_thesis.presentation.home.implementation.HomeRepositoryImpl
import ryu.masters_thesis.presentation.home.implementation.HomeScreenModel
import ryu.masters_thesis.presentation.settings.ui.SettingsScreen

// Lehký entry point – žádné UI, pouze definice screenu a propojení závislostí
object HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // TODO DUMMY: HomeRepositoryImpl injektovat přes DI až bude k dispozici
        val screenModel = rememberScreenModel { HomeScreenModel(HomeRepositoryImpl()) }
        val state by screenModel.state.collectAsState()

        // Konzumace jednorázových eventů
        LaunchedEffect(Unit) {
            screenModel.oneTimeEvents.collect { event ->
                when (event) {
                    is HomeOneTimeEvent.Navigate -> {
                        when (event.event) {
                            is HomeEvent.ConnectClicked -> navigator.push(ConnectScreen)
                            is HomeEvent.CreateClicked -> navigator.push(CreateScreen)
                            is HomeEvent.SettingsClicked -> navigator.push(SettingsScreen)
                            //dummies:
                            is HomeEvent.RoomClicked     -> { /* navigator.push(ChatRoomScreen(event.event.room.id)) */ } //dummy
                        }
                    }
                    is HomeOneTimeEvent.ShowError -> Unit // zpracováno v HomeContent přes snackbar
                }
            }
        }

        HomeContent(
            state = state,
            onEvent = screenModel::onEvent,
        )
    }
}