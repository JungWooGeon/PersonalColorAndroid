package com.gun.personalcolor.core.data

import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Verdict
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClosetRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun item(id: Long, name: String = "item$id") =
        ClosetItem(id = id, hex = "#AABBCC", name = name, category = "상의", toneId = "spring_light", verdict = Verdict.GOOD, score = 80)

    @Test fun addUpdateDeleteClear() = runTest {
        val repo = ClosetRepository(testStore(tmp))
        repo.add(item(1)); repo.add(item(2))
        assertEquals(listOf(2L, 1L), repo.items.first().map { it.id })

        repo.update(item(1, name = "renamed"))
        assertEquals(listOf("item2", "renamed"), repo.items.first().map { it.name })

        repo.delete(2)
        assertEquals(listOf(1L), repo.items.first().map { it.id })

        repo.clear()
        assertTrue(repo.items.first().isEmpty())
    }

    @Test fun updateUnknownIdChangesNothing() = runTest {
        val repo = ClosetRepository(testStore(tmp))
        repo.add(item(1))
        repo.update(item(42, name = "ghost"))
        assertEquals(listOf(item(1)), repo.items.first())
    }

    @Test fun capsAtTwoHundred() = runTest {
        val repo = ClosetRepository(testStore(tmp))
        (1L..205L).forEach { repo.add(item(it)) }
        val list = repo.items.first()
        assertEquals(200, list.size)
        assertEquals(205L, list.first().id)
        assertEquals(6L, list.last().id)
    }
}
