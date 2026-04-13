package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    isDark: Boolean,
    // TODO DUMMY: chatColorHex z state až bude theme systém hotový
    chatColorHex: String = "#9E9E9E",
) {
    val bubbleColor = when {
        isDark && message.isMe   -> Color(0xFF3A3A3A)
        isDark && !message.isMe  -> Color(0xFFFFFFFF)
        !isDark && message.isMe  -> Color(0xFF9E9E9E)
        else                     -> Color(0xFF212121)
    }
    val textColor = when {
        isDark && message.isMe   -> Color.White
        isDark && !message.isMe  -> Color.Black
        !isDark && message.isMe  -> Color.White
        else                     -> Color.White
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
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = if (message.isMe) 16.dp else 4.dp,
                        bottomEnd   = if (message.isMe) 4.dp  else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text  = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text     = message.time,
                    color    = timeColor,
                    style    = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}