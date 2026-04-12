package ryu.masters_thesis.presentation.settings.implementation

// Immutable snapshot – jediný zdroj pravdy pro SettingsContent
// TODO DUMMY: language a theme jsou String, až bude AppSettings z :core nahradit enummy
data class SettingsState(
    val language: String = "English",
    val theme: String = "System",
    val availableLanguages: List<String> = listOf("English", "Czech"),
    val availableThemes: List<String> = listOf("System", "Light", "Dark"),
)