package com.gun.personalcolor.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class HistoryStore
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SettingsStore
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ClosetStore

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton @HistoryStore
    fun history(@ApplicationContext c: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { c.preferencesDataStoreFile("history") }

    @Provides @Singleton @SettingsStore
    fun settings(@ApplicationContext c: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { c.preferencesDataStoreFile("settings") }

    @Provides @Singleton @ClosetStore
    fun closet(@ApplicationContext c: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { c.preferencesDataStoreFile("closet") }
}
