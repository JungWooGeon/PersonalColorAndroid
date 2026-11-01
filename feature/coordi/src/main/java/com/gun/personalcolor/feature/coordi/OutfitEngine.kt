package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.SHOE_KINDS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.weather.TodayWeather
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** One piece of today's outfit, e.g. 아우터 · 카멜 트렌치코트. */
data class OutfitSlot(
    val label: String,
    val color: NamedColor,
    val item: String,
    val fromCloset: Boolean = false,
    /** CLOTHING_KINDS key for the illustration; null = no fitting kind (shown as a color chip). */
    val kind: String? = null,
    /** Closet cut-out photo under filesDir/closet/, when [fromCloset]. */
    val imageFile: String? = null,
)

/**
 * Keyword → CLOTHING_KINDS key, checked in order: specific names before generic ones
 * ("니트 조끼" before "니트", "셔츠 원피스" before "셔츠", "롱패딩" before "패딩", "후드티" before "티").
 */
private val KIND_KEYWORDS: List<Pair<List<String>, String>> = listOf(
    listOf("셔츠 원피스", "셔츠원피스") to "shirt_dress", listOf("니트 원피스") to "knit_dress", listOf("슬립 원피스", "슬립") to "slip_dress",
    listOf("점프수트", "점프슈트") to "jumpsuit", listOf("원피스", "드레스") to "dress",
    listOf("니트 조끼", "니트조끼", "스웨터 조끼") to "knit_vest", listOf("패딩 조끼", "조끼", "베스트") to "vest",
    listOf("롱패딩") to "long_padding", listOf("패딩", "다운") to "padding",
    listOf("후드 집업", "후드집업", "집업") to "zipup", listOf("바람막이", "윈드브레이커") to "windbreaker",
    listOf("청자켓", "데님 자켓", "데님 재킷") to "denim_jacket", listOf("가죽", "라이더") to "leather",
    listOf("트위드") to "tweed", listOf("블레이저") to "blazer", listOf("무스탕", "시어링") to "mustang", listOf("플리스", "뽀글이") to "fleece",
    listOf("트렌치") to "trench", listOf("코트") to "coat", listOf("자켓", "재킷") to "jacket",
    listOf("가디건") to "cardigan", listOf("후드") to "hoodie", listOf("맨투맨", "스웨트") to "sweatshirt",
    listOf("터틀넥", "목폴라") to "turtleneck", listOf("민소매", "나시", "슬리브리스") to "sleeveless",
    listOf("폴로") to "polo", listOf("블라우스") to "blouse", listOf("긴팔 티", "긴팔티", "롱슬리브") to "longtee",
    listOf("티셔츠") to "tee", listOf("셔츠", "남방") to "shirt", listOf("니트", "스웨터") to "knit", listOf("티") to "tee",
    listOf("슬랙스") to "slacks", listOf("와이드") to "wide", listOf("조거") to "jogger", listOf("카고") to "cargo",
    listOf("레깅스") to "leggings", listOf("데님", "청바지", "진 ") to "jeans", listOf("쇼츠", "반바지") to "shorts",
    listOf("미니스커트", "미니 스커트") to "mini_skirt", listOf("롱스커트", "롱 스커트", "맥시") to "long_skirt",
    listOf("플리츠") to "pleats", listOf("스커트", "치마") to "skirt", listOf("팬츠", "바지") to "pants",
    listOf("운동화", "러닝화") to "running", listOf("스니커즈") to "shoes", listOf("로퍼") to "loafers",
    listOf("힐", "펌프스") to "heels", listOf("플랫") to "flats", listOf("메리제인") to "mary_jane",
    listOf("앵클부츠", "앵클 부츠") to "ankle_boots", listOf("롱부츠", "롱 부츠") to "long_boots",
    listOf("방한 부츠", "어그", "방한화") to "winter_boots", listOf("레인부츠", "장화") to "rain_boots",
    listOf("샌들") to "sandals", listOf("슬리퍼", "뮬", "슬라이드") to "slippers", listOf("부츠") to "ankle_boots",
    listOf("구두") to "loafers", listOf("신발") to "shoes",
    listOf("머플러", "목도리") to "muffler", listOf("스카프") to "scarf", listOf("숄더백") to "shoulder_bag",
    listOf("크로스백") to "crossbody", listOf("백팩", "배낭") to "backpack", listOf("클러치") to "clutch",
    listOf("가방", "토트", "백") to "bag", listOf("비니") to "beanie", listOf("버킷햇", "버킷 햇") to "bucket_hat",
    listOf("베레모") to "beret", listOf("모자", "캡") to "hat", listOf("선글라스") to "sunglasses", listOf("벨트") to "belt",
    listOf("장갑") to "gloves", listOf("양말") to "socks", listOf("우산") to "umbrella", listOf("귀걸이", "이어링") to "earrings",
    listOf("목걸이") to "necklace", listOf("시계") to "watch", listOf("헤어", "집게핀", "머리끈", "곱창") to "hair_clip",
)

