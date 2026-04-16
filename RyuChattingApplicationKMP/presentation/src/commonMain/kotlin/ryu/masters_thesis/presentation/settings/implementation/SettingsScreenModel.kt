package ryu.masters_thesis.presentation.settings.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
// mhm implement later on
import ryu.masters_thesis.core.configuration.AppLanguage
import ryu.masters_thesis.core.configuration.AppTheme
import ryu.masters_thesis.presentation.settings.domain.SettingsEvent
import ryu.masters_thesis.presentation.settings.domain.SettingsOneTimeEvent
import ryu.masters_thesis.presentation.settings.domain.SettingsRepository

class SettingsScreenModel(
    private val repository: SettingsRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _oneTimeEvents = MutableSharedFlow<SettingsOneTimeEvent>()
    val oneTimeEvents: SharedFlow<SettingsOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        observeRepository()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LanguageChanged -> screenModelScope.launch {
                repository.setLanguage(event.language)
            }
            is SettingsEvent.ThemeChanged -> screenModelScope.launch {
                repository.setTheme(event.theme)
            }
            is SettingsEvent.DismissClicked -> screenModelScope.launch {
                _oneTimeEvents.emit(SettingsOneTimeEvent.Dismiss)
            }
        }
    }

    private fun observeRepository() {
        screenModelScope.launch {
            repository.getLanguage().collect { language ->
                _state.update { it.copy(language = language) }
            }
        }
        screenModelScope.launch {
            repository.getTheme().collect { theme ->
                _state.update { it.copy(theme = theme) }
            }
        }
    }
}