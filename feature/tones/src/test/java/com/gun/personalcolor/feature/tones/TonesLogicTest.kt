package com.gun.personalcolor.feature.tones

import com.gun.personalcolor.core.model.NamedColor
import org.junit.Assert.assertEquals
import org.junit.Test

class TonesLogicTest {
    @Test fun sharedColorsMatchesCloseHexOnly() {
        val a = listOf(NamedColor("코랄", "#FF7F50"), NamedColor("네이비", "#000080"))
        val b = listOf(NamedColor("코랄2", "#FF8055"))
        assertEquals(listOf("코랄"), sharedColors(a, b).map { it.name })
    }

    @Test fun sharedColorsIgnoresBadHexInsteadOfCrashing() {
        val a = listOf(NamedColor("bad", "not-a-color"), NamedColor("코랄", "#FF7F50"))
        val b = listOf(NamedColor("코랄", "#FF7F50"), NamedColor("bad", "#zz"))
        assertEquals(listOf("코랄"), sharedColors(a, b).map { it.name })
    }

    @Test fun similarTonesAreSeasonSiblingsPlusOpposite() {
        val tones = previewTones()
        val spring = tones.first { it.id == "spring_light" }
        assertEquals(listOf("spring_true", "summer_light"), similarTones(spring, tones).map { it.id })
    }
}
