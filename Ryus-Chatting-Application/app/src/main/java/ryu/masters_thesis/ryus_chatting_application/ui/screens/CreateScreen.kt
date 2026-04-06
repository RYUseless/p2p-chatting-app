package ryu.masters_thesis.ryus_chatting_application.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.AppTranslations
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.logic.QRCode.QRCodeGenerator
import ryu.masters_thesis.ryus_chatting_application.ui.theme.RyusChattingApplicationTheme

data class FieldConfig(
    val label: String,
    val defaultValue: String,
    val isPassword: Boolean = false
)

@SuppressLint("RememberReturnType")
@Composable
fun CreateScreen(
    onDismiss: () -> Unit,
    settings: AppSettings
) {
    val strings = getTranslations(settings.language)
    val isDark = settings.isDarkTheme()

    //todo: migrovat do config folderu
    //val surfaceColor     = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5) //check if needed
    val backgroundColor  = if (isDark) Color(0xFF121212) else Color.White
    val textColor        = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )
    //qrcode:
    var showQrDialog by remember { mutableStateOf(false) }


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

        // inputy:
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {

            val fields = listOf(
                FieldConfig(label = "Chatroom Name", defaultValue = "RyuRoom-ID"),
                FieldConfig(label = "Room Password", defaultValue = "", isPassword = true)
            )

            val fieldValues = remember {
                mutableStateMapOf(*fields.map { it.label to it.defaultValue }.toTypedArray())
            }

            //foreach loop co vykresli chatroom name a roomPassword textinputy
            fields.forEach { field ->
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    // funkce pro nadpisy
                    TextWidget(field.label, textColor, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = fieldValues[field.label] ?: "",
                        onValueChange = { fieldValues[field.label] = it },
                        visualTransformation = if (field.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // qr code button
            Button(
                onClick = {showQrDialog = true  },
                enabled = fields
                    .filter { it.isPassword }
                    .all { fieldValues[it.label]?.isNotEmpty() == true },
                colors = blackButtonColors,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show QR Code") }

            //showqr
            if (showQrDialog) {
                QrCodeDialog(
                    roomName = fieldValues["Chatroom Name"] ?: "",
                    password = fieldValues["Room Password"] ?: "",
                    isDark = isDark,
                    onDismiss = { showQrDialog = false },
                    strings = strings
                )
            }


        }


        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onDismiss,
            colors = blackButtonColors,
            modifier = Modifier
                .fillMaxWidth()
        ) { Text(strings.close) }

    }
}

//text widget
@Composable
fun TextWidget(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
    // Nadpisy pro user inputy (specificky nad)
) {
    Text(
        text = "$text:",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Left
    )
}

//QR CODE
@Composable
fun QrCodeDialog(
    roomName: String,
    password: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    strings: AppTranslations
) {
    //TODO: check this, move to settings if needed
    val backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor       = if (isDark) Color.White else Color.Black

    // formát: "roomName|password" — ConnectScreen splitne podle "|"
    val qrContent = password
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
                    contentDescription = "QR kód",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                // fallback pokud generování selže
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
                text = roomName,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
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


@Preview(showBackground = true, widthDp = 320)
@Composable
fun CreateScreenPreview() {
    RyusChattingApplicationTheme { CreateScreen(onDismiss = {}, settings = AppSettings()) }
}