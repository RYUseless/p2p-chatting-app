package ryu.masters_thesis.presentation.settings.domain

import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme

sealed class SettingsEvent {
    data class LanguageChanged(val language: AppLanguage) : SettingsEvent()
    data class ThemeChanged(val theme: AppTheme)          : SettingsEvent()
    data class NicknameChanged(val nickname: String)      : SettingsEvent()
    object DismissClicked                                 : SettingsEvent()
}