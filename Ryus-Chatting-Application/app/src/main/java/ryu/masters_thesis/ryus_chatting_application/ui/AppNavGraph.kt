package ryu.masters_thesis.ryus_chatting_application.ui

//import androidx.compose.animation.core.Animatable
//import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
//import androidx.compose.animation.slideInHorizontally
//import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
//import androidx.compose.foundation.gestures.detectHorizontalDragGestures
//import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.ui.screens.*
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.launch
//import kotlin.math.abs
import kotlin.math.roundToInt
//import kotlinx.coroutines.cancelChildren


sealed class Screen(val route: String) {
    object Start    : Screen("start")
    object Connect  : Screen("connect")
    object Create   : Screen("create")
    object Settings : Screen("settings")

    //rozdilne, protoze route je dynamicka, lebo jmeno roomky se meni etc.
    object ChatRoom : Screen("chat_room/{roomName}") {
        fun createRoute(roomName: String) = "chat_room/$roomName"
    }
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    var localSettings by remember { mutableStateOf(settings) }
    LaunchedEffect(settings) { localSettings = settings }

    val handleChange: (AppSettings) -> Unit = {
        localSettings = it
        onSettingsChange(it)
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Start.route,
        modifier = modifier
    ) {
        composable(Screen.Start.route) {
            StartScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                settings = localSettings,
            )
        }

        dialog(
            route = Screen.Connect.route,
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings ) {
                ConnectScreen(
                    onDismiss = { navController.popBackStack() },
                    settings = localSettings
                )
            }
        }

        /*
        -- SETTINGS --
         */
        dialog(
            route = Screen.Settings.route,
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings ) {
                SettingsScreen(
                    onDismiss = { navController.popBackStack() },
                    settings = localSettings,
                    onSettingsChange = handleChange
                )
            }
        }
        /*
        -- CREATE SCREEN --
        */
        dialog(
            route = Screen.Create.route,
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false)
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings ) {
                CreateScreen(
                    onDismiss = { navController.popBackStack() },
                    settings = localSettings
                )
            }
        }

        //CHAT ROOM -----------------------------------------:
        composable(
            route = Screen.Start.route,
            exitTransition = {
                when (targetState.destination.route) {
                    Screen.ChatRoom.route -> fadeOut(animationSpec = tween(200))
                    else -> null
                }
            },
            popEnterTransition = {
                when (initialState.destination.route) {
                    Screen.ChatRoom.route -> fadeIn(animationSpec = tween(200))
                    else -> null
                }
            }
        ) {
            StartScreen(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                settings = localSettings,
            )
        }

        // ChatRoom — žádné slide, jen fade
        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(navArgument("roomName") { type = NavType.StringType }),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) { backStackEntry ->
            val roomName = backStackEntry.arguments?.getString("roomName") ?: ""
            ChatRoomScreen(
                roomName = roomName,
                onBack = { navController.popBackStack() },
                onInfo = { },
                settings = localSettings
            )
        }


    }
}

// SWIPE TO THE SIDE MAGICO
// issue with jerk motion při přechodu z swipe na fadeaway animaci → fixed?

enum class DismissValue { Center, DismissedLeft, DismissedRight }

@Composable
private fun SwipeableDismissDialog(
    onDismiss: () -> Unit,
    settings: AppSettings,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = settings.isDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val density = LocalDensity.current

    val state = remember {
        AnchoredDraggableState(
            initialValue = DismissValue.Center,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 300.dp.toPx() } },
            snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(state.settledValue) {
        if (state.settledValue == DismissValue.DismissedLeft ||
            state.settledValue == DismissValue.DismissedRight
        ) {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                state.updateAnchors(
                    DraggableAnchors {
                        DismissValue.DismissedLeft  at -size.width.toFloat()
                        DismissValue.Center         at 0f
                        DismissValue.DismissedRight at  size.width.toFloat()
                    }
                )
            }
            .offset {
                IntOffset(state.requireOffset().roundToInt(), 0)
            }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(backgroundColor)
                .navigationBarsPadding()
                // padding pro outline dialogu, pozdeji premigrovat do configu
                .padding(horizontal = 12.dp, vertical = 12.dp), // horizontal: vodorovny, vertical: stojaty
            content = content
        )
    }
}