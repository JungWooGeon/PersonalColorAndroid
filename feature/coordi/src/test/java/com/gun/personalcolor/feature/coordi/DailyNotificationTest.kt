package com.gun.personalcolor.feature.coordi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class DailyNotificationTest {
    private val now = LocalDateTime.of(2026, 11, 1, 15, 30)

    @Test fun laterTodayFiresToday() =
        assertEquals(LocalDateTime.of(2026, 11, 1, 15, 35), nextTrigger(now, 15, 35))

    @Test fun earlierTodayFiresTomorrow() =
        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 0), nextTrigger(now, 8, 0))

    @Test fun exactlyNowFiresTomorrowNotTwice() =
        assertEquals(LocalDateTime.of(2026, 11, 2, 15, 30), nextTrigger(now, 15, 30))

    @Test fun crossesMonthEnd() =
        assertEquals(LocalDateTime.of(2026, 12, 1, 7, 0), nextTrigger(LocalDateTime.of(2026, 11, 30, 23, 0), 7, 0))

    @Test fun fallbackCopyWithoutTone() {
        val (short, long) = notificationText(null)
        assertTrue(short.isNotBlank())
        assertNull(long)
    }

    @Test fun copyUsesWeatherAndOutfit() {
        val (short, long) = notificationText(CoordiSummary("가을 뮤트", "살몬 베이지", "#D9A08A", "영등포구 10°C 이슬비", "오트밀 방수 코트 + 세이지 그린 터틀넥"))
        assertTrue(short.contains("살몬 베이지"))
        assertTrue(long!!.contains("영등포구 10°C 이슬비"))
        assertTrue(long.contains("오트밀 방수 코트"))
    }

    @Test fun copyWithoutWeather() {
        val (_, long) = notificationText(CoordiSummary("가을 뮤트", "살몬 베이지", "#D9A08A", null, "오트밀 울코트"))
        assertTrue(long!!.contains("\n오트밀 울코트"))
    }
}
