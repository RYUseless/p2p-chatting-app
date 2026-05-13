package ryu.masters_thesis.presentation.chatroom.implementation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomEvent
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomOneTimeEvent
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomRepository
import ryu.masters_thesis.presentation.component.domain.AppSettingsSingleton
import ryu.masters_thesis.feature.bluetooth.domain.HandoffData
//import ryu.masters_thesis.feature.bluetoothFinderProtocol.domain.FinderResponder
import ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain.NeighbourProtocol

class ChatRoomScreenModel(
    private val roomName:          String,
    password:                      String,
    private val isServer:          Boolean,
    private val repository:        ChatRoomRepository,
    private val neighbourProtocol: NeighbourProtocol,
) : ScreenModel {

    private val _state = MutableStateFlow(
        ChatRoomState(
            roomName     = roomName,
            roomPassword = password,
            isServer     = isServer,
        )
    )
    val state: StateFlow<ChatRoomState> = _state.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()
    private val _oneTimeEvents = Channel<ChatRoomOneTimeEvent>(capacity = Channel.BUFFERED)
    val oneTimeEvents: Flow<ChatRoomOneTimeEvent> = _oneTimeEvents.receiveAsFlow()

    // Ochrana proti "spamování" tlačítka zpět
    private var isLeaving = false

    //init {
    //    observeRepository()
    //}
    init {
        // Vynutíme, aby state neobsahoval žádné staré zprávy před začátkem pozorování
        _state.update { it.copy(messages = emptyList(), isConnected = false, isVerified = false) }
        observeRepository()
    }

    fun onEvent(event: ChatRoomEvent) {
        when (event) {
            is ChatRoomEvent.MessageInputChanged -> _messageInput.update { event.text }
            is ChatRoomEvent.SendMessageClicked  -> sendMessage()
            is ChatRoomEvent.EmojiMenuToggled    -> _state.update { it.copy(showEmojiMenu = !it.showEmojiMenu) }
            is ChatRoomEvent.EmojiSelected -> {
                _messageInput.update { it + event.emoji }
                _state.update { it.copy(showEmojiMenu = false) }
            }
            is ChatRoomEvent.AttachFileClicked   -> screenModelScope.launch {
                // Místo emit() používáme send()
                _oneTimeEvents.send(ChatRoomOneTimeEvent.OpenFilePicker)
            }
            is ChatRoomEvent.BackClicked         -> leaveRoom()
            is ChatRoomEvent.InfoClicked         -> _state.update { it.copy(showInfoSheet = true) }
            is ChatRoomEvent.SearchClicked       -> { /* TODO */ }
            is ChatRoomEvent.InfoSheetDismissed  -> _state.update { it.copy(showInfoSheet = false) }
            is ChatRoomEvent.ShowQrClicked       -> _state.update { it.copy(showQrDialog = true) }
            is ChatRoomEvent.QrDialogDismissed   -> _state.update { it.copy(showQrDialog = false) }
            is ChatRoomEvent.ChatColorChanged    -> screenModelScope.launch {
                _state.update { it.copy(chatColorHex = event.colorHex) }
                repository.saveChatColor(event.colorHex)
            }
            is ChatRoomEvent.NicknameChanged     -> {
                _state.update { it.copy(nicknames = it.nicknames + (event.userId to event.nickname)) }
            }
            is ChatRoomEvent.BlacklistToggled    -> screenModelScope.launch {
                val current = _state.value.blacklist.toMutableList()
                if (event.userId in current) current.remove(event.userId) else current.add(event.userId)
                _state.update { it.copy(blacklist = current) }
                repository.saveBlacklist(current)
            }
            is ChatRoomEvent.MarkRoomSaved       -> screenModelScope.launch {
                repository.markRoomAsSaved()
            }
        }
    }

    private fun leaveRoom() {
        if (isLeaving) return
        isLeaving = true
        _state.update { it.copy(isLeaving = true) }
        screenModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            neighbourProtocol.stopDiscovery()
            repository.cleanup()
            _oneTimeEvents.send(ChatRoomOneTimeEvent.NavigateBack)
        }
    }

    //redo
    private fun observeRepository() {
        screenModelScope.launch {
            repository.getMessages().collect { messages ->
                _state.update { it.copy(
                    messages          = messages,
                    isLoadingMessages = false,
                )}
            }
        }
        screenModelScope.launch {
            repository.getIsConnected().collect { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
        screenModelScope.launch {
            var wasVerified = false
            repository.getIsVerified().collect { verified ->
                _state.update { it.copy(isVerified = verified) }
                when {
                    verified && !wasVerified -> {
                        neighbourProtocol.startDiscovery()
                        val nickname = AppSettingsSingleton.settings.value.userNickname
                        if (nickname.isNotBlank()) repository.sendNickname(nickname)
                    }
                    !verified && wasVerified -> {
                        neighbourProtocol.stopDiscovery()
                    }
                }
                wasVerified = verified
            }
        }
        screenModelScope.launch {
            repository.getCurrentRoomId().collect { roomId ->
                _state.update { it.copy(currentRoomId = roomId) }
            }
        }

        if (!isServer) {
            screenModelScope.launch {
                repository.getHandoffEvent().collect { event ->
                    when (event) {
                        is HandoffData.PromoteToServer ->
                            _oneTimeEvents.send(ChatRoomOneTimeEvent.ReloadAsServer(event.roomId))
                        is HandoffData.SearchNewServer ->
                            _oneTimeEvents.send(ChatRoomOneTimeEvent.NavigateBack)
                    }
                }
            }
            screenModelScope.launch {
                repository.getServerHandoffRequired().collect { required ->
                    if (required) {
                        neighbourProtocol.stopDiscovery()
                        repository.promoteToServer(roomName, _state.value.roomPassword)
                        _oneTimeEvents.send(ChatRoomOneTimeEvent.ReloadAsServer(roomName))
                    }
                }
            }
        }

        screenModelScope.launch {
            repository.getChatColor().collect { hex ->
                _state.update { it.copy(chatColorHex = hex) }
            }
        }
        screenModelScope.launch {
            repository.getBlacklist().collect { blacklist ->
                _state.update { it.copy(blacklist = blacklist) }
            }
        }
        screenModelScope.launch {
            repository.getIsSaved().collect { isSaved ->
                _state.update { it.copy(isSaved = isSaved) }
            }
        }
        screenModelScope.launch {
            repository.getNicknames().collect { nicknames ->
                _state.update { it.copy(nicknames = nicknames) }
            }
        }
        screenModelScope.launch {
            repository.getConnectedUserIds().collect { ids ->
                _state.update { it.copy(connectedUserIds = ids) }
            }
        }
    }

    private fun sendMessage() {
        val text = _messageInput.value.trim()
        if (text.isBlank()) return
        _messageInput.update { "" }
        screenModelScope.launch {
            repository.sendMessage(text)
        }
    }

    //nova fce
    override fun onDispose() {
        neighbourProtocol.stopDiscovery()
    }
}