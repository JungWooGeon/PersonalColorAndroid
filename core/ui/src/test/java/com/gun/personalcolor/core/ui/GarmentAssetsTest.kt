package com.gun.personalcolor.core.ui

import com.gun.personalcolor.core.model.CLOTHING_KINDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every garment kind ships its painted art: there is no vector fallback any more, only a plain chip. */
class GarmentAssetsTest {
    private val dir = File("src/main/assets/garments")

    @Test fun everyKindHasNormalAndDarkArt() {
        assertTrue("run from the module dir: ${dir.absolutePath}", dir.isDirectory)
        val files = dir.list()!!.toSet()
        val missing = CLOTHING_KINDS.keys.flatMap { k -> listOf("${k}_n.webp", "${k}_d.webp") }.filter { it !in files }
        assertEquals(emptyList<String>(), missing)
    }

    @Test fun parseHexHandlesGoodAndBadInput() {
        assertEquals(0xFFC19A6B.toInt(), parseHex("#C19A6B"))
        assertEquals(0xFFC19A6B.toInt(), parseHex(" c19a6b "))
        assertEquals(0x80112233.toInt(), parseHex("#80112233"))
        assertEquals(null, parseHex("#12345"))
        assertEquals(null, parseHex("#GGGGGG"))
        assertEquals(null, parseHex(""))
    }
}
