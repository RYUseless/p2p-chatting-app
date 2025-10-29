package ryu.masters.p2papp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import ryu.masters.p2papp.ui.theme.P2PAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var enableBluetooth: EnableBluetooth
    private var consoleLog by mutableStateOf("")

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
        enableEdgeToEdge()
        setContent {
            P2PAppTheme {
                BluetoothScreen(
                    consoleText = consoleLog,
                    onEnableClick = { handleEnableClick() }
                )
            }
        }
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

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
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
    onEnableClick: () -> Unit = {}
) {
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
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Check, "Connect") },
                    label = { Text("Connect") },
                    selected = false,
                    onClick = { }
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
                modifier = Modifier.fillMaxSize()
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
