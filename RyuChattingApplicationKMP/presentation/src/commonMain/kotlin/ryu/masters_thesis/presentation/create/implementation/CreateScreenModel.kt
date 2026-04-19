package ryu.masters_thesis.presentation.create.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.create.domain.CreateEvent
import ryu.masters_thesis.presentation.create.domain.CreateOneTimeEvent
import ryu.masters_thesis.presentation.create.domain.CreateRepository

class CreateScreenModel(
    private val repository: CreateRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(CreateState())
    val state: StateFlow<CreateState> = _state.asStateFlow()

    private val _oneTimeEvents = MutableSharedFlow<CreateOneTimeEvent>()
    val oneTimeEvents: SharedFlow<CreateOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        observeRepository()
        initRoom()
    }

    fun onEvent(event: CreateEvent) {
        when (event) {
            is CreateEvent.RoomNameChanged   -> _state.update { it.copy(roomName = event.name) }
            is CreateEvent.PasswordChanged   -> _state.update { it.copy(password = event.password) }
            is CreateEvent.CreateRoomClicked -> createRoom()
            is CreateEvent.ShowQrClicked     -> _state.update { it.copy(showQrDialog = true) }
            is CreateEvent.QrDialogDismissed -> _state.update { it.copy(showQrDialog = false) }
            is CreateEvent.DismissClicked    -> {
                repository.cleanup()
                screenModelScope.launch {
                    _oneTimeEvents.emit(CreateOneTimeEvent.Dismiss)
                }
            }
        }
    }

    override fun onDispose() {
        if (!_state.value.serverStarted) {
            repository.cleanup()
        }
    }

    private fun observeRepository() {
        screenModelScope.launch {
            repository.getCurrentRoomId().collect { roomId ->
                _state.update { it.copy(currentRoomId = roomId, roomName = roomId ?: it.roomName) }
            }
        }
        screenModelScope.launch {
            repository.getPasswordError().collect { error ->
                _state.update { it.copy(passwordError = error) }
            }
        }
    }

    private fun createRoom() {
        val state = _state.value
        if (state.password.isEmpty()) return
        screenModelScope.launch {
            repository.setRoomId(state.roomName)
            val roomId = repository.createRoom(state.password)
            _state.update { it.copy(serverStarted = true) }
            if (roomId != null) {
                _oneTimeEvents.emit(CreateOneTimeEvent.NavigateToChat(roomId))
            }
        }
    }

    private fun initRoom() {
        screenModelScope.launch {
            repository.initRoomId()
        }
    }

}