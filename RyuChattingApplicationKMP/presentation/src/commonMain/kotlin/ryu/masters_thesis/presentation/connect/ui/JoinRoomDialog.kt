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
import ryu.masters_thesis.presentation.component.ui.QrCodeScanner
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.domain.ScannedDeviceUiModel
import ryu.masters_thesis.presentation.connect.implementation.ConnectState

@Composable
fun JoinRoomDialog(
    device:  ScannedDeviceUiModel,
    state:   ConnectState,
    onEvent: (ConnectEvent) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor       = MaterialTheme.colorScheme.onSurface
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    var passwordInput by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onEvent(ConnectEvent.DialogDismissed) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "Connect to: ${device.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
            Text(
                text  = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.needsPassword) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting...", color = textColor)
            }

            if (state.needsPassword) {
                if (showQrScanner) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCodeScanner(
                            onResult = { scanned ->
                                onEvent(ConnectEvent.QrScanned(scanned))
                                showQrScanner = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showQrScanner = false }) {
                        Text("Enter manually", color = textColor)
                    }
                } else {
                    OutlinedTextField(
                        value                = passwordInput,
                        onValueChange        = { passwordInput = it },
                        label                = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError              = state.passwordError != null,
                        supportingText       = {
                            state.passwordError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick  = { showQrScanner = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Scan QR code", color = textColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick  = { onEvent(ConnectEvent.DialogDismissed) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel", color = textColor)
                }

                if (state.needsPassword) {
                    Button(
                        onClick  = { onEvent(ConnectEvent.PasswordSubmitted(passwordInput)) },
                        enabled  = passwordInput.isNotBlank(),
                        colors   = buttonColors,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}