package com.gun.personalcolor.core.vision

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class BitmapLoaderTest {
    @Test fun smallImageKeepsSize() = assertEquals(1f, BitmapLoader.fitScale(800, 600, 1280))

    @Test fun longSideScaledDownToMax() {
        // inSampleSize leaves e.g. 2016 px; the loader must still end at ≤ maxSide.
        val s = BitmapLoader.fitScale(1512, 2016, 1280)
        assertEquals(1280, (2016 * s).roundToInt())
        assertEquals(960, (1512 * s).roundToInt())
    }
}
