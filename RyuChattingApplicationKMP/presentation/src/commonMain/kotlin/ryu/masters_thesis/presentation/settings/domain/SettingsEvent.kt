package ryu.masters_thesis.presentation.settings.domain

// Všechny akce které může uživatel na SettingsScreen provést
sealed class SettingsEvent {
    data class LanguageChanged(val language: String) : SettingsEvent()
    data class ThemeChanged(val theme: String) : SettingsEvent()
    object DismissClicked : SettingsEvent()
}