package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomState

@Composable
fun ChatInfoSheet(
    state: ChatRoomState,
    onEvent: (ChatRoomEvent) -> Unit,
    // isDark ← odebráno
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor       = MaterialTheme.colorScheme.onSurface
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    Dialog(onDismissRequest = { onEvent(ChatRoomEvent.InfoSheetDismissed) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text  = "Room info",
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
            )

            state.currentRoomId?.let { roomId ->
                ChatInfoQrSection(
                    roomId       = roomId,
                    showQrDialog = state.showQrDialog,
                    onEvent      = onEvent,
                    // isDark, textColor, buttonColors ← odebráno
                )
                HorizontalDivider()
            }

            ChatInfoColorSection(
                currentColorHex = state.chatColorHex,
                onEvent         = onEvent,
                // textColor ← odebráno
            )
            HorizontalDivider()

            ChatInfoNicknameSection(
                nicknames = state.nicknames,
                onEvent   = onEvent,
                // textColor, buttonColors ← odebráno
            )
            HorizontalDivider()

            ChatInfoWhitelistSection(
                whitelist = state.whitelist,
                nicknames = state.nicknames,
                onEvent   = onEvent,
                // textColor ← odebráno
            )
            HorizontalDivider()

            Button(
                onClick  = { onEvent(ChatRoomEvent.InfoSheetDismissed) },
                colors   = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}