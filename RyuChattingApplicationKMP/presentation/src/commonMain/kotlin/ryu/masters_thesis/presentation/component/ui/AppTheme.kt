package ryu.masters_thesis.presentation.component.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ryu.masters_thesis.core.configuration.AppTheme

private val LightColors = lightColorScheme(
    primary   = Color.Black,
    onPrimary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary   = Color.White,
    onPrimary = Color.Black,
)

// renamed from AppTheme to AppThemeWrapper cause of :Core new function
@Composable
fun AppThemeWrapper(
    themeMode: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppTheme.LIGHT  -> false
        AppTheme.DARK   -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content     = content,
    )
}