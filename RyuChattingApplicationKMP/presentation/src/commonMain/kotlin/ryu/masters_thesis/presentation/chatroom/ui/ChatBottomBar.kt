package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatBottomBar(
    messageInput: String,
    onEvent: (ChatRoomEvent) -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val iconColor    = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = { onEvent(ChatRoomEvent.AttachFileClicked) }) {
            Icon(Icons.Default.AddCircle, contentDescription = "Příloha", tint = iconColor)
        }

        OutlinedTextField(
            value = messageInput,
            onValueChange = { onEvent(ChatRoomEvent.MessageInputChanged(it)) },
            placeholder = { Text("Zpráva...") },
            trailingIcon = {
                IconButton(onClick = { onEvent(ChatRoomEvent.EmojiMenuToggled) }) {
                    Text("😊")
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
        )

        IconButton(
            onClick = { onEvent(ChatRoomEvent.SendMessageClicked) },
            enabled = messageInput.isNotBlank()
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "Odeslat",
                tint = if (messageInput.isNotBlank()) iconColor else iconColor.copy(alpha = 0.3f)
            )
        }
    }
}