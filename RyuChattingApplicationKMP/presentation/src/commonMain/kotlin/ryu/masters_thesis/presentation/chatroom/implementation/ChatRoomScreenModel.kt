package ryu.masters_thesis.presentation.chatroom.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository

class ChatRoomScreenModel(
    private val roomName: String,
    private val repository: ChatRoomRepository,
) : ScreenModel {

    // Stav UI – StateFlow, ChatRoomContent collectuje přes collectAsState()
    private val _state = MutableStateFlow(ChatRoomState(roomName = roomName))
    val state: StateFlow<ChatRoomState> = _state.asStateFlow()

    // Jednorázové eventy – SharedFlow, UI poslouchá přes LaunchedEffect
    private val _oneTimeEvents = MutableSharedFlow<ChatRoomOneTimeEvent>()
    val oneTimeEvents: SharedFlow<ChatRoomOneTimeEvent> = _oneTimeEvents.asSharedFlow()

    init {
        observeRepository()
    }

    // Jediný vstupní bod pro UI akce
    fun onEvent(event: ChatRoomEvent) {
        when (event) {
            is ChatRoomEvent.MessageInputChanged -> _state.update { it.copy(messageInput = event.text) }
            is ChatRoomEvent.SendMessageClicked  -> sendMessage()
            is ChatRoomEvent.EmojiMenuToggled    -> _state.update { it.copy(showEmojiMenu = !it.showEmojiMenu) }
            is ChatRoomEvent.EmojiSelected       -> {
                _state.update { it.copy(
                    messageInput = it.messageInput + event.emoji,
                    showEmojiMenu = false,
                )}
            }
            is ChatRoomEvent.AttachFileClicked   -> {
                screenModelScope.launch {
                    // TODO DUMMY: file picker bude v :core
                    _oneTimeEvents.emit(ChatRoomOneTimeEvent.OpenFilePicker)
                }
            }
            is ChatRoomEvent.BackClicked         -> {
                screenModelScope.launch {
                    _oneTimeEvents.emit(ChatRoomOneTimeEvent.NavigateBack)
                }
            }
            is ChatRoomEvent.InfoClicked         -> _state.update { it.copy(showInfoSheet = true) }
            is ChatRoomEvent.SearchClicked       -> { /* TODO: search implementace */ }
            is ChatRoomEvent.InfoSheetDismissed  -> _state.update { it.copy(showInfoSheet = false) }
            is ChatRoomEvent.ShowQrClicked       -> _state.update { it.copy(showQrDialog = true) }
            is ChatRoomEvent.QrDialogDismissed   -> _state.update { it.copy(showQrDialog = false) }
            is ChatRoomEvent.ChatColorChanged    -> _state.update { it.copy(chatColorHex = event.colorHex) }
            is ChatRoomEvent.NicknameChanged     -> {
                _state.update { it.copy(
                    nicknames = it.nicknames + (event.userId to event.nickname)
                )}
            }
            is ChatRoomEvent.WhitelistToggled    -> {
                _state.update {
                    val updated = if (event.userId in it.whitelist)
                        it.whitelist - event.userId
                    else
                        it.whitelist + event.userId
                    it.copy(whitelist = updated)
                }
            }
        }
    }

    override fun onDispose() {
        repository.cleanup()
    }

    private fun observeRepository() {
        screenModelScope.launch {
            repository.getMessages().collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
        screenModelScope.launch {
            repository.getIsConnected().collect { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
        screenModelScope.launch {
            repository.getIsVerified().collect { verified ->
                _state.update { it.copy(isVerified = verified) }
            }
        }
        screenModelScope.launch {
            repository.getCurrentRoomId().collect { roomId ->
                _state.update { it.copy(currentRoomId = roomId) }
            }
        }
    }

    private fun sendMessage() {
        val text = _state.value.messageInput.trim()
        if (text.isBlank()) return
        screenModelScope.launch {
            repository.sendMessage(text)
            _state.update { it.copy(messageInput = "") }
        }
    }
}