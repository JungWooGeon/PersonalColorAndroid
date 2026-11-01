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
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Verdict
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

    /** Writes are called from bare viewModelScope.launch in features: an I/O error must not escape. */
    @Test fun writesDoNotThrow() = runTest {
        HistoryRepository(broken).apply { add(HistoryEntry("spring_light", ResultSource.QUIZ, 1)); delete(1); clear() }
        UserPreferencesRepository(broken).apply { setMyTone("spring_light"); toggleFavorite("x"); reset() }
        val closet = ClosetRepository(broken, java.io.File("build/tmp/closet-test"))
        closet.add(ClosetItem(1, "#FFFFFF", "n", "상의", "spring_light", Verdict.GOOD, 90))
        closet.update(ClosetItem(1, "#FFFFFF", "n", "상의", "spring_light", Verdict.GOOD, 90))
        closet.delete(1); closet.clear()
    }
}
