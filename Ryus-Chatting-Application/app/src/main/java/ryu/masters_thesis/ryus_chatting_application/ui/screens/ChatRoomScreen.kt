package ryu.masters_thesis.ryus_chatting_application.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AddCircle
//import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.KeyboardArrowLeft
//import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothController

// dataclass, bude zmigrovano do jednotne <all_sorted_by_usecase>_Datacalss.kt nebo neco takoveho:
data class ChatMessage(
    val text: String,
    val time: String,
    val isMe: Boolean
)

//TODO: remove pri migraci BT backednu zpet
val previewMessages = listOf(
    ChatMessage("Bogos Binted?", "12:00", isMe = false),
    ChatMessage("huh?", "12:01", isMe = true),
    ChatMessage("worp?", "12:02", isMe = false),
    ChatMessage("\uD83D\uDC7D", "12:03", isMe = true),
)

@Composable
fun ChatRoomScreen(
    roomName: String,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    settings: AppSettings,
    bluetoothController: BluetoothController
) {
    val btMessages by bluetoothController.messages.collectAsState()
    val isConnected by bluetoothController.isConnected.collectAsState()
    val isVerified by bluetoothController.isVerified.collectAsState()  // Opraveno na collectAsState()


    val messages = btMessages.map { msg ->
        ChatMessage(
            text  = msg.content,
            time  = "",
            isMe  = msg.sender == "You"
        )
    }

    ChatRoomScreenContent(
        roomName      = roomName,
        messages      = messages,
        isConnected   = isConnected,
        isVerified    = isVerified,
        onSendMessage = { bluetoothController.sendMessage(it) },
        onBack        = onBack,
        onInfo        = onInfo,
        settings      = settings
    )
}

@Composable
private fun ChatRoomScreenContent(
    roomName: String,
    messages: List<ChatMessage>,
    isConnected: Boolean,
    isVerified: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onInfo: () -> Unit,
    settings: AppSettings
) {
    val isDark          = settings.isDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val surfaceColor    = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor       = if (isDark) Color.White else Color.Black
    val iconColor       = if (isDark) Color.White else Color.Black

    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = iconColor)
            }
            Text(
                text = roomName,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )
            // row, aby byly hezky vedle sebe a nebyla tam ta mezera hehehehe
            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search,
                        contentDescription = "Search",
                        tint = iconColor)
                }
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info,
                        contentDescription = "Info",
                        tint = iconColor)
                }
            }
        }

        // Stav připojení — zobrazí se dokud klient není připojen
        if (!isConnected || !isVerified) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        !isConnected  -> "⏳ Čeká se na připojení klienta..."
                        true -> "⚠️ Spojení ověřuje se..."
                        else                   -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.6f)
                )
            }
        }

        // ZPRÁVY:
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message, isDark = isDark)
            }
        }

        // BOTTOM BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Média", tint = iconColor)
            }
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Zpráva...") },
                trailingIcon = {
                    IconButton(onClick = { }) {
                        Text("😊")
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
                enabled = messageInput.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Odeslat", tint = if (messageInput.isNotBlank()) iconColor else iconColor.copy(alpha = 0.3f))
            }
        }
    }
}

//chatroombubble
@Composable
fun MessageBubble(message: ChatMessage, isDark: Boolean) {
    val bubbleColor = when {
        //move to config soon
        isDark && message.isMe  -> Color(0xFF3A3A3A)
        isDark && !message.isMe -> Color(0xFFFFFFFF)
        !isDark && message.isMe -> Color(0xFF9E9E9E)
        else                    -> Color(0xFF212121)
    }
    val textColor = when {
        isDark && message.isMe  -> Color.White
        isDark && !message.isMe -> Color.Black
        !isDark && message.isMe -> Color.White
        else                    -> Color.White
    }
    val timeColor = textColor.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                Text(text = message.time, color = timeColor, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatRoomScreenPreview() {
    ChatRoomScreenContent(
        roomName      = "RyuRoom-696",
        messages      = previewMessages,
        isConnected   = true,
        isVerified = true,
        onSendMessage = {},
        onBack        = {},
        onInfo        = {},
        settings      = AppSettings()
    )
}