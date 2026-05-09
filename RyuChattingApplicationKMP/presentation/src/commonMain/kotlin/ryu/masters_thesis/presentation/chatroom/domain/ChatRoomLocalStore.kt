package ryu.masters_thesis.presentation.chatroom.domain

import kotlinx.coroutines.flow.Flow

interface ChatRoomLocalStore {
    fun getChatColor(roomId: String): Flow<String>
    suspend fun saveChatColor(roomId: String, colorHex: String)

    fun getNicknames(roomId: String): Flow<Map<String, String>>
    suspend fun saveNickname(roomId: String, userId: String, nickname: String)

    fun getWhitelist(roomId: String): Flow<Set<String>>
    suspend fun saveWhitelist(roomId: String, whitelist: Set<String>)
}