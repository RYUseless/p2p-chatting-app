package ryu.masters_thesis.ryus_chatting_application.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import ryu.masters_thesis.ryus_chatting_application.ChatRoom
/*
START SCREEN LIST:
 */
@Composable
fun ChatRoomItem(room: ChatRoom, textColor: Color, surfaceColor: Color, onClick: () -> Unit) {
    val isClickable = room.active

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor.copy(alpha = if (isClickable) 1f else 0.5f))
            .then(
                if (isClickable) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = room.name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = if (isClickable) 1f else 0.4f)
        )
        Text(
            text = if (room.active) "Online" else "Offline",
            style = MaterialTheme.typography.labelMedium,
            color = if (room.active) Color.Green else Color.Red.copy(alpha = 0.5f)
        )
    }
}


/*
DRUHY LIST, NA FIND SCREENU
 */
@Composable
fun FindRoomItem(name: String, mac: String, textColor: Color, surfaceColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge, color = textColor)
        Text(text = mac, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = 0.5f))
    }
}