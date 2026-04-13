package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.domain.DEFAULT_EMOJI_LIST

@Composable
fun EmojiMenuPopup(
    onEvent: (ChatRoomEvent) -> Unit,
) {
    // ← Popup { } wrapper odstraněn — pozici řídí Column v ChatRoomContent
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()          // ← width(280.dp) → fillMaxWidth pro konzistenci s BottomBarem
            .height(180.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(DEFAULT_EMOJI_LIST) { emoji ->
                Text(
                    text     = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onEvent(ChatRoomEvent.EmojiSelected(emoji)) }
                )
            }
        }
    }
}