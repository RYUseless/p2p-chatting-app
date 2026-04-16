package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.implementation.ChatRoomState
import ryu.masters_thesis.presentation.chatroom.ui.info.ChatInfoSheet

@Composable
fun ChatRoomContent(
    state: ChatRoomState,
    onEvent: (ChatRoomEvent) -> Unit,
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            ChatTopBar(
                roomName    = state.roomName,
                isConnected = state.isConnected,
                isVerified  = state.isVerified,
                onEvent     = onEvent,
            )

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
                        chatColorHex = state.chatColorHex,
                    )
                }
            }

            if (state.showEmojiMenu) {
                EmojiMenuPopup(onEvent = onEvent)
            }

            ChatBottomBar(
                messageInput = state.messageInput,
                onEvent      = onEvent,
            )
        }

        if (state.showInfoSheet) {
            ChatInfoSheet(
                state   = state,
                onEvent = onEvent,
            )
        }
    }
}