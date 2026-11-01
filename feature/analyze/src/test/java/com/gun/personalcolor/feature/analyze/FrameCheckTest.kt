package com.gun.personalcolor.feature.analyze

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameCheckTest {
    private val w = 120
    private val h = 160

    /** A synthetic frontal face, 55% of the frame high, centered on the guide. */
    private fun face(dx: Float = 0f, scale: Float = 1f): List<Offset> {
        val pts = MutableList(478) { Offset(0.5f + dx, GUIDE_CENTER_Y) }
        fun set(i: Int, x: Float, y: Float) { pts[i] = Offset(0.5f + dx + x * scale, GUIDE_CENTER_Y + y * scale) }
        set(10, 0f, -0.275f); set(152, 0f, 0.275f)          // top / chin
        set(234, -0.2f, 0f); set(454, 0.2f, 0f)              // face sides
        set(33, -0.12f, -0.06f); set(263, 0.12f, -0.06f)     // eye corners
        set(1, 0f, 0.02f)                                    // nose tip
        set(50, -0.1f, 0.05f); set(280, 0.1f, 0.05f); set(151, 0f, -0.2f)
        return pts
    }

    private fun frame(left: Int, right: Int = left) = IntArray(w * h) { i -> if (i % w < w / 2) left else right }

    private val skin = 0xFFD9A98C.toInt()

    @Test fun goodFramePasses() = assertTrue(checkFrame(listOf(face()), frame(skin), w, h).failed.toString(), checkFrame(listOf(face()), frame(skin), w, h).ok)

    @Test fun issuesAreDetected() {
        assertEquals(FrameCheck.Waiting, checkFrame(emptyList(), frame(skin), w, h))
        assertTrue(Check.SIZE in checkFrame(listOf(face(scale = 0.5f)), frame(skin), w, h).failed)
        assertTrue(Check.CENTER in checkFrame(listOf(face(dx = 0.2f)), frame(skin), w, h).failed)
        assertTrue(Check.LIGHT in checkFrame(listOf(face()), frame(0xFF302018.toInt()), w, h).failed)
        assertTrue(Check.EVEN in checkFrame(listOf(face()), frame(skin, 0xFF8A6050.toInt()), w, h).failed)
        assertTrue(Check.TINT in checkFrame(listOf(face()), frame(0xFFD0B030.toInt()), w, h).failed)
        assertTrue(Check.TINT in checkFrame(listOf(face()), frame(0xFFA0B0D8.toInt()), w, h).failed)
    }
}
