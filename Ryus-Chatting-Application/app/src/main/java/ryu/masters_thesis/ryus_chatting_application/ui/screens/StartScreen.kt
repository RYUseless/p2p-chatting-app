package ryu.masters_thesis.ryus_chatting_application.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
//import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ryu.masters_thesis.ryus_chatting_application.ChatRoom
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.ui.Screen
import ryu.masters_thesis.ryus_chatting_application.ui.components.ChatRoomItem
import ryu.masters_thesis.ryus_chatting_application.ui.theme.RyusChattingApplicationTheme

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    settings: AppSettings,
) {
    val isDark = settings.isDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val surfaceColor    = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor       = if (isDark) Color.White else Color.Black
    val borderColor     = if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD)
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    // preklady
    //val settings = remember { AppSettings() }
    val strings  = getTranslations(settings.language)

    // promenna pro ulozene chatroomy
    val chatRooms = remember { mutableStateListOf<ChatRoom>() }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header – Logo + Settings ikona
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
                onClick = { navController.navigate(Screen.Settings.route) },
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

        // Chat list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            Text(
                text = strings.savedChats,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = textColor
            )
            HorizontalDivider(color = borderColor)
            if (chatRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.noChatsAvailable,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    items(chatRooms) { room ->
                        ChatRoomItem(
                            room = room,
                            textColor = textColor,
                            surfaceColor = surfaceColor,
                            onClick = { navController.navigate(Screen.ChatRoom.createRoute(room.name)) }
                        )
                    }
                }
            }
        }

        // TLACITKA SPODNI CAST OBRAZOVKY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CONNECT TLACITKO
            Button(
                onClick = { navController.navigate(Screen.Connect.route) },
                modifier = Modifier.weight(1f),
                colors = blackButtonColors
            ) { Text(strings.buttonConnect, maxLines = 1) }
            // CREATE TLACITKO
            Button(
                onClick = { navController.navigate(Screen.Create.route) },
                modifier = Modifier.weight(1f),
                colors = blackButtonColors
            ) { Text(strings.buttonCreate, maxLines = 1) }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun StartScreenPreview() {
    RyusChattingApplicationTheme {
        StartScreen(
            navController = rememberNavController(),
            settings = AppSettings(),
        )
    }
}