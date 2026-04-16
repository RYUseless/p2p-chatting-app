package ryu.masters_thesis.presentation.settings.domain

import ryu.masters_thesis.presentation.component.domain.ThemeMode

data class SettingsState(
    val language: String         = "English",
    val theme: ThemeMode         = ThemeMode.SYSTEM,
    val availableLanguages: List<String>    = listOf("English", "Czech"),
    val availableThemes: List<ThemeMode>    = ThemeMode.entries,
)