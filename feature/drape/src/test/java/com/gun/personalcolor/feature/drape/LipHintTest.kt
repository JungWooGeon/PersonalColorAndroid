package com.gun.personalcolor.feature.drape

import org.junit.Assert.assertEquals
import org.junit.Test

class LipHintTest {
    @Test fun hintMatchesTab() {
        assertEquals("생기 있어 보이는 색이 잘 어울려요", lipHint(0))
        assertEquals("얼굴이 칙칙해 보일 수 있어요", lipHint(1)) // WORST must never praise the color
        assertEquals(lipHint(2), lipHint(5))
    }
}
