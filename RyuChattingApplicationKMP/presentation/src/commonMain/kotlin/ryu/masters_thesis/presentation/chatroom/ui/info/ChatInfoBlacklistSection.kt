package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatInfoBlacklistSection(
    blacklist:        List<String>,
    nicknames:        Map<String, String>,
    connectedUserIds: List<String>,
    onEvent:          (ChatRoomEvent) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Blacklist",
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (connectedUserIds.isEmpty()) {
            Text(
                text  = "No users connected",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.5f),
            )
        } else {
            connectedUserIds.forEach { userId ->
                val isBlocked   = userId in blacklist
                val displayName = nicknames[userId] ?: userId
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(text = displayName, color = textColor)
                    Switch(
                        checked         = isBlocked,
                        onCheckedChange = { onEvent(ChatRoomEvent.BlacklistToggled(userId)) },
                    )
                }
            }
        }
    }
}