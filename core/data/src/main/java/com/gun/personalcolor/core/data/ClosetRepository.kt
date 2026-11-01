package com.gun.personalcolor.core.data

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gun.personalcolor.core.model.ClosetItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "내 옷장": the user's garments, newest first. The list lives in DataStore; cut-out photos are PNGs in [dir]
 * (app-private `filesDir/closet/`), never uploaded.
 */
@Singleton
class ClosetRepository(private val store: DataStore<Preferences>, private val dir: File) {
    @Inject constructor(@ClosetStore store: DataStore<Preferences>, @ApplicationContext context: Context) :
        this(store, File(context.filesDir, "closet"))

    val items: Flow<List<ClosetItem>> = store.safeData.map { decode(it) }

    suspend fun add(item: ClosetItem) {
        var dropped = emptyList<ClosetItem>()
        store.editOrNull {
            val all = listOf(item) + decode(it)
            dropped = all.drop(MAX)
            it[KEY] = Json.encodeToString(all.take(MAX))
        }
        deleteFiles(dropped)
    }

    suspend fun update(item: ClosetItem) = store.safeEdit { p -> p[KEY] = Json.encodeToString(decode(p).map { if (it.id == item.id) item else it }) }

    suspend fun delete(id: Long) {
        val gone = items.first().filter { it.id == id }
        // Keep the photo if the list write failed: the item is still shown.
        store.editOrNull { p -> p[KEY] = Json.encodeToString(decode(p).filterNot { it.id == id }) } ?: return
        deleteFiles(gone)
    }

    suspend fun clear() {
        store.editOrNull { it.remove(KEY) } ?: return
        withContext(Dispatchers.IO) { dir.listFiles()?.forEach { it.delete() } }
    }

    /** Writes [bitmap] as `<id>.png` and returns the file name to store in [ClosetItem.imageFile]. */
    suspend fun saveImage(bitmap: Bitmap, id: Long = System.currentTimeMillis()): String = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val name = "$id.png"
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        name
    }

    fun imageFile(name: String): File = File(dir, name)

    private suspend fun deleteFiles(items: List<ClosetItem>) = withContext(Dispatchers.IO) {
        items.forEach { it.imageFile?.let { n -> File(dir, n).delete() } }
    }

    private fun decode(p: Preferences) =
        p[KEY]?.let { runCatching { Json.decodeFromString<List<ClosetItem>>(it) }.getOrNull() } ?: emptyList()

    private companion object {
        val KEY = stringPreferencesKey("items")
        const val MAX = 200
    }
}
