package com.gun.personalcolor.feature.learn

import org.junit.Assert.assertEquals
import org.junit.Test

class LearnUiStateTest {
    @Test fun nullCategoryShowsAll() = assertEquals(5, LearnUiState(previewArticles).visible.size)

    @Test fun categoryFilters() =
        assertEquals(listOf("a0", "a3"), LearnUiState(previewArticles, "기초").visible.map { it.id })
}
