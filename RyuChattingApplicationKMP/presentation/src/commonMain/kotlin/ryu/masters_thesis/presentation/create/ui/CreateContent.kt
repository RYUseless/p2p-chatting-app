package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.create.domain.CreateEvent
import ryu.masters_thesis.presentation.create.implementation.CreateState

@Composable
fun CreateContent(
    state: CreateState,
    onEvent: (CreateEvent) -> Unit,
    // TODO DUMMY: isDark nahradit Theme systémem až bude dostupný
    isDark: Boolean = false,
) {
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor       = if (isDark) Color.White       else Color.Black
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(bottom = 32.dp)
    ) {
        // TODO DUMMY: překlad hardcoded
        Text(
            text = "Create a room",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Chatroom Name
            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget("Chatroom Name", textColor)
                OutlinedTextField(
                    value = state.roomName,
                    onValueChange = { onEvent(CreateEvent.RoomNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Room Password
            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget("Room Password", textColor)
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(CreateEvent.PasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.passwordError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                state.passwordError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Varování pokud chybí BT permissions
            // TODO DUMMY: hasPermissions vždy true, nahradit až bude :core dostupný
            if (!state.hasPermissions) {
                Text(
                    text = "Bluetooth permissions required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Vytvoř místnost
            Button(
                onClick = { onEvent(CreateEvent.CreateRoomClicked) },
                enabled = state.password.isNotEmpty() && !state.serverStarted && state.hasPermissions,
                colors = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                // TODO DUMMY: překlad hardcoded
                Text("Create room")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QR kód
            Button(
                onClick = { onEvent(CreateEvent.ShowQrClicked) },
                enabled = state.password.isNotEmpty() && state.hasPermissions,
                colors = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                // TODO DUMMY: překlad hardcoded
                Text("Show QR code")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onEvent(CreateEvent.DismissClicked) },
            colors = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            // TODO DUMMY: překlad hardcoded
            Text("Close")
        }
    }

    // QR dialog
    if (state.showQrDialog && state.currentRoomId != null) {
        QrCodeDialog(
            roomId = state.currentRoomId,
            password = state.password,
            isDark = isDark,
            onDismiss = { onEvent(CreateEvent.QrDialogDismissed) },
        )
    }
}