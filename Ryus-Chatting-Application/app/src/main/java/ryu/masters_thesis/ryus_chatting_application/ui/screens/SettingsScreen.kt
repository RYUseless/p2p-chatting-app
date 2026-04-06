package ryu.masters_thesis.ryus_chatting_application.ui.screens

import androidx.compose.foundation.background
//import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
//import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
//import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.ryus_chatting_application.config.AppLanguage
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.AppTheme
import ryu.masters_thesis.ryus_chatting_application.config.getTranslations
import ryu.masters_thesis.ryus_chatting_application.config.isDarkTheme
import ryu.masters_thesis.ryus_chatting_application.ui.theme.RyusChattingApplicationTheme

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit
) {
    val isDark = settings.isDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val blackButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    val strings = getTranslations(settings.language)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        val titleModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 10.dp)

        val optionModifier = Modifier // k cemu toto bylo lel
            .fillMaxWidth()
            .padding(vertical = 6.dp)

        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            modifier = titleModifier
        )
        Spacer(modifier = Modifier.height(16.dp))

        var expandedLanguage by remember { mutableStateOf(false) }
        var expandedTheme    by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settingsLanguageLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Box {
                TextButton(onClick = { expandedLanguage = !expandedLanguage }) {
                    Text(settings.language.displayName, color = textColor)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                }
                DropdownMenu(
                    expanded = expandedLanguage,
                    onDismissRequest = { expandedLanguage = false }
                ) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.displayName) },
                            onClick = {
                                onSettingsChange(settings.copy(language = language))
                                expandedLanguage = false
                            }
                        )
                    }
                }
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settingsThemeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Box {
                TextButton(onClick = { expandedTheme = !expandedTheme }) {
                    Text(settings.theme.displayName, color = textColor)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                }
                DropdownMenu(
                    expanded = expandedTheme,
                    onDismissRequest = { expandedTheme = false }
                ) {
                    AppTheme.entries.forEach { theme ->        // ← AppLanguage → AppTheme
                        DropdownMenuItem(
                            text = { Text(theme.displayName) }, // ← language → theme
                            onClick = {
                                onSettingsChange(settings.copy(theme = theme))  // ← language → theme
                                expandedTheme = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        //exit yeeter
        Button(
            onClick = onDismiss,
            colors = blackButtonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.close)
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun SettingsScreenPreview() {
    RyusChattingApplicationTheme {
        SettingsScreen(
            onDismiss = {},
            settings = AppSettings(),
            onSettingsChange = {}
        )
    }
}