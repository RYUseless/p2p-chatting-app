package ryu.masters_thesis.presentation.settings.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme

interface SettingsRepository {
    fun getLanguage(): Flow<AppLanguage>
    fun getTheme(): Flow<AppTheme>
    fun getNickname(): Flow<String>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setNickname(nickname: String)
}