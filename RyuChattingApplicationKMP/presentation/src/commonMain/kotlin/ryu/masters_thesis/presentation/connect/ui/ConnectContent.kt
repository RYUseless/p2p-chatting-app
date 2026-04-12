package ryu.masters_thesis.presentation.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.component.ui.FindRoomItem
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.implementation.ConnectState

@Composable
fun ConnectContent(
    state: ConnectState,
    onEvent: (ConnectEvent) -> Unit,
    // TODO DUMMY: isDark nahradit AppSettings/Theme systémem až bude dostupný
    isDark: Boolean = false,
) {
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val surfaceColor    = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
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
        // TODO DUMMY: překlady hardcoded, nahradit až bude i18n dostupné
        Text(
            text = "Connect to a room",
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = if (state.isSearching)
                "Time remaining: ${state.remainingSeconds}s"
            else
                "Search ended",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.scannedDevices.isEmpty()) {
            Text(
                // TODO DUMMY: překlad hardcoded
                text = "No rooms found",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.Red
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Text(
                // TODO DUMMY: překlad hardcoded
                text = "Available rooms: ${state.scannedDevices.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = textColor
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(backgroundColor)
            ) {
                items(state.scannedDevices) { device ->
                    FindRoomItem(
                        name = device.displayName,
                        mac = device.address,
                        textColor = textColor,
                        surfaceColor = surfaceColor,
                        onClick = { onEvent(ConnectEvent.DeviceClicked(device)) }
                    )
                }
            }
        }

        Button(
            onClick = { onEvent(ConnectEvent.DismissClicked) },
            colors = buttonColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
        ) {
            // TODO DUMMY: překlad hardcoded
            Text("Close")
        }
    }

    // Dialog pro připojení k vybranému zařízení
    state.selectedDevice?.let { device ->
        JoinRoomDialog(
            device = device,
            state = state,
            isDark = isDark,
            onEvent = onEvent,
        )
    }
}