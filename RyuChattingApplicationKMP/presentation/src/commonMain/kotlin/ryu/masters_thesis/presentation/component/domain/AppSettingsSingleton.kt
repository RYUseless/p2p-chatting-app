package ryu.masters_thesis.presentation.component.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppSettings
import ryu.masters_thesis.core.configuration.AppTheme
import ryu.masters_thesis.core.configuration.DefaultSettings

object AppSettingsSingleton {
    private val _settings = MutableStateFlow(DefaultSettings)
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val themeMode    = _settings.map { it.theme }
    val languageMode = _settings.map { it.language }

    fun updateTheme(theme: AppTheme) {
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateLanguage(language: AppLanguage) {
        _settings.value = _settings.value.copy(language = language)
    }
}