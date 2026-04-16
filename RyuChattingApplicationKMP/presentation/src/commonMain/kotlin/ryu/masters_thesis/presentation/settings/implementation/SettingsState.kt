package ryu.masters_thesis.presentation.settings.implementation

import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme

/*
state is now getting its values from :core.commonmain.configration
→ no more hardcoded values → everything can be configured in :Core:configuration
 */

data class SettingsState(
    val language: AppLanguage        = AppLanguage.ENGLISH,
    val theme: AppTheme              = AppTheme.SYSTEM,
    val availableLanguages: List<AppLanguage> = AppLanguage.entries,
    val availableThemes: List<AppTheme>       = AppTheme.entries,
)