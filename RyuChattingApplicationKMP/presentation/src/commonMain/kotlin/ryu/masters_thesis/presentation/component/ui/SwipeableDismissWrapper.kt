package ryu.masters_thesis.presentation.component.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import kotlin.math.roundToInt

enum class DismissValue { Center, DismissedLeft, DismissedRight }

// Sdílený swipe wrapper – obaluje ConnectScreen, CreateScreen, SettingsScreen
// onDismiss = navigator.pop() z Voyageru
@Composable
fun SwipeableDismissWrapper(
    onDismiss: () -> Unit,
    // TODO DUMMY: isDark nahradit Theme systémem až bude dostupný
    isDark: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
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
                        DismissValue.DismissedLeft  at -size.width.toFloat()
                        DismissValue.Center         at 0f
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