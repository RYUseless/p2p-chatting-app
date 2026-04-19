package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
// Reuse QrCodeDialog z create modulu
import ryu.masters_thesis.presentation.create.ui.QrCodeDialog

@Composable
fun ChatInfoQrSection(
    roomId: String,
    password: String,
    showQrDialog: Boolean,
    onEvent: (ChatRoomEvent) -> Unit,
) {
    val textColor    = MaterialTheme.colorScheme.onSurface
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text  = "Room QR Code",
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick  = { onEvent(ChatRoomEvent.ShowQrClicked) },
            colors   = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show QR Code")
        }

        if (showQrDialog) {
            QrCodeDialog(
                roomId    = roomId,
                password  = password,
                onDismiss = { onEvent(ChatRoomEvent.QrDialogDismissed) },
            )
        }
    }
}