package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.weather.TodayWeather
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** One piece of today's outfit, e.g. 아우터 · 카멜 트렌치코트. */
data class OutfitSlot(val label: String, val color: NamedColor, val item: String, val fromCloset: Boolean = false)

data class Outfit(
    /** "체감 14°C · 자켓·트렌치 날씨" or the season-based fallback line. */
    val layerLine: String,
    val slots: List<OutfitSlot>,
    val lip: NamedColor,
    val lipFromCloset: Boolean,
    val eye: NamedColor,
    val blush: NamedColor,
    val metal: String,
    /** This month's mood color closest to the user's palette. */
    val moodColor: NamedColor,
    /** Things to bring: 우산, 선글라스, 목도리 … */
    val extras: List<String>,
    val tip: String,
    /** "카멜 트렌치코트 + 아이보리 니트" */
    val headline: String,
)

/**
 * Builds today's outfit from the tone palette, weather (or season defaults), this month's mood colors and the closet.
 * Deterministic: same date + tone + variant → same outfit.
 */
object OutfitEngine {
    private class Band(val min: Int, val name: String, val outer: List<String>, val top: List<String>, val bottom: List<String>)

    // Ordered warmest first; the first band whose [min] ≤ feels-like wins.
    private val bands = listOf(
        Band(28, "민소매·린넨 날씨", emptyList(), listOf("민소매 탑", "린넨 반팔 셔츠"), listOf("린넨 와이드 팬츠", "쇼츠", "플리츠 스커트")),
        Band(23, "반팔 날씨", emptyList(), listOf("반팔 티셔츠", "반팔 블라우스", "폴로 셔츠"), listOf("코튼 팬츠", "미디 스커트", "데님 팬츠")),
        Band(20, "얇은 긴팔·셔츠 날씨", emptyList(), listOf("얇은 긴팔 티", "셔츠", "블라우스"), listOf("슬랙스", "데님 팬츠", "롱스커트")),
        Band(17, "니트·가디건 날씨", emptyList(), listOf("니트", "가디건", "맨투맨"), listOf("슬랙스", "데님 팬츠", "코듀로이 스커트")),
        Band(12, "자켓·트렌치 날씨", listOf("트렌치코트", "자켓", "블레이저"), listOf("얇은 니트", "셔츠"), listOf("슬랙스", "데님 팬츠", "미디 스커트")),
        Band(9, "코트·니트 날씨", listOf("코트", "트위드 자켓"), listOf("니트", "터틀넥"), listOf("울 슬랙스", "데님 팬츠", "롱스커트")),
        Band(5, "울코트·두꺼운 니트 날씨", listOf("울코트", "무스탕"), listOf("두꺼운 니트", "터틀넥 니트"), listOf("기모 슬랙스", "울 스커트")),
        Band(Int.MIN_VALUE, "패딩·목도리 날씨", listOf("롱패딩", "숏패딩"), listOf("두꺼운 니트", "터틀넥"), listOf("기모 팬츠", "울 슬랙스")),
    )
    private val dresses = listOf("셔츠 원피스", "니트 원피스", "플레어 원피스")
    private val points = listOf("스카프", "가방", "헤어 액세서리", "양말")

    /** Typical feels-like °C per month (Korea) when there's no weather. */
    private val monthlyFeels = intArrayOf(-2, 1, 8, 14, 19, 24, 28, 29, 24, 16, 8, 1)

