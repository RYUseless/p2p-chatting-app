package ryu.masters_thesis.ryus_chatting_application.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.AppTranslations
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.logic.QRCode.QRCodeGenerator
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothController

/*
YEET OUT
data class FieldConfig(
    val label: String,
    val defaultValue: String,
    val isPassword: Boolean = false
)
 */
//TODO: odjebat ten shit s chatRoomName a napojit to lépe na backend, ať se zrovna vypíše Ryusmth-ID


@Composable
fun CreateScreen(
    onDismiss: () -> Unit,
    onNavigateToChat: (roomId: String) -> Unit,
    settings: AppSettings,
    bluetoothController: BluetoothController
) {
    val context = LocalContext.current
    val strings = getTranslations(settings.language)
    val isDark  = settings.isDarkTheme()

    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor       = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    val passwordError by bluetoothController.passwordError.collectAsState()
    val currentRoomId by bluetoothController.currentRoomId.collectAsState()

    var password      by remember { mutableStateOf("") }
    var serverStarted by remember { mutableStateOf(false) }
    var showQrDialog  by remember { mutableStateOf(false) }

    // Zkontroluje jestli jsou Bluetooth permissions granted
    val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val passwordIsValid = password.isNotEmpty()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = strings.createTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Chatroom Name — read-only, bere roomId z backendu po spuštění serveru
            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget("Chatroom Name", textColor)
                OutlinedTextField(
                    value = currentRoomId ?: "Generuje se po vytvoření...",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Room Password — editovatelné
            Column(modifier = Modifier.fillMaxWidth()) {
                TextWidget("Room Password", textColor)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Varování pokud chybí BT permissions
            if (!hasPermissions) {
                Text(
                    text = "⚠️ Vyžadována Bluetooth oprávnění — povolte je v nastavení",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Tlačítko 1: Vytvořit místnost
            // → spustí server + makeDiscoverable() + naviguje do ChatRoomScreen
            Button(
                onClick = {
                    val roomId = bluetoothController.createRoomFromCreateScreen(password)
                    serverStarted = true
                    if (roomId != null) {
                        onNavigateToChat(roomId)
                    }
                },
                enabled = passwordIsValid && !serverStarted && hasPermissions,
                colors = blackButtonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (serverStarted) "✓ Místnost vytvořena" else "Vytvořit místnost")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tlačítko 2: Zobrazit QR kód
            // → pokud server ještě neběží, spustí ho + zobrazí QR (bez navigace)
            Button(
                onClick = {
                    if (!serverStarted) {
                        bluetoothController.createRoomFromCreateScreen(password)
                        serverStarted = true
                    }
                    showQrDialog = true
                },
                enabled = passwordIsValid && hasPermissions,
                colors = blackButtonColors,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zobrazit QR kód")
            }

            if (showQrDialog && currentRoomId != null) {
                QrCodeDialog(
                    roomId    = currentRoomId!!,
                    password  = password,
                    isDark    = isDark,
                    onDismiss = { showQrDialog = false },
                    strings   = strings
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onDismiss,
            colors = blackButtonColors,
            modifier = Modifier.fillMaxWidth()
        ) { Text(strings.close) }
    }
}

@Composable
fun TextWidget(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = "$text:",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Left
    )
}

@Composable
fun QrCodeDialog(
    roomId: String,
    password: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    strings: AppTranslations
) {
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White else Color.Black

    // formát: "roomId|password" — ConnectScreen splitne podle "|"
    val qrContent   = "$roomId|$password"
    val qrVariables = remember(qrContent) {
        QRCodeGenerator.generate(content = qrContent, sizePx = 512)
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
                text = strings.createRoomQR,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (qrVariables != null) {
                Image(
                    bitmap = qrVariables.bitmap.asImageBitmap(),
                    contentDescription = "QR kód místnosti",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chyba generování QR", color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ID: $roomId",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color.White else Color.Black,
                    contentColor   = if (isDark) Color.Black else Color.White
                )
            ) { Text(strings.close) }
        }
    }
}