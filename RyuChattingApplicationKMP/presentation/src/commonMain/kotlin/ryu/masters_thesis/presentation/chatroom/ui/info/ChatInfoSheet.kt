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
    isDark: Boolean,
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White       else Color.Black
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
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
                // TODO DUMMY: překlad hardcoded
                text  = "Room info",
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
            )

            // QR sekce – reuse z create modulu
            state.currentRoomId?.let { roomId ->
                ChatInfoQrSection(
                    roomId      = roomId,
                    showQrDialog = state.showQrDialog,
                    onEvent     = onEvent,
                    isDark      = isDark,
                    textColor   = textColor,
                    buttonColors = buttonColors,
                )
                HorizontalDivider()
            }

            // Barva chatu
            ChatInfoColorSection(
                currentColorHex = state.chatColorHex,
                onEvent         = onEvent,
                textColor       = textColor,
            )
            HorizontalDivider()

            // Přezdívky
            ChatInfoNicknameSection(
                nicknames    = state.nicknames,
                onEvent      = onEvent,
                textColor    = textColor,
                buttonColors = buttonColors,
            )
            HorizontalDivider()

            // Whitelist
            ChatInfoWhitelistSection(
                whitelist  = state.whitelist,
                nicknames  = state.nicknames,
                onEvent    = onEvent,
                textColor  = textColor,
            )
            HorizontalDivider()

            // Close
            Button(
                onClick  = { onEvent(ChatRoomEvent.InfoSheetDismissed) },
                colors   = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                // TODO DUMMY: překlad hardcoded
                Text("Close")
            }
        }
    }
}