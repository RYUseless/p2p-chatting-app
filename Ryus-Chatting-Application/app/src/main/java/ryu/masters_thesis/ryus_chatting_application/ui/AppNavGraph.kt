package ryu.masters_thesis.ryus_chatting_application.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
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
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothController
import ryu.masters_thesis.ryus_chatting_application.ui.screens.*
import kotlin.math.roundToInt

sealed class Screen(val route: String) {
    object Start : Screen("start")
    object Connect : Screen("connect")
    object Create : Screen("create")
    object Settings : Screen("settings")
    object ChatRoom : Screen("chat_room/{roomName}") {
        fun createRoute(roomName: String) = "chat_room/$roomName"
    }
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    bluetoothController: BluetoothController
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

        // CONNECT
        dialog(
            route = Screen.Connect.route,
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings
            ) {
                ConnectScreen(
                    onDismiss = { navController.popBackStack() },
                    settings = localSettings,
                    bluetoothController = bluetoothController,
                    onNavigateToChat = { roomId ->
                        navController.navigate(Screen.ChatRoom.createRoute(roomId)) {
                            popUpTo(Screen.Start.route)
                        }
                    }
                )
            }
        }

        // SETTINGS
        dialog(
            route = Screen.Settings.route,
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings
            ) {
                SettingsScreen(
                    onDismiss = { navController.popBackStack() },
                    settings = localSettings,
                    onSettingsChange = handleChange
                )
            }
        }

        // CREATE
        dialog(
            route = Screen.Create.route,
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            SwipeableDismissDialog(
                onDismiss = { navController.popBackStack() },
                settings = localSettings
            ) {
                CreateScreen(
                    onDismiss = { navController.popBackStack() },
                    onNavigateToChat = { roomId ->
                        navController.navigate(Screen.ChatRoom.createRoute(roomId)) {
                            popUpTo(Screen.Start.route)
                        }
                    },
                    settings = localSettings,
                    bluetoothController = bluetoothController
                )
            }
        }

        // CHATROOM
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
                settings = localSettings,
                bluetoothController = bluetoothController
            )
        }
    }
}

enum class DismissValue { Center, DismissedLeft, DismissedRight }

@Composable
private fun SwipeableDismissDialog(
    onDismiss: () -> Unit,
    settings: AppSettings,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = settings.isDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White

    val state = remember {
        AnchoredDraggableState(initialValue = DismissValue.Center)
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

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
                        DismissValue.DismissedLeft at -size.width.toFloat()
                        DismissValue.Center at 0f
                        DismissValue.DismissedRight at size.width.toFloat()
                    }
                )
            }
            .offset {
                IntOffset(state.requireOffset().roundToInt(), 0)
            }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                flingBehavior = flingBehavior
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
                .padding(horizontal = 12.dp, vertical = 12.dp),
            content = content
        )
    }
}