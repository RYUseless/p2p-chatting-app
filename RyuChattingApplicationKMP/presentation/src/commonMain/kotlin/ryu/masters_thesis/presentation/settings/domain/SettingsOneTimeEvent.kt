package ryu.masters_thesis.presentation.settings.domain

// Jednorázové efekty – nezapisují se do SettingsState
sealed class SettingsOneTimeEvent {
    object Dismiss : SettingsOneTimeEvent()
}