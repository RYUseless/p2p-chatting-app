package ryu.masters_thesis.presentation.settings.implementation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme
import ryu.masters_thesis.presentation.component.domain.AppSettingsSingleton
import ryu.masters_thesis.presentation.settings.domain.SettingsRepository

class SettingsRepositoryImpl : SettingsRepository {

    override fun getLanguage(): Flow<AppLanguage> =
        AppSettingsSingleton.settings.map { it.language }

    override fun getTheme(): Flow<AppTheme> =
        AppSettingsSingleton.settings.map { it.theme }

    override suspend fun setLanguage(language: AppLanguage) {
        AppSettingsSingleton.updateLanguage(language)
    }

    override suspend fun setTheme(theme: AppTheme) {
        AppSettingsSingleton.updateTheme(theme)
    }
}