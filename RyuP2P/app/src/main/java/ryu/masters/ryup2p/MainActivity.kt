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
import androidx.compose.ui.unit.dp
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
            val scope = rememberCoroutineScope()
            var inputText by remember { mutableStateOf("") }
            val isSearching by btController.isSearching.collectAsState()


            var buttonText by remember { mutableStateOf("Find Server") }

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
                                enabled = !isServer,
                                onClick = { btController.startServer() },
                                modifier = Modifier.weight(1f)
                            ) { Text("Be Server") }
                            Button(  // client
                                enabled = !isServer && !isSearching,
                                onClick = { btController.startClientMode() },
                                modifier = Modifier.weight(1f)
                            ) {
                                val buttonText = if (isSearching) "Searching..." else "Find Server"
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
                                placeholder = { Text("Message...") }
                            )
                            Button(onClick = {
                                if (inputText.isNotBlank()) {
                                    //zde volat encrypt funkcu
                                    btController.sendMessage(inputText)
                                    inputText = ""
                                }
                            }) { Text("Send") }
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
                                Text("Status: ${if (btController.verifyConnection()) "✓ Verified" else "✗ Lost"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Messages (${messages.size})", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(messages) { msg ->
                                Text(msg, modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Text("Status: ${if (isServer) "Server Waiting..." else "Idle"}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        if (scannedDevices.isNotEmpty()) {
                            Text("Available Devices (${scannedDevices.size})", style = MaterialTheme.typography.titleMedium)
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


