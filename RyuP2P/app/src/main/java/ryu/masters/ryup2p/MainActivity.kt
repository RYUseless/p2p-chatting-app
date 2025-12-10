package ryu.masters.ryup2p

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import ryu.masters.ryup2p.logic.bluetooth.BluetoothController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ))
        }

        setContent {
            val btController = remember { BluetoothController(this@MainActivity) }
            val scannedDevices by btController.scannedDevices.collectAsState()
            val isConnected by btController.isConnected.collectAsState()
            val connectedDeviceName by btController.connectedDeviceName.collectAsState()
            val isServer by btController.isServer.collectAsState()
            val messages by btController.messages.collectAsState()
            val currentRoomId by btController.currentRoomId.collectAsState()
            val needsPassword by btController.needsPassword.collectAsState()
            val passwordError by btController.passwordError.collectAsState()
            val scope = rememberCoroutineScope()

            var inputText by remember { mutableStateOf("") }
            val isSearching by btController.isSearching.collectAsState()

            // Password dialog - shows for both server (create) and client (unlock)
            if (needsPassword) {
                PasswordDialog(
                    isServer = isServer,
                    isConnected = isConnected,
                    error = passwordError,
                    onPasswordSubmit = { password ->
                        if (isServer && !isConnected) {
                            // Server creating room with password
                            btController.submitServerPassword(password)
                        } else {
                            // Client unlocking room with password
                            btController.submitClientPassword(password)
                        }
                    },
                    onDismiss = { /* Cannot dismiss - must enter password */ }
                )
            }

            Scaffold(
                bottomBar = {
                    if (!isConnected) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !isServer && !needsPassword,
                                onClick = { btController.startServer() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create Room")
                            }

                            Button(
                                enabled = !isServer && !isSearching && !needsPassword,
                                onClick = { btController.startClientMode() },
                                modifier = Modifier.weight(1f)
                            ) {
                                val buttonText = if (isSearching) "Searching..." else "Connect to Room"
                                Text(buttonText)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message...") },
                                enabled = !needsPassword
                            )

                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        btController.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                enabled = !needsPassword
                            ) { Text("Send") }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    if (isConnected && connectedDeviceName != null) {
                        Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("✓ Connected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Device: $connectedDeviceName", style = MaterialTheme.typography.bodyMedium)
                                if (currentRoomId != null) {
                                    Text("Room ID: $currentRoomId", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (needsPassword) {
                                    Text("🔒 Locked - Enter password to decrypt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("🔓 Unlocked - Messages encrypted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Text("Status: ${if (btController.verifyConnection()) "✓ Verified" else "✗ Lost"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Messages (${messages.size})", style = MaterialTheme.typography.titleMedium)

                        if (needsPassword) {
                            Text("Enter password to view messages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }

                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(messages) { msg ->
                                Text(msg, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        val statusText = when {
                            isServer && currentRoomId != null -> "Server Waiting (Room: $currentRoomId)..."
                            isServer && needsPassword -> "Creating room..."
                            isServer -> "Server Waiting..."
                            else -> "Idle"
                        }

                        Text("Status: $statusText", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))

                        if (scannedDevices.isNotEmpty()) {
                            Text("Available Rooms (${scannedDevices.size})", style = MaterialTheme.typography.titleMedium)
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(scannedDevices) { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(device.name ?: "Unknown")
                                            if (device.roomId != null) {
                                                Text("Room: ${device.roomId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                                        }

                                        Button(onClick = {
                                            scope.launch {
                                                btController.connectToDevice(device)
                                            }
                                        }) { Text("Connect") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordDialog(
    isServer: Boolean,
    isConnected: Boolean,
    error: String?,
    onPasswordSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    // Determine dialog type based on state
    val dialogTitle = when {
        isServer && !isConnected -> "Create Room Password"
        else -> "Unlock Room"
    }

    val dialogDescription = when {
        isServer && !isConnected -> "Set a password to encrypt messages in this room. Share this password with people you want to connect."
        else -> "Enter the room password to decrypt messages"
    }

    val buttonText = when {
        isServer && !isConnected -> "Create Room"
        else -> "Unlock Room"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = dialogDescription,
                    style = MaterialTheme.typography.bodyMedium
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (password.isNotEmpty()) {
                            onPasswordSubmit(password)
                        }
                    },
                    enabled = password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

