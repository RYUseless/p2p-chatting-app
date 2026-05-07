package ryu.masters_thesis.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.core.configuration.getTranslations
import ryu.masters_thesis.data.vault.domain.RoomRole
import ryu.masters_thesis.data.vault.domain.RoomSummary
import ryu.masters_thesis.presentation.component.ui.LocalAppSettings
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.implementation.HomeState

import ryu.masters_thesis.presentation.component.implementation.formatTimestamp

@Composable
fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    val settings        = LocalAppSettings.current
    val t               = getTranslations(settings.language)
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor    = MaterialTheme.colorScheme.surface
    val textColor       = MaterialTheme.colorScheme.onSurface
    val borderColor     = MaterialTheme.colorScheme.outlineVariant
    val buttonColors    = ButtonDefaults.buttonColors(
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
                        ) { CircularProgressIndicator(color = textColor) }
                    }
                    state.rooms.isEmpty() -> {
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
                            items(
                                items = state.rooms,
                                key   = { it.hashedRoomId },
                            ) { room ->
                                RoomSummaryItem(
                                    room         = room,
                                    textColor    = textColor,
                                    surfaceColor = surfaceColor,
                                    borderColor  = borderColor,
                                    onClick      = { onEvent(HomeEvent.RoomClicked(room)) },
                                    onDelete = { onEvent(HomeEvent.DeleteRoomClicked(room.roomName)) }                                )
                                HorizontalDivider(color = borderColor)
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
                ) { Text(t.buttonConnect, maxLines = 1) }
                Button(
                    onClick  = { onEvent(HomeEvent.CreateClicked) },
                    modifier = Modifier.weight(1f),
                    colors   = buttonColors
                ) { Text(t.buttonCreate, maxLines = 1) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomSummaryItem(
    room:         RoomSummary,
    textColor:    androidx.compose.ui.graphics.Color,
    surfaceColor: androidx.compose.ui.graphics.Color,
    borderColor:  androidx.compose.ui.graphics.Color,
    onClick:      () -> Unit,
    onDelete:     () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state             = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector        = Icons.Filled.Delete,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        Surface(
            onClick = onClick,
            color   = surfaceColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = room.roomName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    room.lastMessage?.let { msg ->
                        Text(
                            text  = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        if (!room.isSaved) {
                            UnsavedBadge()
                        }
                        RoleBadge(role = room.role)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = formatRoomTimestamp(room.lastTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: RoomRole) {
    val (label, containerColor, contentColor) = when (role) {
        RoomRole.SERVER -> Triple(
            "SERVER",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        RoomRole.CLIENT -> Triple(
            "CLIENT",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Surface(
        shape         = RoundedCornerShape(4.dp),
        color         = containerColor,
        contentColor  = contentColor,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun UnsavedBadge() {
    Surface(
        shape        = RoundedCornerShape(4.dp),
        color        = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text     = "TEMP",
            style    = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatRoomTimestamp(epochMs: Long): String = formatTimestamp(epochMs)