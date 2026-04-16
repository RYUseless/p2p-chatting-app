package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatInfoWhitelistSection(
    whitelist: List<String>,
    nicknames: Map<String, String>,
    onEvent: (ChatRoomEvent) -> Unit,
    // textColor ← odebráno
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Whitelist",
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (nicknames.isEmpty()) {
            Text(
                text  = "No users connected",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.5f)
            )
        } else {
            nicknames.keys.forEach { userId ->
                val isWhitelisted = userId in whitelist
                val displayName   = nicknames[userId] ?: userId
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(text = displayName, color = textColor)
                    Switch(
                        checked         = isWhitelisted,
                        onCheckedChange = { onEvent(ChatRoomEvent.WhitelistToggled(userId)) }
                    )
                }
            }
        }
    }
}