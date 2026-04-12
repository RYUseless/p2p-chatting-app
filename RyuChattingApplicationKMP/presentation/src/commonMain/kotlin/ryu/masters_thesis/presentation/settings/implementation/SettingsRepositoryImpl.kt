package ryu.masters_thesis.presentation.settings.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ryu.masters_thesis.presentation.settings.domain.SettingsRepository

// TODO DUMMY: až bude AppSettings z :core dostupný, nahradit skutečnou implementací
class SettingsRepositoryImpl : SettingsRepository {

    private val _language = MutableStateFlow("English")
    private val _theme    = MutableStateFlow("System")

    override fun getLanguage(): Flow<String> = _language
    override fun getTheme(): Flow<String>    = _theme

    override suspend fun setLanguage(language: String) { _language.value = language }
    override suspend fun setTheme(theme: String)       { _theme.value = theme }
}