/** Maps an outfit item text ("카멜 트렌치코트", "셔츠 원피스", a closet name …) to a CLOTHING_KINDS key, falling back by slot label. */
internal fun kindOf(itemText: String, slot: String): String? {
    val t = "$itemText "
    return KIND_KEYWORDS.firstOrNull { (words, _) -> words.any { it in t } }?.second ?: when (slot) {
        "아우터" -> "jacket"
        "상의" -> "tee"
        "하의" -> "pants"
        "원피스" -> "dress"
        "신발" -> "shoes"
        else -> null
    }
}

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
    private class Band(
        val min: Int, val name: String, val outer: List<String>, val top: List<String>, val bottom: List<String>,
        val shoes: List<String>, val points: List<String>, val dresses: List<String>,
    )

    // Ordered warmest first; the first band whose [min] ≤ feels-like wins. Every item name maps to a kind via [kindOf].
    private val bands = listOf(
        Band(28, "민소매·린넨 날씨", emptyList(),
            listOf("민소매 탑", "린넨 반팔 셔츠", "반팔 티셔츠", "반팔 블라우스"),
            listOf("린넨 와이드 팬츠", "쇼츠", "플리츠 스커트", "미니스커트", "롱스커트"),
            listOf("샌들", "슬리퍼", "스니커즈", "플랫슈즈"),
            listOf("버킷햇", "볼캡", "크로스백", "귀걸이", "헤어 집게핀", "토트백"),
            listOf("슬립 원피스", "셔츠 원피스", "플레어 원피스", "점프수트")),
        Band(23, "반팔 날씨", emptyList(),
            listOf("반팔 티셔츠", "반팔 블라우스", "폴로 셔츠", "린넨 셔츠"),
            listOf("코튼 팬츠", "미디 스커트", "데님 팬츠", "와이드 팬츠", "쇼츠", "플리츠 스커트"),
            listOf("스니커즈", "샌들", "로퍼", "플랫슈즈", "뮬"),
            listOf("볼캡", "숄더백", "토트백", "귀걸이", "목걸이", "시계", "헤어 집게핀"),
            listOf("셔츠 원피스", "플레어 원피스", "점프수트")),
        Band(20, "얇은 긴팔·셔츠 날씨", emptyList(),
            listOf("긴팔 티셔츠", "셔츠", "블라우스", "얇은 니트"),
            listOf("슬랙스", "데님 팬츠", "롱스커트", "와이드 팬츠", "플리츠 스커트"),
            listOf("스니커즈", "로퍼", "플랫슈즈", "메리제인", "힐"),
            listOf("스카프", "토트백", "숄더백", "벨트", "시계", "목걸이"),
            listOf("셔츠 원피스", "플레어 원피스")),
        Band(17, "니트·가디건 날씨", emptyList(),
            listOf("니트", "가디건", "맨투맨", "후드티", "니트 조끼"),
            listOf("슬랙스", "데님 팬츠", "코듀로이 스커트", "조거 팬츠", "카고 팬츠", "롱스커트"),
            listOf("스니커즈", "로퍼", "메리제인", "운동화"),
            listOf("볼캡", "크로스백", "백팩", "토트백", "시계", "양말"),
            listOf("니트 원피스", "셔츠 원피스")),
        Band(12, "자켓·트렌치 날씨",
            listOf("트렌치코트", "자켓", "블레이저", "청자켓", "가죽 자켓", "후드 집업"),
            listOf("얇은 니트", "셔츠", "긴팔 티셔츠", "블라우스", "맨투맨"),
            listOf("슬랙스", "데님 팬츠", "미디 스커트", "와이드 팬츠", "카고 팬츠", "플리츠 스커트"),
            listOf("로퍼", "스니커즈", "앵클부츠", "메리제인", "힐", "운동화"),
            listOf("스카프", "숄더백", "토트백", "벨트", "베레모", "목걸이"),
            emptyList()),
        Band(9, "코트·니트 날씨",
            listOf("코트", "트위드 자켓", "가죽 자켓", "플리스", "패딩 조끼"),
            listOf("니트", "터틀넥", "맨투맨", "후드티"),
            listOf("울 슬랙스", "데님 팬츠", "롱스커트", "와이드 팬츠"),
            listOf("앵클부츠", "로퍼", "롱부츠", "스니커즈"),
            listOf("머플러", "베레모", "숄더백", "토트백", "벨트"),
            emptyList()),
        Band(5, "울코트·두꺼운 니트 날씨",
            listOf("울코트", "무스탕", "숏패딩"),
            listOf("두꺼운 니트", "터틀넥 니트", "후드티"),
            listOf("기모 슬랙스", "울 스커트", "기모 조거 팬츠", "데님 팬츠"),
            listOf("롱부츠", "앵클부츠", "로퍼", "방한 부츠"),
            listOf("머플러", "비니", "베레모", "장갑", "숄더백"),
            emptyList()),
        Band(Int.MIN_VALUE, "패딩·목도리 날씨",
            listOf("롱패딩", "숏패딩"),
            listOf("두꺼운 니트", "터틀넥", "후드티"),
            listOf("기모 팬츠", "울 슬랙스", "기모 조거 팬츠"),
            listOf("방한 부츠", "롱부츠", "앵클부츠"),
            listOf("머플러", "비니", "장갑", "백팩"),
            emptyList()),
    )

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
        val shoeColor = neutrals[rnd.nextInt(neutrals.size)]
        val bright = mains.maxBy { lab(it.hex).l + lab(it.hex).chroma / 2 }
        val mood = closestMood(moods, tone, variant) ?: main

        val mine = closet.filter { it.toneId == tone.id && it.verdict != Verdict.AVOID }
        fun ofCategory(category: String) = mine.filter {
            val shoe = it.category == "신발" || it.kind in SHOE_KINDS   // older shoe items were saved under 액세서리
            if (category == "신발") shoe else it.category == category && !shoe
        }
        fun pick(list: List<String>) = list[rnd.nextInt(list.size)]
        fun slot(label: String, category: String, color: NamedColor, noun: String): OutfitSlot {
            val owned = ofCategory(category)
            if (owned.isEmpty()) return OutfitSlot(label, color, "${color.name} $noun", kind = kindOf(noun, label))
            val item = owned[rnd.nextInt(owned.size)]
            return OutfitSlot(label, NamedColor(item.name, item.hex), "내 옷장의 '${item.name}'", fromCloset = true,
                kind = item.kind ?: kindOf(item.name, label), imageFile = item.imageFile)
        }

        val slots = buildList {
            val outerNoun = when {
                rainy && feels >= 12 -> "방수 바람막이"
                // Only outerwear that comes in a waterproof version (no 방수 무스탕/트위드/가죽).
                rainy && band.outer.isNotEmpty() -> "방수 " + pick(band.outer.filter { o -> listOf("코트", "패딩", "자켓").any { it in o } && listOf("트위드", "가죽").none { it in o } }.ifEmpty { band.outer })
                band.outer.isNotEmpty() -> pick(band.outer)
                else -> null
            }
            if (outerNoun != null) add(slot("아우터", "아우터", outerColor, outerNoun))
            if (band.dresses.isNotEmpty() && rnd.nextInt(4) == 0) {
                add(slot("원피스", "원피스", main, pick(band.dresses)))
            } else {
                add(slot("상의", "상의", main, pick(band.top)))
                add(slot("하의", "하의", bottomColor, pick(band.bottom)))
            }
            add(slot("신발", "신발", if (rainy) bright else shoeColor, if (rainy && feels < 28) "레인부츠" else pick(band.shoes)))
            if (rainy) add(OutfitSlot("포인트", bright, "${bright.name} 우산", kind = "umbrella"))
            else add(slot("포인트", "액세서리", mood, pick(band.points)))
        }

        val closetLip = mine.filter { it.category == "립·메이크업" }.let { if (it.isEmpty()) null else it[rnd.nextInt(it.size)] }
        val m = tone.makeup
        val extras = buildList {
            if (rainy) add("우산")
            if (strongUv) { add("모자"); add("선글라스") }
            if (band.min == Int.MIN_VALUE) add("목도리")
        }
        val tip = when {
            rainy -> "비 소식이 있어요(강수확률 ${weather!!.precipProb}%). 우산 챙기고, 밝은 포인트 컬러로 기분 전환해요."
            strongUv -> "자외선이 강해요. 모자나 선글라스를 챙기세요."
            weather != null && weather.max - weather.min >= 10 -> "일교차가 커요. 벗고 입기 쉬운 겉옷을 챙기세요."
            else -> "이달의 무드 컬러 '${mood.name}', 소품으로 살짝 더해 봐요."
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
            headline = slots.filter { it.label != "포인트" && it.label != "신발" }.take(2).joinToString(" + ", transform = shortName),
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
