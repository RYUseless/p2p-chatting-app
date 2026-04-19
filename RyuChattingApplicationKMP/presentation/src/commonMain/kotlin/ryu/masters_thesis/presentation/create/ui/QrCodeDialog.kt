package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ryu.masters_thesis.presentation.component.ui.QrCodeImage

@Composable
fun QrCodeDialog(
    roomId:    String,
    password:  String,
    onDismiss: () -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor       = MaterialTheme.colorScheme.onSurface
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    val qrContent = "$roomId|$password"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "Room QR Code",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
            Spacer(modifier = Modifier.height(16.dp))

            QrCodeImage(
                content = qrContent,
                sizeDp  = 220,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "ID: $roomId",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick  = onDismiss,
                colors   = buttonColors,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        }
    }
}