package com.gun.personalcolor.core.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class HistoryStore
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SettingsStore
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ClosetStore

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton @HistoryStore
    fun history(@ApplicationContext c: Context): DataStore<Preferences> =
        store(c, "history")

    @Provides @Singleton @SettingsStore
    fun settings(@ApplicationContext c: Context): DataStore<Preferences> =
        store(c, "settings")

    @Provides @Singleton @ClosetStore
    fun closet(@ApplicationContext c: Context): DataStore<Preferences> =
        store(c, "closet")

    /** A corrupt file (rare, e.g. power loss mid-write) is reset instead of crashing every read. */
    private fun store(c: Context, name: String): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    ) { c.preferencesDataStoreFile(name) }
}

/** [edit] that logs and returns null on an I/O error (disk full, file locked) instead of throwing into a bare launch. */
internal suspend fun DataStore<Preferences>.editOrNull(block: suspend (MutablePreferences) -> Unit): Preferences? =
    try { edit(block) } catch (e: IOException) { Log.w("DataStore", "write failed", e); null }

/** [editOrNull] with the same non-null return type as [edit]; a failed write returns empty preferences. */
internal suspend fun DataStore<Preferences>.safeEdit(block: suspend (MutablePreferences) -> Unit): Preferences =
    editOrNull(block) ?: emptyPreferences()

/** [DataStore.data] that reads as empty on an I/O error instead of throwing into the collector. */
internal val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
