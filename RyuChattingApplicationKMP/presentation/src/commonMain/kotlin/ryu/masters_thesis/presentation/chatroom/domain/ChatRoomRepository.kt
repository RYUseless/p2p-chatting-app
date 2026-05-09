package ryu.masters_thesis.presentation.chatroom.domain

import kotlinx.coroutines.flow.Flow
import ryu.masters_thesis.feature.bluetooth.domain.HandoffData

interface ChatRoomRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    fun getIsConnected(): Flow<Boolean>
    fun getIsVerified(): Flow<Boolean>
    fun getCurrentRoomId(): Flow<String?>
    fun getServerHandoffRequired(): Flow<Boolean>
    suspend fun promoteToServer(channelId: String, password: String)
    suspend fun sendMessage(text: String)
    suspend fun sendFile(fileName: String, bytes: ByteArray)

    // Room config
    fun getChatColor(): Flow<String>
    suspend fun saveChatColor(colorHex: String)
    fun getBlacklist(): Flow<List<String>>
    suspend fun saveBlacklist(blacklist: List<String>)

    // isSaved
    fun getIsSaved(): Flow<Boolean>
    suspend fun markRoomAsSaved()

    // Nicknames — populovány z příchozích MSG_NICKNAME paketů
    fun getNicknames(): Flow<Map<String, String>>

    // Pošle vlastní přezdívku peeru po verifikaci
    suspend fun sendNickname(nickname: String)

    // Connected peers
    fun getConnectedUserIds(): Flow<List<String>>

    fun cleanup()

    //handoff
    fun getHandoffEvent(): Flow<HandoffData>
    suspend fun triggerHandoffAndShutdown(successorMac: String)
}