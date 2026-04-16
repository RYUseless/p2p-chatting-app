package ryu.masters_thesis.presentation.settings.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme

interface SettingsRepository {
    fun getLanguage(): Flow<AppLanguage>
    fun getTheme(): Flow<AppTheme>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
}