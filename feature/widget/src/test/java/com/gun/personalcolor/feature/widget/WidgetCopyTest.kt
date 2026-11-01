package com.gun.personalcolor.feature.widget

import com.gun.personalcolor.feature.widget.LookWidgets.Sky
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCopyTest {
    @Test fun sky() {
        assertEquals(Sky.rain, LookWidgets.skyOf("영등포구 9°C 이슬비", 14))
        assertEquals(Sky.rain, LookWidgets.skyOf("망원동 -2°C 눈", 9))
        assertEquals(Sky.cloudy, LookWidgets.skyOf("망원동 12°C 흐림", 9))
        assertEquals(Sky.sunny, LookWidgets.skyOf("망원동 17°C 맑음", 6))
        assertEquals(Sky.night, LookWidgets.skyOf("망원동 17°C 비", 22))
        assertEquals(Sky.night, LookWidgets.skyOf(null, 5))
    }

    @Test fun copy() {
        assertEquals("비 오는 날, 와인 한 방울", LookWidgets.lineOf(Sky.rain, "이슬비", 9, "와인"))
        assertEquals("흐린 날엔 와인으로", LookWidgets.lineOf(Sky.cloudy, "흐림", 12, "와인"))
        assertEquals("흐린 날엔 코랄로", LookWidgets.lineOf(Sky.cloudy, "흐림", 12, "코랄"))
        assertEquals("버건디로 따뜻하게", LookWidgets.lineOf(Sky.sunny, "맑음", 2, "버건디"))
        assertEquals("오렌지로 따뜻하게", LookWidgets.lineOf(Sky.sunny, "맑음", 2, "오렌지"))
        assertEquals("내일은 라벤더", LookWidgets.lineOf(Sky.night, null, 2, "라벤더"))
        assertEquals("오늘의 룩", LookWidgets.lineOf(Sky.sunny, null, 20, null))
        assertEquals("눈 오는 날, 버건디 한 방울", LookWidgets.lineOf(Sky.rain, "눈", 0, "버건디"))
    }
}
