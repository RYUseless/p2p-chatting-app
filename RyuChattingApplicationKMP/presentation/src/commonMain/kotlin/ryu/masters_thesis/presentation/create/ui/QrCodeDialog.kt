package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun QrCodeDialog(
    roomId: String,
    password: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White       else Color.Black

    // TODO DUMMY: QRCodeGenerator bude v :core modulu, zatím placeholder
    // formát pro QR: "roomId|password"
    // val qrContent = "$roomId|$password"
    // val qrBitmap = QRCodeGenerator.generate(content = qrContent, sizePx = 512)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                // TODO DUMMY: překlad hardcoded
                text = "Room QR Code",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            // TODO DUMMY: až bude QRCodeGenerator z :core dostupný, nahradit Image(bitmap = ...)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QR placeholder\n$roomId",
                    color = Color.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ID: $roomId",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.White else Color.Black,
                    contentColor   = if (isDark) Color.Black else Color.White
                )
            ) {
                // TODO DUMMY: překlad hardcoded
                Text("Close")
            }
        }
    }
}