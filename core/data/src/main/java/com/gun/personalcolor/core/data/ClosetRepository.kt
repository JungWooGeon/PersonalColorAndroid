package com.gun.personalcolor.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gun.personalcolor.core.model.ClosetItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** "내 옷장": saved color-check results, newest first. */
@Singleton
class ClosetRepository @Inject constructor(@ClosetStore private val store: DataStore<Preferences>) {
    val items: Flow<List<ClosetItem>> = store.data.map { decode(it) }

    suspend fun add(item: ClosetItem) = store.edit { it[KEY] = Json.encodeToString((listOf(item) + decode(it)).take(MAX)) }
    suspend fun update(item: ClosetItem) = store.edit { p -> p[KEY] = Json.encodeToString(decode(p).map { if (it.id == item.id) item else it }) }
    suspend fun delete(id: Long) = store.edit { p -> p[KEY] = Json.encodeToString(decode(p).filterNot { it.id == id }) }
    suspend fun clear() = store.edit { it.remove(KEY) }

    private fun decode(p: Preferences) =
        p[KEY]?.let { runCatching { Json.decodeFromString<List<ClosetItem>>(it) }.getOrNull() } ?: emptyList()

    private companion object {
        val KEY = stringPreferencesKey("items")
        const val MAX = 200
    }
}
