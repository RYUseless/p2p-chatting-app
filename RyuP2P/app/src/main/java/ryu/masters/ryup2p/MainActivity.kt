package ryu.masters.ryup2p

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ryu.masters.ryup2p.logic.bluetooth.BluetoothController
import ryu.masters.ryup2p.logic.bluetooth.BluetoothState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Running on API level: ${Build.VERSION.SDK_INT}")
        setContent { BluetoothScreen(applicationContext) }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScreen(appContext: Context) {
    val context = LocalContext.current

    // ===== ROZLIŠENÍ API VERZÍ PRO PERMISSIONS =====
    // API 31+ (Android 12+): BLUETOOTH_SCAN + BLUETOOTH_CONNECT + Location
    // API 29-30 (Android 10-11): Pouze Location permissions
    val corePerms = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("BluetoothScreen", "API 31+ detected - requesting new Bluetooth permissions")
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            Log.d("BluetoothScreen", "API 29-30 detected - requesting legacy Location permissions")
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val advertisePerm = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("BluetoothScreen", "API 31+ - BLUETOOTH_ADVERTISE permission available")
            Manifest.permission.BLUETOOTH_ADVERTISE
        } else {
            Log.d("BluetoothScreen", "API 29-30 - BLUETOOTH_ADVERTISE not needed")
            null
        }
    }

    var state by remember { mutableStateOf(BluetoothState.fromContext(appContext)) }
    var error by remember { mutableStateOf<String?>(null) }
    var enableInProgress by remember { mutableStateOf(false) }
    var discoverableInProgress by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun has(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    fun missing(perms: Array<String>): Array<String> {
        val missingPerms = perms.filter { !has(it) }.toTypedArray()
        if (missingPerms.isNotEmpty()) {
            Log.w("BluetoothScreen", "Missing permissions: ${missingPerms.joinToString()}")
        }
        return missingPerms
    }

    fun missingForSearch() = missing(corePerms)

    fun missingForVisible(): Array<String> {
        val m = corePerms.filter { !has(it) }.toMutableList()
        if (advertisePerm != null && !has(advertisePerm)) m += advertisePerm
        return m.toTypedArray()
    }

    val controller = remember {
        BluetoothController(
            context = appContext,
            onUpdate = { s ->
                state = s
                Log.d("BluetoothScreen", "State updated - scanning: ${s.isScanning}, discovered: ${s.discoveredDevices.size}")
            },
            onError = { msg ->
                error = msg
                Log.e("BluetoothScreen", "Error: $msg")
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("BluetoothScreen", "Disposing - unregistering receiver")
            controller.unregisterReceiver()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        enableInProgress = false
        if (res.resultCode == Activity.RESULT_OK) {
            Log.d("BluetoothScreen", "Bluetooth enabled successfully")
            state = BluetoothState.fromContext(appContext)
            if (state.isBluetoothSupported && state.isEnabled) {
                controller.startDiscovery()
            }
        } else {
            error = "Bluetooth enabling was canceled"
            Log.w("BluetoothScreen", "Bluetooth enabling was canceled")
        }
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        discoverableInProgress = false
        if (res.resultCode == Activity.RESULT_CANCELED) {
            error = "Discoverable request was canceled"
            Log.w("BluetoothScreen", "Discoverable request was canceled")
        } else {
            Log.d("BluetoothScreen", "Device is now discoverable for ${res.resultCode} seconds")
        }
    }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d("BluetoothScreen", "Permissions result: $results")
        val allGranted = results.all { it.value }
        if (allGranted) {
            Log.d("BluetoothScreen", "All permissions granted")
        } else {
            Log.w("BluetoothScreen", "Some permissions were denied")
        }
        state = BluetoothState.fromContext(appContext)
        pendingAction?.invoke()
        pendingAction = null
    }

    fun proceedScanAfterPerms() {
        Log.d("BluetoothScreen", "Proceeding with scan after permissions check")
        state = BluetoothState.fromContext(appContext)
        if (!state.isBluetoothSupported) {
            error = "Bluetooth not supported"
            Log.e("BluetoothScreen", "Bluetooth not supported on this device")
            return
        }
        if (!state.isEnabled) {
            Log.d("BluetoothScreen", "Bluetooth disabled - requesting enable")
            enableInProgress = true
            enableBtLauncher.launch(controller.buildEnableIntent())
            return
        }
        Log.d("BluetoothScreen", "Starting discovery")
        controller.startDiscovery()
    }

    fun onSearchClick() {
        error = null
        if (state.isScanning) {
            Log.w("BluetoothScreen", "Already scanning - ignoring click")
            return
        }

        Log.d("BluetoothScreen", "Search clicked - checking permissions for API ${Build.VERSION.SDK_INT}")
        val miss = missingForSearch()
        if (miss.isNotEmpty()) {
            Log.d("BluetoothScreen", "Requesting missing permissions: ${miss.joinToString()}")
            pendingAction = { proceedScanAfterPerms() }
            permsLauncher.launch(miss)
            return
        }
        proceedScanAfterPerms()
    }

    fun onMakeVisibleClick(durationSec: Int = 300) {
        error = null
        if (state.isScanning) {
            Log.w("BluetoothScreen", "Scanning in progress - ignoring visible click")
            return
        }

        Log.d("BluetoothScreen", "Make visible clicked - checking permissions")
        val miss = missingForVisible()
        if (miss.isNotEmpty()) {
            Log.d("BluetoothScreen", "Requesting missing permissions for discoverable: ${miss.joinToString()}")
            pendingAction = {
                discoverableInProgress = true
                discoverableLauncher.launch(controller.buildDiscoverableIntent(durationSec))
            }
            permsLauncher.launch(miss)
            return
        }
        discoverableInProgress = true
        discoverableLauncher.launch(controller.buildDiscoverableIntent(durationSec))
    }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = !state.isScanning && !enableInProgress && !discoverableInProgress,
                            onClick = { onSearchClick() }
                        ) { Text(if (state.isScanning) "Scanning…" else "Search") }
                        Button(
                            enabled = !state.isScanning && !enableInProgress && !discoverableInProgress,
                            onClick = { onMakeVisibleClick(300) }
                        ) { Text("Make visible") }
                    }
                    if (state.isScanning) {
                        Text("Running 33s scan", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            // API level info
            Text(
                "Android API: ${Build.VERSION.SDK_INT}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Text("Paired devices (${state.bondedDevices.size}):")
            Spacer(Modifier.height(6.dp))
            val paired = state.bondedDevices.toList().sortedBy { it.name ?: it.address }
            if (paired.isEmpty()) {
                Text("No paired devices", style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(Modifier.heightIn(max = 160.dp)) {
                items(paired) { dev -> DeviceRow(dev) {} }
            }

            Spacer(Modifier.height(12.dp))
            Text("Discovered devices (${state.discoveredDevices.size}):")
            Spacer(Modifier.height(6.dp))
            val devices = state.discoveredDevices.toList().sortedBy { it.name ?: it.address }
            if (!state.isScanning && devices.isEmpty()) {
                Text("No devices found", style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(devices) { dev -> DeviceRow(dev) {} }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceRow(device: BluetoothDevice, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(device.name ?: "(Unknown name)")
        Text(device.address, style = MaterialTheme.typography.bodySmall)
    }
}
