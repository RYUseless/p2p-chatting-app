package ryu.masters_thesis.presentation.component.ui

import androidx.compose.runtime.compositionLocalOf
import ryu.masters_thesis.core.configuration.AppSettings
import ryu.masters_thesis.core.configuration.DefaultSettings

val LocalAppSettings = compositionLocalOf<AppSettings> { DefaultSettings }