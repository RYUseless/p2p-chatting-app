package ryu.masters_thesis.presentation.chatroom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.chatroom.domain.ChatMessage

private fun parseHexColor(hex: String): Color {
    val cleaned = hex.trimStart('#')
    val long    = when (cleaned.length) {
        6    -> cleaned.toLongOrNull(16)?.let { it or 0xFF000000L }
        8    -> cleaned.toLongOrNull(16)
        else -> null
    } ?: 0xFF9E9E9EL
    return Color(
        red   = ((long shr 16) and 0xFF) / 255f,
        green = ((long shr 8)  and 0xFF) / 255f,
        blue  = ( long         and 0xFF) / 255f,
        alpha = ((long shr 24) and 0xFF) / 255f,
    )
}

// Vrátí světlejší nebo tmavší variantu dle luminance originálu
private fun complementaryBubbleColor(base: Color): Color =
    if (base.luminance() > 0.5f)
        base.copy(red = base.red * 0.6f, green = base.green * 0.6f, blue = base.blue * 0.6f)
    else
        base.copy(red = (base.red + 0.4f).coerceAtMost(1f), green = (base.green + 0.4f).coerceAtMost(1f), blue = (base.blue + 0.4f).coerceAtMost(1f))

@Composable
fun MessageBubble(
    message:      ChatMessage,
    chatColorHex: String = "#9E9E9E",
) {
    val isMe = message.isMe

    val peerBubbleColor = remember(chatColorHex) { parseHexColor(chatColorHex) }
    val myBubbleColor   = remember(peerBubbleColor) { complementaryBubbleColor(peerBubbleColor) }

    val peerTextColor = remember(peerBubbleColor) {
        if (peerBubbleColor.luminance() > 0.4f) Color.Black else Color.White
    }
    val myTextColor = remember(myBubbleColor) {
        if (myBubbleColor.luminance() > 0.4f) Color.Black else Color.White
    }

    val bubbleColor = if (isMe) myBubbleColor   else peerBubbleColor
    val textColor   = if (isMe) myTextColor     else peerTextColor
    val timeColor   = textColor.copy(alpha = 0.5f)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 16.dp,
                        topEnd      = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd   = if (isMe) 4.dp  else 16.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text  = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text     = message.time,
                    color    = timeColor,
                    style    = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}