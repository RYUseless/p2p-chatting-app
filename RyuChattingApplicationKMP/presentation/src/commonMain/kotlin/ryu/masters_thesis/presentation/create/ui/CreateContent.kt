package ryu.masters_thesis.presentation.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.core.configuration.getTranslations
import ryu.masters_thesis.presentation.component.ui.LocalAppSettings
import ryu.masters_thesis.presentation.create.domain.CreateEvent
import ryu.masters_thesis.presentation.create.implementation.CreateState

@Composable
fun CreateContent(
    state: CreateState,
    onEvent: (CreateEvent) -> Unit,
) {
    val settings              = LocalAppSettings.current
    val textValueTranslation  = getTranslations(settings.language)
    val backgroundColor       = MaterialTheme.colorScheme.background
    val textColor             = MaterialTheme.colorScheme.onSurface
    val buttonColors          = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text      = textValueTranslation.createTitle,
            style     = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color     = textColor,
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget(textValueTranslation.createRoomName)
                OutlinedTextField(
                    value         = state.roomName,
                    onValueChange = { onEvent(CreateEvent.RoomNameChanged(it)) },
                    modifier      = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget(textValueTranslation.createRoomPassword)
                OutlinedTextField(
                    value                = state.password,
                    onValueChange        = { onEvent(CreateEvent.PasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError              = state.passwordError != null,
                    modifier             = Modifier.fillMaxWidth()
                )
                state.passwordError?.let {
                    Text(
                        text     = it,
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (!state.hasPermissions) {
                Text(
                    text      = textValueTranslation.bluetoothPermissionsRequired,
                    color     = MaterialTheme.colorScheme.error,
                    style     = MaterialTheme.typography.bodySmall,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick  = { onEvent(CreateEvent.CreateRoomClicked) },
                enabled  = state.password.isNotEmpty() && !state.serverStarted && state.hasPermissions,
                colors   = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(textValueTranslation.createRoomCreate)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick  = { onEvent(CreateEvent.ShowQrClicked) },
                enabled  = state.password.isNotEmpty() && state.hasPermissions,
                colors   = buttonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(textValueTranslation.createRoomQR)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick  = { onEvent(CreateEvent.DismissClicked) },
            colors   = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(textValueTranslation.close)
        }
    }

    if (state.showQrDialog && state.currentRoomId != null) {
        QrCodeDialog(
            roomId    = state.currentRoomId,
            password  = state.password,
            onDismiss = { onEvent(CreateEvent.QrDialogDismissed) },
        )
    }
}