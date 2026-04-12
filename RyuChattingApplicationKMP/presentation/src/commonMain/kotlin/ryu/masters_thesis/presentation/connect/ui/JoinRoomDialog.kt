package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel
import ryu.masters_thesis.presentation.connect.implementation.ConnectState

@Composable
fun JoinRoomDialog(
    device: ScannedDeviceUiModel,
    state: ConnectState,
    isDark: Boolean,
    onEvent: (ConnectEvent) -> Unit,
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White       else Color.Black
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    var passwordInput by remember { mutableStateOf("") }
    // QR scanner je Android-only (kamera) – řeší se na UI vrstvě přes callback
    // TODO DUMMY: showQrScanner logika přesunuta na ConnectContent level až bude platforma dostupná
    var showQrScanner by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onEvent(ConnectEvent.DialogDismissed) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connect to: ${device.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Connecting indicator
            if (!state.needsPassword) {
                CircularProgressIndicator(color = if (isDark) Color.White else Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting...", color = textColor)
            }

            // Password input
            if (state.needsPassword) {
                if (showQrScanner) {
                    // TODO DUMMY: QR scanner je Android-only, nahradit expect/actual až bude :core dostupný
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR Scanner\n(TODO: platform impl)",
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showQrScanner = false }) {
                        Text("Enter manually", color = textColor)
                    }
                } else {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = state.passwordError != null,
                        supportingText = {
                            state.passwordError?.let {
                                Text(it, color = Color.Red)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // TODO DUMMY: překlad hardcoded
                        Text("Scan QR code", color = textColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onEvent(ConnectEvent.DialogDismissed) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = textColor)
                }

                if (state.needsPassword) {
                    Button(
                        onClick = { onEvent(ConnectEvent.PasswordSubmitted(passwordInput)) },
                        enabled = passwordInput.isNotBlank(),
                        colors = buttonColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}