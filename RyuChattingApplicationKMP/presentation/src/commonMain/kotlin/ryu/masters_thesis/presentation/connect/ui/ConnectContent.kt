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
import ryu.masters_thesis.core.configuration.getTranslations
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data.NeighbouringDevice
import ryu.masters_thesis.presentation.component.ui.FindRoomItem
import ryu.masters_thesis.presentation.component.ui.LocalAppSettings
import ryu.masters_thesis.presentation.connect.domain.ConnectEvent
import ryu.masters_thesis.presentation.connect.implementation.ConnectState

@Composable
fun ConnectContent(
    state: ConnectState,
    onEvent: (ConnectEvent) -> Unit,
) {
    val settings             = LocalAppSettings.current
    val textValueTranslation = getTranslations(settings.language)
    val backgroundColor      = MaterialTheme.colorScheme.background
    val surfaceColor         = MaterialTheme.colorScheme.surface
    val textColor            = MaterialTheme.colorScheme.onSurface
    val buttonColors         = ButtonDefaults.buttonColors(
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
            text      = textValueTranslation.connectTitle,
            style     = MaterialTheme.typography.headlineSmall,
            color     = textColor,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = if (state.isSearching)
                //search probiha → t - n time
                textValueTranslation.connectTimeRemaining.replace("%d",
                    state.remainingSeconds.toString())
            //search time dobehl na t-0
            else textValueTranslation.connectSearchEnded,
            style     = MaterialTheme.typography.titleSmall,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            color     = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.scannedDevices.isEmpty()) {
            Text(
                text     = textValueTranslation.connectNoRooms,
                style    = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.Red
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Text(
                text = textValueTranslation.connectAvailableRooms.replace("%d", state.scannedDevices.size.toString()),
                style    = MaterialTheme.typography.titleMedium,
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
                        name         = device.displayName,
                        mac          = device.address,
                        textColor    = textColor,
                        surfaceColor = surfaceColor,
                        onClick      = { onEvent(ConnectEvent.DeviceClicked(device)) }
                    )
                }
            }
        }
        //new if:Else shittemitte
        if (state.meshNodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text     = "Mesh Peers (${state.meshNodes.size})",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color    = textColor,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(backgroundColor)
            ) {
                items(state.meshNodes) { node ->
                    MeshPeerItem(
                        node     = node,
                        hopCount = state.meshRoutes.getBestPath(node.neighbourBluetoothAddress)?.totalHopCount,
                        textColor    = textColor,
                        surfaceColor = surfaceColor,
                        onClick  = {
                            onEvent(ConnectEvent.MeshPeerClicked(
                                address = node.neighbourBluetoothAddress,
                                name    = node.neighbourNodeName,
                            ))
                        }
                    )
                }
            }
        }
        if (state.canReconnect) {
            Button(
                onClick  = { onEvent(ConnectEvent.ReconnectClicked) },
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor   = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp)
            ) {
                if (state.connectionState == ConnectionState.RECONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                //reconnect
                Text(textValueTranslation.close)
            }
        }

        Button(
            onClick  = { onEvent(ConnectEvent.DismissClicked) },
            colors   = buttonColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
        ) {
            Text(textValueTranslation.close)
        }
    }

    state.selectedDevice?.let { device ->
        JoinRoomDialog(
            device  = device,
            state   = state,
            onEvent = onEvent,
        )
    }
}

//new func
@Composable
private fun MeshPeerItem(
    node         : NeighbouringDevice,
    hopCount     : Int?,
    textColor    : androidx.compose.ui.graphics.Color,
    surfaceColor : androidx.compose.ui.graphics.Color,
    onClick      : () -> Unit,
) {
    val badge = if (hopCount == 1) "DIRECT" else "RELAY ${hopCount ?: "?"}hops"
    val badgeColor = if (hopCount == 1) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

    FindRoomItem(
        name         = "${node.neighbourNodeName ?: node.neighbourBluetoothAddress} [$badge]",
        mac          = node.neighbourBluetoothAddress,
        textColor    = if (node.isNeighbourAlive) textColor else textColor.copy(alpha = 0.4f),
        surfaceColor = surfaceColor,
        onClick      = onClick,
    )
}