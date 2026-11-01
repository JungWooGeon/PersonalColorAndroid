package com.gun.personalcolor.core.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Today's weather at the user's (coarse) location. */
@Serializable data class TodayWeather(
    val placeLabel: String?,      // "마포구 망원동", null if geocoding failed
    val temp: Double,
    val feelsLike: Double,
    val min: Double,
    val max: Double,
    val precipProb: Int,          // 0..100, today's max
    val condition: String,        // Korean label, e.g. "맑음"
    val code: Int,                // WMO weather code
    val uv: Double?,
    val wind: Double,             // km/h
    val fetchedAtMillis: Long,
) {
    /** Icon key, see [WeatherCodes.icon]. */
    val icon: String get() = WeatherCodes.icon(code)
    /** Last word of the place, e.g. "망원동" — for short lines. */
    val shortPlace: String? get() = placeLabel?.substringAfterLast(' ')
}

/** WMO weather interpretation codes (Open-Meteo) → Korean label + icon key. */
object WeatherCodes {
    const val CLEAR = "clear"
    const val PARTLY = "partly"
    const val CLOUDY = "cloudy"
    const val FOG = "fog"
    const val DRIZZLE = "drizzle"
    const val RAIN = "rain"
    const val SNOW = "snow"
    const val SHOWER = "shower"
    const val THUNDER = "thunder"

    fun icon(code: Int): String = when (code) {
        0 -> CLEAR
        1, 2 -> PARTLY
        3 -> CLOUDY
        45, 48 -> FOG
        in 51..57 -> DRIZZLE
        in 61..67 -> RAIN
        in 71..77, 85, 86 -> SNOW
        in 80..82 -> SHOWER
        in 95..99 -> THUNDER
        else -> CLOUDY
    }

    fun label(code: Int): String = when (icon(code)) {
        CLEAR -> "맑음"
        PARTLY -> "구름 조금"
        FOG -> "안개"
        DRIZZLE -> "이슬비"
        RAIN -> "비"
        SNOW -> "눈"
        SHOWER -> "소나기"
        THUNDER -> "뇌우"
        else -> "흐림"
    }

    fun isWet(code: Int) = icon(code) in setOf(DRIZZLE, RAIN, SNOW, SHOWER, THUNDER)
}

// --- Open-Meteo response ---

@Serializable internal data class Forecast(val current: Current, val daily: Daily)

@Serializable internal data class Current(
    @SerialName("temperature_2m") val temp: Double,
    @SerialName("apparent_temperature") val feelsLike: Double? = null,
    @SerialName("weather_code") val code: Int = 3,
    @SerialName("wind_speed_10m") val wind: Double? = null,
)

@Serializable internal data class Daily(
    @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipProb: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uv: List<Double?> = emptyList(),
)

private val json = Json { ignoreUnknownKeys = true }

internal fun parseForecast(body: String, place: String?, now: Long): TodayWeather {
    val f = json.decodeFromString<Forecast>(body)
    val c = f.current
    return TodayWeather(
        placeLabel = place,
        temp = c.temp,
        feelsLike = c.feelsLike ?: c.temp,
        min = f.daily.min.firstOrNull() ?: c.temp,
        max = f.daily.max.firstOrNull() ?: c.temp,
        precipProb = f.daily.precipProb.firstOrNull()?.toInt() ?: 0,
        condition = WeatherCodes.label(c.code),
        code = c.code,
        uv = f.daily.uv.firstOrNull(),
        wind = c.wind ?: 0.0,
        fetchedAtMillis = now,
    )
}

/** "마포구 망원동": the last two useful address parts (city/district + neighborhood). Road names are skipped. */
internal fun shortPlace(admin: String?, locality: String?, subLocality: String?, thoroughfare: String?): String? {
    val dong = thoroughfare?.takeIf { t -> listOf("동", "읍", "면", "리", "가").any { t.endsWith(it) } }
    val parts = listOfNotNull(locality, subLocality, dong).filter { it.isNotBlank() }.distinct().takeLast(2)
    return parts.joinToString(" ").ifBlank { admin?.takeIf { it.isNotBlank() } }
}
