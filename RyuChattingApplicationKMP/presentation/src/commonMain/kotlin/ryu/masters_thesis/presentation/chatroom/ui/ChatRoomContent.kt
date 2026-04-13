package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomState
import ryu.masters_thesis.presentation.chatroom.ui.info.ChatInfoSheet

@Composable
fun ChatRoomContent(
    state: ChatRoomState,
    onEvent: (ChatRoomEvent) -> Unit,
    // TODO DUMMY: isDark nahradit Theme systémem až bude dostupný
    isDark: Boolean = false,
) {
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // Top bar
            ChatTopBar(
                roomName    = state.roomName,
                isConnected = state.isConnected,
                isVerified  = state.isVerified,
                onEvent     = onEvent,
                isDark      = isDark,
            )

            // Zprávy
            LazyColumn(
                modifier       = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout  = true,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages.reversed()) { message ->
                    MessageBubble(
                        message      = message,
                        isDark       = isDark,
                        chatColorHex = state.chatColorHex,
                    )
                }
            }

            // Emoji menu nad bottom barem
            if (state.showEmojiMenu) {
                EmojiMenuPopup(onEvent = onEvent)
            }

            // Bottom bar
            ChatBottomBar(
                messageInput = state.messageInput,
                onEvent      = onEvent,
                isDark       = isDark,
            )
        }

        // Info sheet dialog
        if (state.showInfoSheet) {
            ChatInfoSheet(
                state   = state,
                onEvent = onEvent,
                isDark  = isDark,
            )
        }
    }
}