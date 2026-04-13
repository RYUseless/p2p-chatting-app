package ryu.masters_thesis.presentation.chatroom.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent

// TODO DUMMY: základní paleta, rozšířit nebo napojit na color picker až bude :core
private val COLOR_PALETTE = listOf(
    "#9E9E9E", "#F44336", "#E91E63", "#9C27B0",
    "#3F51B5", "#2196F3", "#009688", "#4CAF50",
    "#FF9800", "#795548",
)

@Composable
fun ChatInfoColorSection(
    currentColorHex: String,
    onEvent: (ChatRoomEvent) -> Unit,
    textColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            // TODO DUMMY: překlad hardcoded
            text  = "Chat color",
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            COLOR_PALETTE.forEach { hex ->
                val color = remember(hex) {
                    val colorLong = hex.trimStart('#').toLong(16) or 0xFF000000
                    Color(colorLong.toInt())
                }
                val selected = hex == currentColorHex
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(2.dp, textColor, CircleShape)
                            else Modifier
                        )
                        .clickable { onEvent(ChatRoomEvent.ChatColorChanged(hex)) }
                )
            }
        }
    }
}