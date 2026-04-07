package ryu.masters_thesis.ryus_chatting_application.ui.screens

import android.Manifest
import android.content.pm.PackageManager
//import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.logic.QRCode.QRCodeReader
import ryu.masters_thesis.ryus_chatting_application.ui.components.FindRoomItem
import ryu.masters_thesis.ryus_chatting_application.ui.theme.RyusChattingApplicationTheme

data class FoundRoom(val name: String, val mac: String, val password: String)

@Composable
fun ConnectScreen(
    onDismiss: () -> Unit,
    settings: AppSettings
) {
    val strings = getTranslations(settings.language)
    val isDark = settings.isDarkTheme()

    //todo: migrovat do config folderu
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor = if (isDark) Color.Black else Color.White
    )

    //dummydevices → nalezene roomky v okoli
    // default name, adresa serveru chatu, aka kdo to naposledy hostoval, heslo(zasifrovat)
    val dummyDevices = remember { mutableStateListOf<FoundRoom>() }


    var selectedRoom by remember { mutableStateOf<FoundRoom?>(null) }

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
            strings.connectTimeRemaining.format(69),
            style = MaterialTheme.typography.titleSmall,
            modifier = titleModifier.padding(horizontal = 24.dp),
            color = textColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (dummyDevices.isEmpty()) {
            Text(
                strings.connectNoRooms,
                style = MaterialTheme.typography.titleLarge,
                modifier = titleModifier.padding(horizontal = 24.dp),
                color = Color.Red
            )
            Spacer(modifier = Modifier.weight(1f)) // ← tlačí button dolů
        } else {
            Text(
                //zde vyresit odsazeni
                strings.connectAvailableRooms.format(dummyDevices.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = titleModifier.padding(horizontal = 24.dp),
                color = textColor
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // ← zabere zbývající prostor
                    .background(backgroundColor)
            ) {
                items(dummyDevices) { room ->
                    FindRoomItem(
                        name = room.name,
                        mac = room.mac,
                        textColor = textColor,
                        surfaceColor = surfaceColor,
                        onClick = { selectedRoom = room }
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            colors = blackButtonColors,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp) // ← padding na button
                .padding(top = 16.dp)
        ) { Text(strings.close) }
    }

    selectedRoom?.let { room ->
        JoinRoomDialog(
            room = room,
            isDark = isDark,
            //staticka analyza false positive issue maybe
            //TODO: check this shit
            onDismiss = { selectedRoom = null },
            onJoin = { selectedRoom = null }
        )
    }
}

@Composable
fun JoinRoomDialog(
    room: FoundRoom,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    var passwordInput by remember { mutableStateOf("") }
    var wrongPassword by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    // permice
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showQrScanner = true
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
                text = "Připojit se k: ${room.name}",
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Text(
                text = room.mac,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            //QR CODE
            if (showQrScanner) {
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            //implementationMode = PreviewView.ImplementationMode.COMPATIBLE // ← yeet
                        }
                    },
                    update = { previewView ->
                        QRCodeReader.start(previewView.context, previewView, lifecycleOwner) { scannedContent ->
                            // val parts = scannedContent.split("|")
                            //if (parts.size == 2) {
                            //    passwordInput = parts[1]
                            //}
                            passwordInput = scannedContent
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
                    onValueChange = {
                        passwordInput = it
                        wrongPassword = false
                    },
                    label = { Text("Heslo") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = wrongPassword,
                    supportingText = {
                        if (wrongPassword) Text("Špatné heslo", color = Color.Red)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Nahraď TextButton pro otevření skeneru:
                TextButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            showQrScanner = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📷 Načíst QR kód", color = textColor)
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

                Button(
                    onClick = {
                        if (passwordInput == room.password) {
                            onJoin(passwordInput)
                        } else {
                            wrongPassword = true
                        }
                    },
                    enabled = passwordInput.isNotBlank(),
                    colors = blackButtonColors,
                    modifier = Modifier.weight(1f)
                ) { Text("Připojit") }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun ConnectScreenPreview() {
    RyusChattingApplicationTheme { ConnectScreen(onDismiss = {}, settings = AppSettings()) }
}