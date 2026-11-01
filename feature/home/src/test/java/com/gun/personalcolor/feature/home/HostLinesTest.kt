package com.gun.personalcolor.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HostLinesTest {
    @Test fun particle() {
        assertEquals("로", ro("퍼플"))
        assertEquals("로", ro("버건디"))
        assertEquals("로", ro("코랄"))
        assertEquals("으로", ro("올리브 그린"))
        assertEquals("로", ro("RED"))
    }

    @Test fun firstRunLines() {
        assertEquals("내 색, 같이 찾아봐요", hostLines(9, null, null, null).first())
    }
}
