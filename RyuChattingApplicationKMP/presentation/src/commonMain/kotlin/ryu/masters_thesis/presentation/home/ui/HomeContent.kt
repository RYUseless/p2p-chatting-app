package ryu.masters_thesis.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.core.configuration.getTranslations
import ryu.masters_thesis.presentation.component.ui.ChatRoomItem
import ryu.masters_thesis.presentation.component.ui.LocalAppSettings
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.implementation.HomeState

@Composable
fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    val settings          = LocalAppSettings.current
    val t                 = getTranslations(settings.language)
    val backgroundColor   = MaterialTheme.colorScheme.background
    val surfaceColor      = MaterialTheme.colorScheme.surface
    val textColor         = MaterialTheme.colorScheme.onSurface
    val borderColor       = MaterialTheme.colorScheme.outlineVariant
    val buttonColors      = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor   = MaterialTheme.colorScheme.onPrimary,
    )

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                Text(
                    text     = "~ Ryu's chatting app ~",
                    style    = MaterialTheme.typography.headlineLarge,
                    color    = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick  = { onEvent(HomeEvent.SettingsClicked) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Settings,
                        contentDescription = t.settingsTitle,
                        tint               = textColor,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Text(
                    text     = t.savedChats,
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = textColor
                )
                HorizontalDivider(color = borderColor)

                when {
                    state.isLoading -> {
                        Box(
                            modifier         = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = textColor)
                        }
                    }
                    state.chatRooms.isEmpty() -> {
                        Box(
                            modifier         = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = t.noChatsAvailable,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundColor)
                        ) {
                            items(state.chatRooms) { room ->
                                ChatRoomItem(
                                    room         = room,
                                    textColor    = textColor,
                                    surfaceColor = surfaceColor,
                                    onClick      = { onEvent(HomeEvent.RoomClicked(room)) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = { onEvent(HomeEvent.ConnectClicked) },
                    modifier = Modifier.weight(1f),
                    colors   = buttonColors
                ) {
                    Text(t.buttonConnect, maxLines = 1)
                }
                Button(
                    onClick  = { onEvent(HomeEvent.CreateClicked) },
                    modifier = Modifier.weight(1f),
                    colors   = buttonColors
                ) {
                    Text(t.buttonCreate, maxLines = 1)
                }
            }
        }
    }
}