package com.gun.personalcolor.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.ResultSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HistoryRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun entry(t: Long) = HistoryEntry(toneId = "spring_light", source = ResultSource.QUIZ, timeMillis = t)

    @Test fun addKeepsNewestFirst() = runTest {
        val repo = HistoryRepository(testStore(tmp))
        repo.add(entry(1)); repo.add(entry(2)); repo.add(entry(3))
        assertEquals(listOf(3L, 2L, 1L), repo.entries.first().map { it.timeMillis })
    }

    @Test fun capsAtFifty() = runTest {
        val repo = HistoryRepository(testStore(tmp))
        (1L..55L).forEach { repo.add(entry(it)) }
        val list = repo.entries.first()
        assertEquals(50, list.size)
        assertEquals(55L, list.first().timeMillis)
        assertEquals(6L, list.last().timeMillis)
    }

    @Test fun deleteByTimeAndClear() = runTest {
        val repo = HistoryRepository(testStore(tmp))
        repo.add(entry(1)); repo.add(entry(2))
        repo.delete(1)
        assertEquals(listOf(2L), repo.entries.first().map { it.timeMillis })
        repo.delete(999) // unknown: no-op
        assertEquals(1, repo.entries.first().size)
        repo.clear()
        assertTrue(repo.entries.first().isEmpty())
    }

    @Test fun corruptJsonReadsAsEmptyAndIsRecoverable() = runTest {
        val store = testStore(tmp)
        store.edit { it[stringPreferencesKey("entries")] = "{not json" }
        val repo = HistoryRepository(store)
        assertTrue(repo.entries.first().isEmpty())
        repo.add(entry(7))
        assertEquals(listOf(7L), repo.entries.first().map { it.timeMillis })
    }
}
