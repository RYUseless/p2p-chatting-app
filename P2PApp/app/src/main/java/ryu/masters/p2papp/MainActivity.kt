package ryu.masters.p2papp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ryu.masters.p2papp.backend.bluetooth.EnableBluetooth
import ryu.masters.p2papp.backend.bluetooth.FindBLDevices
import ryu.masters.p2papp.backend.bluetooth.ConnectBLDevices
import ryu.masters.p2papp.ui.theme.P2PAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var enableBluetooth: EnableBluetooth
    private lateinit var findBLDevices: FindBLDevices
    private lateinit var connectBLDevices: ConnectBLDevices
    private var consoleLog by mutableStateOf("")
    private var discoveredDevices by mutableStateOf<List<Pair<String, String>>>(emptyList())
    private var showConnectDialog by mutableStateOf(false)

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            consoleLog += "Bluetooth enabled successfully\n"
        } else {
            consoleLog += "Bluetooth enable canceled\n"
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            consoleLog += "Bluetooth permissions granted\n"
            checkAndEnableBluetooth()
        } else {
            consoleLog += "Bluetooth permissions denied\n"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableBluetooth = EnableBluetooth(this)
        findBLDevices = FindBLDevices(this)
        connectBLDevices = ConnectBLDevices(this)
        enableEdgeToEdge()

        findBLDevices.onDeviceDiscovered = { name, address ->
            consoleLog += "Found: $name ($address)\n"
            discoveredDevices = discoveredDevices + Pair(name, address)
        }

        findBLDevices.onDiscoveryFinished = {
            consoleLog += "Discovery finished\n"
        }

        connectBLDevices.onConnectionSuccess = { address ->
            consoleLog += "Connected to: $address\n"
        }

        connectBLDevices.onConnectionFailed = { error ->
            consoleLog += "Connection error: $error\n"
        }

        connectBLDevices.onDataReceived = { data ->
            consoleLog += "[DATA] $data\n"
        }

        setContent {
            P2PAppTheme {
                BluetoothScreen(
                    consoleText = consoleLog,
                    discoveredDevices = discoveredDevices,
                    showConnectDialog = showConnectDialog,
                    onEnableClick = { handleEnableClick() },
                    onSearchClick = { handleSearchClick() },
                    onConnectClick = { handleConnectClick() },
                    onDeviceConnect = { address -> connectBLDevices.connectToDevice(address) },
                    onDialogDismiss = { showConnectDialog = false }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        findBLDevices.stopDiscovery()
        connectBLDevices.disconnect()
    }

    private fun handleEnableClick() {
        consoleLog += "Enable button clicked\n"

        if (!enableBluetooth.hasBluetoothPermission()) {
            consoleLog += "Requesting Bluetooth permissions...\n"
            requestBluetoothPermissions()
        } else {
            checkAndEnableBluetooth()
        }
    }

    private fun handleSearchClick() {
        consoleLog += "Search button clicked\n"
        discoveredDevices = emptyList()

        if (!enableBluetooth.hasBluetoothPermission()) {
            consoleLog += "Requesting Bluetooth permissions...\n"
            requestBluetoothPermissions()
            return
        }

        consoleLog += "=== DISCOVERING NEW DEVICES ===\n"
        findBLDevices.startDiscovery(timeoutMs = 60000L)
    }

    private fun handleConnectClick() {
        consoleLog += "Connect button clicked\n"
        if (discoveredDevices.isNotEmpty()) {
            showConnectDialog = true
        } else {
            consoleLog += "No devices discovered. Click Search first.\n"
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        }
    }

    private fun checkAndEnableBluetooth() {
        if (enableBluetooth.isBluetoothEnabled()) {
            consoleLog += "Bluetooth already enabled\n"
        } else {
            enableBluetooth.getEnableBluetoothIntent()?.let { intent ->
                consoleLog += "Requesting Bluetooth enable...\n"
                enableBtLauncher.launch(intent)
            } ?: run {
                consoleLog += "Bluetooth adapter not found\n"
            }
        }
    }
}

@Composable
fun BluetoothScreen(
    consoleText: String = "Console ready...\n",
    discoveredDevices: List<Pair<String, String>> = emptyList(),
    showConnectDialog: Boolean = false,
    onEnableClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onConnectClick: () -> Unit = {},
    onDeviceConnect: (String) -> Unit = {},
    onDialogDismiss: () -> Unit = {}
) {
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text("Select Device to Connect") },
            text = {
                LazyColumn {
                    items(discoveredDevices) { (name, address) ->
                        Button(
                            onClick = {
                                onDeviceConnect(address)
                                onDialogDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("$name\n$address")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = onDialogDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Enable") },
                    label = { Text("Enable") },
                    selected = false,
                    onClick = onEnableClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, "Search") },
                    label = { Text("Search") },
                    selected = false,
                    onClick = onSearchClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Check, "Connect") },
                    label = { Text("Connect") },
                    selected = false,
                    onClick = onConnectClick
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Close, "Disconnect") },
                    label = { Text("Disc...") },
                    selected = false,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, "Debug") },
                    label = { Text("Debug") },
                    selected = false,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Bluetooth Magic by RYU",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = consoleText,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (discoveredDevices.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        items(discoveredDevices) { (name, address) ->
                            Text(
                                text = "$name ($address)",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothScreenPreview() {
    P2PAppTheme {
        BluetoothScreen()
    }
}

