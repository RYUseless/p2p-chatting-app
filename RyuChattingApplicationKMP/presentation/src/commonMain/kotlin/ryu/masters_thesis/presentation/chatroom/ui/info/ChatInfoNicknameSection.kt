package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatInfoNicknameSection(
    nicknames:        Map<String, String>,
    connectedUserIds: List<String>,
    onEvent:          (ChatRoomEvent) -> Unit,
) {
    val textColor    = MaterialTheme.colorScheme.onSurface
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Nicknames",
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
                var input by remember(userId) { mutableStateOf(nicknames[userId] ?: "") }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value         = input,
                        onValueChange = { input = it },
                        label         = { Text(userId) },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                    )
                    Button(
                        onClick  = { onEvent(ChatRoomEvent.NicknameChanged(userId, input)) },
                        colors   = buttonColors,
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Save") }
                }
            }
        }
    }
}