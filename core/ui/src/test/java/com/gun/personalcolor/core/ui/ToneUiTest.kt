package com.gun.personalcolor.core.ui

import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.model.Tone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

/** The 12 bundled tones (core/data assets) dress 틴티 in their own colors: picks resolve, scarf has exactly 4 chips. */
class ToneUiTest {
    private val tones: List<Tone> by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(File("../data/src/main/assets/tones.json").readText()).jsonObject
        root["tones"]!!.let { json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(Tone.serializer()), it) }
    }

    @Test fun everyToneDressesTinty() {
        assertEquals(12, tones.size)
        for (t in tones) {
            val c = t.tintyColors
            assertEquals(t.id, 4, c.scarf.size)
            assertNotEquals("${t.id} fell back to the brand colors (renamed color in tones.json?)", TintyColors.Brand, c)
        }
    }

    @Test fun tooFewColorsFallsBackToBrandAndEmptyToneDoesNotCrash() {
        val t = tones.first()
        assertEquals(TintyColors.Brand, t.copy(id = "x", bestColors = t.bestColors.take(3)).tintyColors)
        val empty = t.copy(id = "x", bestColors = emptyList())
        assertEquals(TintyColors.Brand, empty.tintyColors)
        empty.accent // grey, no NoSuchElementException
    }
}
