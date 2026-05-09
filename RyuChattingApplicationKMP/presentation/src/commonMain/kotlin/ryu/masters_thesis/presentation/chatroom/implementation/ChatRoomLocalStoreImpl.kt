package ryu.masters_thesis.presentation.chatroom.implementation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ryu.masters_thesis.presentation.chatroom.domain.ChatRoomLocalStore

private const val DEFAULT_COLOR      = "#9E9E9E"
private const val NICKNAME_SEPARATOR = "::"

class ChatRoomLocalStoreImpl(
    private val dataStore: DataStore<Preferences>,
) : ChatRoomLocalStore {

    override fun getChatColor(roomId: String): Flow<String> =
        dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("color_$roomId")] ?: DEFAULT_COLOR
        }

    override suspend fun saveChatColor(roomId: String, colorHex: String) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("color_$roomId")] = colorHex
        }
    }

    override fun getNicknames(roomId: String): Flow<Map<String, String>> =
        dataStore.data.map { prefs ->
            prefs[stringSetPreferencesKey("nicknames_$roomId")]
                ?.associate { entry ->
                    val parts = entry.split(NICKNAME_SEPARATOR, limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else entry to entry
                }
                ?: emptyMap()
        }

    override suspend fun saveNickname(roomId: String, userId: String, nickname: String) {
        dataStore.edit { prefs ->
            val key     = stringSetPreferencesKey("nicknames_$roomId")
            val current = prefs[key]?.toMutableSet() ?: mutableSetOf()
            current.removeAll { it.startsWith("$userId$NICKNAME_SEPARATOR") }
            current.add("$userId$NICKNAME_SEPARATOR$nickname")
            prefs[key]  = current
        }
    }

    override fun getWhitelist(roomId: String): Flow<Set<String>> =
        dataStore.data.map { prefs ->
            prefs[stringSetPreferencesKey("whitelist_$roomId")] ?: emptySet()
        }

    override suspend fun saveWhitelist(roomId: String, whitelist: Set<String>) {
        dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("whitelist_$roomId")] = whitelist
        }
    }
}