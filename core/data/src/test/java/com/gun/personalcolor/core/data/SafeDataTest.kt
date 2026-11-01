package com.gun.personalcolor.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** A DataStore read error must surface as defaults, not as an exception in the screen's collector. */
class SafeDataTest {
    private val broken = object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("disk") }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = throw IOException("disk")
    }

    @Test fun historyReadsEmpty() = runTest { assertTrue(HistoryRepository(broken).entries.first().isEmpty()) }

    @Test fun preferencesReadDefaults() = runTest {
        assertEquals(com.gun.personalcolor.core.model.UserPreferences(), UserPreferencesRepository(broken).preferences.first())
    }
}
