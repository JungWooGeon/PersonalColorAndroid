package com.gun.personalcolor.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gun.personalcolor.core.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(@HistoryStore private val store: DataStore<Preferences>) {
    val entries: Flow<List<HistoryEntry>> = store.safeData.map { decode(it) }

    suspend fun add(entry: HistoryEntry) = store.safeEdit { it[KEY] = Json.encodeToString((listOf(entry) + decode(it)).take(MAX)) }

    suspend fun delete(timeMillis: Long) = store.safeEdit { prefs ->
        prefs[KEY] = Json.encodeToString(decode(prefs).filterNot { it.timeMillis == timeMillis })
    }

    suspend fun clear() = store.safeEdit { it.remove(KEY) }

    private fun decode(p: Preferences) =
        p[KEY]?.let { runCatching { Json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull() } ?: emptyList()

    private companion object {
        val KEY = stringPreferencesKey("entries")
        const val MAX = 50
    }
}
