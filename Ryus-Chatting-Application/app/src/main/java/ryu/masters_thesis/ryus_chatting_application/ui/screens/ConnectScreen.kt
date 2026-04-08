package ryu.masters_thesis.ryus_chatting_application.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.logic.QRCode.QRCodeReader
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothController
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothDevice
import ryu.masters_thesis.ryus_chatting_application.ui.components.FindRoomItem

@Composable
fun ConnectScreen(
    onDismiss: () -> Unit,
    settings: AppSettings,
    bluetoothController: BluetoothController,
    onNavigateToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = getTranslations(settings.language)
    val isDark = settings.isDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor = if (isDark) Color.Black else Color.White
    )

    val scannedDevices by bluetoothController.scannedDevices.collectAsState()
    val isConnected by bluetoothController.isConnected.collectAsState()
    val isVerified by bluetoothController.isVerified.collectAsState() // Přidáno sledování isVerified
    val currentRoomId by bluetoothController.currentRoomId.collectAsState()
    val isSearching by bluetoothController.isSearching.collectAsState()

    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var remainingSeconds by remember { mutableStateOf(30) }

    // ACCESS PERMISITIONS -- maybe navíc?
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) bluetoothController.startClientMode()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothController.startClientMode()
        } else {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // do budoucna udelat based on config file
        for (i in 30 downTo 0) {
            remainingSeconds = i
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bluetoothController.unregisterReceiver()
        }
    }

    // Navigace do chatu po připojení a úspěšném ověření (handshake)
    LaunchedEffect(isVerified, currentRoomId) {
        if (isVerified && currentRoomId != null) {
            selectedDevice = null
            onNavigateToChat(currentRoomId!!)
        }
    }

    val titleModifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = strings.connectTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            modifier = titleModifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isSearching) strings.connectTimeRemaining.format(remainingSeconds) else "Hledání ukončeno",
            style = MaterialTheme.typography.titleSmall,
            modifier = titleModifier.padding(horizontal = 24.dp),
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (scannedDevices.isEmpty()) {
            Text(
                strings.connectNoRooms,
                style = MaterialTheme.typography.titleLarge,
                modifier = titleModifier.padding(horizontal = 24.dp),
                color = Color.Red
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Text(
                strings.connectAvailableRooms.format(scannedDevices.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = titleModifier.padding(horizontal = 24.dp),
                color = textColor
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(backgroundColor)
            ) {
                items(scannedDevices) { device ->
                    FindRoomItem(
                        name = device.roomId ?: device.name ?: device.address,
                        mac = device.address,
                        textColor = textColor,
                        surfaceColor = surfaceColor,
                        onClick = { selectedDevice = device }
                    )
                }
            }
        }

        Button(
            onClick = {
                bluetoothController.unregisterReceiver()
                onDismiss()
            },
            colors = blackButtonColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
        ) { Text(strings.close) }
    }

    selectedDevice?.let { device ->
        JoinRoomDialog(
            device = device,
            isDark = isDark,
            bluetoothController = bluetoothController,
            onDismiss = { selectedDevice = null }
        )
    }
}

@Composable
fun JoinRoomDialog(
    device: BluetoothDevice,
    isDark: Boolean,
    bluetoothController: BluetoothController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor = if (isDark) Color.Black else Color.White
    )

    val needsPassword by bluetoothController.needsPassword.collectAsState()
    val passwordError by bluetoothController.passwordError.collectAsState()
    val isConnected by bluetoothController.isConnected.collectAsState()

    var passwordInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showQrScanner = true }

    LaunchedEffect(device) {
        isConnecting = true
        bluetoothController.connectToDevice(device)
    }


    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Připojit se k: ${device.roomId ?: device.name ?: device.address}",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = device.address,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isConnecting && !needsPassword) {
                CircularProgressIndicator(color = if (isDark) Color.White else Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Připojování...", color = textColor)
            }

            if (needsPassword) {
                if (showQrScanner) {
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                        },
                        update = { previewView ->
                            QRCodeReader.start(previewView.context, previewView, lifecycleOwner) { scanned ->
                                passwordInput = scanned
                                showQrScanner = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showQrScanner = false }) {
                        Text("Zadat ručně", color = textColor)
                    }
                } else {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Heslo") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) Text(passwordError!!, color = Color.Red)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) showQrScanner = true
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📷 Načíst QR kód", color = textColor) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Zrušit", color = textColor) }

                if (needsPassword) {
                    Button(
                        onClick = { bluetoothController.submitClientPassword(passwordInput) },
                        enabled = passwordInput.isNotBlank(),
                        colors = blackButtonColors,
                        modifier = Modifier.weight(1f)
                    ) { Text("Připojit") }
                }
            }
        }
    }
}