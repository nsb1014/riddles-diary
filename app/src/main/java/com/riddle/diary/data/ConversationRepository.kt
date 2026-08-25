package com.riddle.diary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.conversationDataStore: DataStore<Preferences> by preferencesDataStore("conversation")

@Serializable
data class DiaryEntry(
    val id: String,
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vanishing: Boolean = false
) {
    @Serializable
    enum class Role { USER, DIARY }
}

class ConversationRepository(private val context: Context) {
    private val entriesKey = stringPreferencesKey("entries_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val entries: Flow<List<DiaryEntry>> = context.conversationDataStore.data.map { prefs ->
        val raw = prefs[entriesKey] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<DiaryEntry>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun append(entry: DiaryEntry) {
        context.conversationDataStore.edit { prefs ->
            val current = prefs[entriesKey]?.let {
                runCatching { json.decodeFromString<List<DiaryEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[entriesKey] = json.encodeToString(current + entry)
        }
    }

    suspend fun replaceAll(entries: List<DiaryEntry>) {
        context.conversationDataStore.edit { prefs ->
            prefs[entriesKey] = json.encodeToString(entries)
        }
    }

    suspend fun clear() {
        context.conversationDataStore.edit { it.remove(entriesKey) }
    }
}
