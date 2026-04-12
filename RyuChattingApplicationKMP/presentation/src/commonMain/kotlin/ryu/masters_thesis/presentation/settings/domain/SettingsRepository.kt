package ryu.masters_thesis.presentation.settings.domain

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getLanguage(): Flow<String>
    fun getTheme(): Flow<String>
    suspend fun setLanguage(language: String)
    suspend fun setTheme(theme: String)
}