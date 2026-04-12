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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.component.ui.ChatRoomItem
import ryu.masters_thesis.presentation.home.domain.HomeEvent
import ryu.masters_thesis.presentation.home.implementation.HomeState

@Composable
fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    // TODO DUMMY: isDark nahradit AppSettings/Theme systémem až bude dostupný
    isDark: Boolean = false,
) {
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val surfaceColor    = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor       = if (isDark) Color.White       else Color.Black
    val borderColor     = if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD)
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // Jednorázové chybové eventy
    LaunchedEffect(Unit) {
        // TODO: napojit na screenModel.oneTimeEvents přes HomeScreen
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundColor,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header – Logo + Settings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                Text(
                    text = "Logo",
                    style = MaterialTheme.typography.headlineLarge,
                    color = textColor,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(
                    onClick = { onEvent(HomeEvent.SettingsClicked) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Seznam chat roomů
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Text(
                    // TODO DUMMY: překlady hardcoded, nahradit až bude i18n dostupné
                    text = "Saved chats",
                    style = MaterialTheme.typography.titleMedium,
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
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = textColor)
                        }
                    }
                    state.chatRooms.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                // TODO DUMMY: překlad hardcoded
                                text = "No chats available",
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
                                    room = room,
                                    textColor = textColor,
                                    surfaceColor = surfaceColor,
                                    onClick = { onEvent(HomeEvent.RoomClicked(room)) }
                                )
                            }
                        }
                    }
                }
            }

            // Spodní tlačítka
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onEvent(HomeEvent.ConnectClicked) },
                    modifier = Modifier.weight(1f),
                    colors = buttonColors
                ) {
                    // TODO DUMMY: překlad hardcoded
                    Text("Connect", maxLines = 1)
                }
                Button(
                    onClick = { onEvent(HomeEvent.CreateClicked) },
                    modifier = Modifier.weight(1f),
                    colors = buttonColors
                ) {
                    // TODO DUMMY: překlad hardcoded
                    Text("Create", maxLines = 1)
                }
            }
        }
    }
}