package ryu.masters.ryup2p.pressentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ryu.masters.ryup2p.logic.bluetooth.BluetoothChatManager

@Composable
fun ChatScreen(chatManager: BluetoothChatManager, onClose: () -> Unit) {
    val messages by chatManager.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Chat", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onClose) { Text("Close") }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { msg ->
                Text("${msg.sender}: ${msg.content}", modifier = Modifier.padding(4.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message") })
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    chatManager.addMessage("Me", inputText)
                    inputText = ""
                }
            }) { Text("Send") }
        }
    }
}
