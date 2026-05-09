package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

@Composable
fun ChatInfoSavedSection(
    isSaved:  Boolean,
    isServer: Boolean,
    onEvent:  (ChatRoomEvent) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Room status",
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier          = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = {},
                label   = {
                    Text(
                        text  = if (isSaved) "✓ Saved" else "⏳ Temp",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
            )
            if (isServer && !isSaved) {
                Button(
                    onClick = { onEvent(ChatRoomEvent.MarkRoomSaved) },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Mark as saved")
                }
            }
        }
    }
}