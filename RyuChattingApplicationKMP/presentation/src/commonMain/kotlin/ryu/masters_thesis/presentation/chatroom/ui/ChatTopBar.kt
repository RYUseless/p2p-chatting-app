package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatTopBar(
    roomName: String,
    isServer: Boolean,
    isConnected: Boolean,
    isVerified: Boolean,
    isLeaving: Boolean,
    onEvent: (ChatRoomEvent) -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor    = MaterialTheme.colorScheme.onSurface

    val statusText = when {
        isLeaving && isServer    -> "\uD83D\uDDFF Odpojuji se..."
        isServer && !isConnected -> "\uD83D\uDDFF Čeká se na připojení klienta..."
        isServer && !isVerified  -> "⚠️ Ověřování spojení..."
        !isServer && !isVerified -> "⚠️ Ověřování hesla..."
        else                     -> null
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onEvent(ChatRoomEvent.BackClicked) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                text  = roomName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
            Row {
                IconButton(onClick = { onEvent(ChatRoomEvent.SearchClicked) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = textColor)
                }
                IconButton(onClick = { onEvent(ChatRoomEvent.InfoClicked) }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = textColor)
                }
            }
        }

        if (statusText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}