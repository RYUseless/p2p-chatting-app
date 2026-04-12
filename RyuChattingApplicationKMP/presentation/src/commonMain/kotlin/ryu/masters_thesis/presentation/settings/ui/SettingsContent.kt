package ryu.masters_thesis.presentation.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ryu.masters_thesis.presentation.settings.domain.SettingsEvent
import ryu.masters_thesis.presentation.settings.implementation.SettingsState

@Composable
fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    // TODO DUMMY: isDark nahradit Theme systémem až bude dostupný
    isDark: Boolean = false,
) {
    val backgroundColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor       = if (isDark) Color.White       else Color.Black
    val buttonColors    = ButtonDefaults.buttonColors(
        containerColor = if (isDark) Color.White else Color.Black,
        contentColor   = if (isDark) Color.Black else Color.White
    )

    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedTheme    by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            // TODO DUMMY: překlad hardcoded
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // TODO DUMMY: překlad hardcoded
                text = "Language",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Box {
                TextButton(onClick = { expandedLanguage = !expandedLanguage }) {
                    Text(state.language, color = textColor)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                }
                DropdownMenu(
                    expanded = expandedLanguage,
                    onDismissRequest = { expandedLanguage = false }
                ) {
                    state.availableLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language) },
                            onClick = {
                                onEvent(SettingsEvent.LanguageChanged(language))
                                expandedLanguage = false
                            }
                        )
                    }
                }
            }
        }

        // Theme dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // TODO DUMMY: překlad hardcoded
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Box {
                TextButton(onClick = { expandedTheme = !expandedTheme }) {
                    Text(state.theme, color = textColor)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor)
                }
                DropdownMenu(
                    expanded = expandedTheme,
                    onDismissRequest = { expandedTheme = false }
                ) {
                    state.availableThemes.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme) },
                            onClick = {
                                onEvent(SettingsEvent.ThemeChanged(theme))
                                expandedTheme = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onEvent(SettingsEvent.DismissClicked) },
            colors = buttonColors,
            modifier = Modifier.fillMaxWidth()
        ) {
            // TODO DUMMY: překlad hardcoded
            Text("Close")
        }
    }
}