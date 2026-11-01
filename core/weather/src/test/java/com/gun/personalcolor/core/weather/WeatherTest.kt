package com.gun.personalcolor.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherTest {
    @Test fun wmoLabels() {
        assertEquals("맑음", WeatherCodes.label(0))
        assertEquals("구름 조금", WeatherCodes.label(2))
        assertEquals("흐림", WeatherCodes.label(3))
        assertEquals("안개", WeatherCodes.label(45))
        assertEquals("이슬비", WeatherCodes.label(53))
        assertEquals("비", WeatherCodes.label(63))
        assertEquals("눈", WeatherCodes.label(75))
        assertEquals("눈", WeatherCodes.label(86))
        assertEquals("소나기", WeatherCodes.label(81))
        assertEquals("뇌우", WeatherCodes.label(95))
        assertEquals(WeatherCodes.RAIN, WeatherCodes.icon(65))
    }

    @Test fun parsesOpenMeteo() {
        val body = """
            {"latitude":37.56,"longitude":126.9,"timezone":"Asia/Seoul",
             "current_units":{"temperature_2m":"°C"},
             "current":{"time":"2026-11-01T09:00","interval":900,"temperature_2m":14.3,"apparent_temperature":12.1,
               "precipitation":0.0,"weather_code":2,"wind_speed_10m":7.6,"relative_humidity_2m":61},
             "daily":{"time":["2026-11-01"],"temperature_2m_max":[18.4],"temperature_2m_min":[8.9],
               "precipitation_probability_max":[30],"uv_index_max":[3.85],"weather_code":[3]}}
        """.trimIndent()
        val w = parseForecast(body, "마포구 망원동", 1000L)
        assertEquals(14.3, w.temp, 0.0)
        assertEquals(12.1, w.feelsLike, 0.0)
        assertEquals(8.9, w.min, 0.0)
        assertEquals(18.4, w.max, 0.0)
        assertEquals(30, w.precipProb)
        assertEquals("구름 조금", w.condition)
        assertEquals(3.85, w.uv!!, 0.0)
        assertEquals("망원동", w.shortPlace)
    }

    @Test fun shortPlaceLabel() {
        assertEquals("마포구 망원동", shortPlace("서울특별시", "서울특별시", "마포구", "망원동"))
        assertEquals("성남시 분당구", shortPlace("경기도", "성남시", "분당구", "판교역로"))
        assertEquals("제주특별자치도", shortPlace("제주특별자치도", null, null, null))
        assertNull(shortPlace(null, null, null, null))
    }

    // Regression: "구름 조금" next to "강수확률 100%" — the daily max counted hours already past. Only the rest of today counts.
    @Test fun precipChanceIsForTheRestOfToday() {
        val body = """
            {"current":{"time":"2026-11-01T14:15","temperature_2m":14.3,"weather_code":2},
             "hourly":{"time":["2026-11-01T00:00","2026-11-01T06:00","2026-11-01T14:00","2026-11-01T15:00","2026-11-01T21:00"],
               "precipitation_probability":[100,90,10,20,40]},
             "daily":{"precipitation_probability_max":[100],"weather_code":[61]}}
        """.trimIndent()
        val w = parseForecast(body, null, 0L)
        assertEquals(40, w.precipProb)
        assertEquals(61, w.dayCode)
        assertEquals(false, w.wet)
        // No hourly block (old response shape): the daily max.
        assertEquals(100, parseForecast(body.replace(Regex("\"hourly\":\\{[^}]*},"), ""), null, 0L).precipProb)
        assertEquals(true, w.copy(code = 61).wet)
        assertEquals(true, w.copy(precipProb = 50).wet)
        assertEquals(true, w.copy(code = 71).snowy)
        assertEquals(true, w.copy(dayCode = 73).snowy)
        assertEquals(false, w.copy(code = 61, dayCode = 73).snowy)
    }
}