    fun build(
        tone: Tone,
        weather: TodayWeather?,
        date: LocalDate,
        moods: List<NamedColor>,
        closet: List<ClosetItem> = emptyList(),
        variant: Int = 0,
    ): Outfit {
        val rnd = Random(date.toEpochDay() * 31 + tone.id.hashCode() + variant * 7919L)
        val feels = weather?.feelsLike?.roundToInt() ?: monthlyFeels[date.monthValue - 1]
        val band = bands.first { feels >= it.min }
        val rainy = weather != null && weather.precipProb >= 50
        val strongUv = (weather?.uv ?: 0.0) >= 6.0

        // Colors: neutrals (lowest chroma) as base, a seeded main, the brightest for rainy-day points.
        val byChroma = tone.bestColors.sortedBy { lab(it.hex).chroma }
        val neutrals = byChroma.take(3)
        val mains = byChroma.drop(3).ifEmpty { byChroma }
        val main = mains[rnd.nextInt(mains.size)]
        val outerColor = neutrals[rnd.nextInt(neutrals.size)]
        val bottomColor = (neutrals - outerColor).maxByOrNull { kotlin.math.abs(lab(it.hex).l - lab(main.hex).l) } ?: outerColor
        val bright = mains.maxBy { lab(it.hex).l + lab(it.hex).chroma / 2 }
        val mood = closestMood(moods, tone, variant) ?: main

        val mine = closet.filter { it.toneId == tone.id && it.verdict != Verdict.AVOID }
        fun slot(label: String, category: String, color: NamedColor, noun: String): OutfitSlot {
            val owned = mine.filter { it.category == category }
            if (owned.isEmpty()) return OutfitSlot(label, color, "${color.name} $noun")
            val item = owned[rnd.nextInt(owned.size)]
            return OutfitSlot(label, NamedColor(item.name, item.hex), "내 옷장의 '${item.name}'", fromCloset = true)
        }

        val slots = buildList {
            val outerNoun = when {
                rainy && feels >= 12 -> "방수 바람막이"
                rainy && band.outer.isNotEmpty() -> "방수 " + band.outer[rnd.nextInt(band.outer.size)]
                band.outer.isNotEmpty() -> band.outer[rnd.nextInt(band.outer.size)]
                else -> null
            }
            if (outerNoun != null) add(slot("아우터", "아우터", outerColor, outerNoun))
            if (feels >= 17 && rnd.nextInt(4) == 0) {
                add(slot("원피스", "원피스", main, dresses[rnd.nextInt(dresses.size)]))
            } else {
                add(slot("상의", "상의", main, band.top[rnd.nextInt(band.top.size)]))
                add(slot("하의", "하의", bottomColor, band.bottom[rnd.nextInt(band.bottom.size)]))
            }
            if (rainy) add(OutfitSlot("포인트", bright, "${bright.name} 우산"))
            else add(slot("포인트", "액세서리", mood, points[rnd.nextInt(points.size)]))
        }

        val closetLip = mine.filter { it.category == "립·메이크업" }.let { if (it.isEmpty()) null else it[rnd.nextInt(it.size)] }
        val m = tone.makeup
        val extras = buildList {
            if (rainy) add("우산")
            if (strongUv) { add("모자"); add("선글라스") }
            if (band.min == Int.MIN_VALUE) add("목도리")
        }
        val tip = when {
            rainy -> "비 소식이 있어요(강수확률 ${weather!!.precipProb}%). 우산을 챙기고 밝은 톤 포인트로 기분을 올려 보세요."
            strongUv -> "자외선이 강해요. 모자나 선글라스로 햇빛을 가려 주세요."
            weather != null && weather.max - weather.min >= 10 -> "일교차가 커요. 벗고 입기 쉬운 겉옷을 챙기세요."
            else -> "이번 달 무드 컬러 '${mood.name}'을(를) 작은 소품으로 더해 보세요."
        }
        val shortName = { s: OutfitSlot -> if (s.fromCloset) s.color.name else s.item }
        return Outfit(
            layerLine = if (weather != null) "체감 ${feels}°C · ${band.name}" else "${date.monthValue}월 평균 기온 기준 · ${band.name}",
            slots = slots,
            lip = closetLip?.let { NamedColor(it.name, it.hex) } ?: m.lipColors.pick(rnd, main),
            lipFromCloset = closetLip != null,
            eye = m.eyeColors.pick(rnd, neutrals.first()),
            blush = m.blushColors.pick(rnd, main),
            metal = tone.accessory.metal,
            moodColor = mood,
            extras = extras,
            tip = tip,
            headline = slots.filter { it.label != "포인트" }.take(2).joinToString(" + ", transform = shortName),
        )
    }

    /** Mood color nearest to any best color, pushed away from worst colors. [variant] rotates through the top 3. */
    internal fun closestMood(moods: List<NamedColor>, tone: Tone, variant: Int = 0): NamedColor? {
        if (moods.isEmpty()) return null
        val best = tone.bestColors.map { lab(it.hex) }
        val worst = tone.worstColors.map { lab(it.hex) }
        val ranked = moods.sortedBy { mood ->
            val l = lab(mood.hex)
            val toBest = best.minOfOrNull { deltaE(l, it) } ?: 0.0
            val toWorst = worst.minOfOrNull { deltaE(l, it) } ?: Double.MAX_VALUE
            toBest + if (toWorst < toBest) 25.0 else 0.0
        }
        return ranked[variant % minOf(3, ranked.size)]
    }

    private fun List<NamedColor>.pick(rnd: Random, fallback: NamedColor) = if (isEmpty()) fallback else this[rnd.nextInt(size)]

    // ponytail: CIE76 distance — fine for picking among a handful of colors; swap in CIEDE2000 if ranking looks off.
    internal fun deltaE(a: Lab, b: Lab) = sqrt((a.l - b.l).let { it * it } + (a.a - b.a).let { it * it } + (a.b - b.b).let { it * it })

    internal fun lab(hex: String): Lab {
        val v = hex.removePrefix("#").toInt(16)
        return ColorMath.rgbToLab(Rgb(v shr 16 and 0xFF, v shr 8 and 0xFF, v and 0xFF))
    }
}
