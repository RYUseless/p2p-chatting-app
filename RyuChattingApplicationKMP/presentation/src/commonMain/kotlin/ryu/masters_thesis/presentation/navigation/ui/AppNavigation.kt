package ryu.masters_thesis.presentation.navigation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import ryu.masters_thesis.presentation.home.ui.HomeScreen

//yeye beta kekel, ale je to co to je
@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun AppNavigation() {
    Navigator(
        screen = HomeScreen,
        disposeBehavior = NavigatorDisposeBehavior(disposeSteps = false),
    ) { navigator ->
        Box(modifier = Modifier.fillMaxSize()) {

            if (navigator.size > 1) {
                HomeScreen.Content()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }

            navigator.lastItem.Content()
        }
    }
}