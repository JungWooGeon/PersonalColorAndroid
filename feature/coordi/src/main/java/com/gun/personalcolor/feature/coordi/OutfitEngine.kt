package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.model.BAG_KINDS
import com.gun.personalcolor.core.model.CLOTHING_KINDS
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
 * ("니트 베스트" before "니트", "셔츠 원피스" before "셔츠", "롱패딩" before "패딩", "부츠컷" before "부츠", "뷔스티에" before "티").
 */
private val KIND_KEYWORDS: List<Pair<List<String>, String>> = listOf(
    listOf("셔츠 원피스", "셔츠원피스") to "shirt_dress", listOf("니트 원피스") to "knit_dress", listOf("슬립 원피스", "슬립") to "slip_dress",
    listOf("미니 원피스", "미니원피스") to "mini_dress", listOf("점프수트", "점프슈트") to "jumpsuit", listOf("멜빵", "오버롤") to "overall",
    listOf("원피스", "드레스") to "dress",
    listOf("니트 조끼", "니트조끼", "니트 베스트", "스웨터 조끼") to "knit_vest", listOf("패딩 조끼", "패딩 베스트", "조끼", "베스트") to "vest",
    listOf("롱패딩") to "long_padding", listOf("패딩", "다운") to "padding",
    listOf("후드 집업", "후드집업", "집업") to "zipup", listOf("바람막이", "윈드브레이커", "아노락") to "windbreaker",
    listOf("청자켓", "데님 자켓", "데님 재킷") to "denim_jacket", listOf("가죽", "라이더", "레더") to "leather",
    listOf("트위드", "부클") to "tweed", listOf("블레이저") to "blazer", listOf("무스탕", "시어링") to "mustang", listOf("플리스", "뽀글이") to "fleece",
    listOf("볼레로") to "bolero", listOf("트렌치") to "trench", listOf("하프코트", "하프 코트", "숏코트") to "short_coat",
    listOf("떡볶이", "더플") to "duffle", listOf("코트") to "coat", listOf("자켓", "재킷") to "jacket",
    listOf("가디건") to "cardigan", listOf("후드") to "hoodie", listOf("스웨트 팬츠", "트레이닝 팬츠", "조거") to "jogger",
    listOf("맨투맨", "스웨트") to "sweatshirt", listOf("터틀넥", "목폴라") to "turtleneck", listOf("민소매", "나시", "슬리브리스") to "sleeveless",
    listOf("뷔스티에", "뷔스티") to "bustier", listOf("오프숄더", "오프 숄더") to "off_shoulder", listOf("카라 니트", "폴로") to "polo",
    listOf("크롭 티", "크롭티", "베이비 티") to "crop_tee", listOf("골지") to "ribbed_top",
    listOf("블라우스") to "blouse", listOf("긴팔 티", "긴팔티", "롱슬리브") to "longtee",
    listOf("티셔츠") to "tee", listOf("셔츠", "남방") to "shirt", listOf("니트", "스웨터") to "knit", listOf("티") to "tee",
    listOf("슬랙스") to "slacks", listOf("와이드") to "wide", listOf("카고") to "cargo", listOf("부츠컷", "플레어 팬츠") to "bootcut",
    listOf("레깅스") to "leggings", listOf("데님 스커트", "청치마", "청 스커트") to "denim_skirt",
    listOf("데님", "청바지", "진 ") to "jeans", listOf("버뮤다") to "bermuda", listOf("쇼츠", "반바지") to "shorts",
    listOf("미니스커트", "미니 스커트") to "mini_skirt", listOf("롱스커트", "롱 스커트", "맥시") to "long_skirt",
    listOf("플리츠") to "pleats", listOf("스커트", "치마") to "skirt", listOf("팬츠", "바지") to "pants",
    listOf("레트로 스니커즈", "운동화", "러닝화") to "running", listOf("스니커즈") to "shoes", listOf("통굽", "플랫폼") to "platform",
    listOf("로퍼") to "loafers", listOf("힐", "펌프스") to "heels", listOf("플랫") to "flats", listOf("메리제인") to "mary_jane",
    listOf("앵클부츠", "앵클 부츠") to "ankle_boots", listOf("롱부츠", "롱 부츠") to "long_boots", listOf("웨스턴") to "western",
    listOf("방한 부츠", "어그", "방한화") to "winter_boots", listOf("레인부츠", "장화") to "rain_boots",
    listOf("샌들") to "sandals", listOf("슬리퍼", "뮬", "슬라이드") to "slippers", listOf("부츠") to "ankle_boots",
    listOf("구두") to "loafers", listOf("신발") to "shoes",
    listOf("레그워머") to "leg_warmers", listOf("머플러", "목도리") to "muffler", listOf("스카프") to "scarf", listOf("숄더백") to "shoulder_bag",
    listOf("호보") to "hobo", listOf("미니백", "미니 백") to "mini_bag",
    listOf("크로스백") to "crossbody", listOf("백팩", "배낭") to "backpack", listOf("클러치") to "clutch",
    listOf("가방", "토트", "백") to "bag", listOf("비니") to "beanie", listOf("버킷햇", "버킷 햇") to "bucket_hat",
    listOf("베레모") to "beret", listOf("모자", "캡") to "hat", listOf("선글라스") to "sunglasses", listOf("벨트") to "belt",
    listOf("장갑") to "gloves", listOf("양말") to "socks", listOf("우산") to "umbrella", listOf("귀걸이", "이어링") to "earrings",
    listOf("목걸이") to "necklace", listOf("시계") to "watch", listOf("리본") to "ribbon", listOf("헤어", "집게핀", "머리끈", "곱창") to "hair_clip",
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
        // Unknown accessories (a closet "진주 반지"…) still need a picture on the board: a small bag reads as "an accessory".
        "포인트", "액세서리" -> "crossbody"
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
    /** "아이보리 니트 + 중청 데님 스커트" */
    val headline: String,
)

/**
 * Builds today's outfit from the tone palette, weather (or season defaults), this month's mood colors and the closet.
 * Deterministic: same date + tone + variant → same outfit.
 */
object OutfitEngine {
    /*
     * Styling rules (target: women in their late teens to mid-20s, Korea). OutfitEngineTest sweeps every tone × weather × variant.
     * 1. Color: a neutral base that suits the tone (warm: ivory/beige/camel/brown, cool: black/white/grey/navy, denim for both)
     *    plus ONE accent from the tone palette, on one piece: the top (usually), a soft outer or dress, the bag, the muffler /
     *    beanie or the umbrella. Big pieces only take the softer half of the accents. Shoes are always neutral; the bag is
     *    neutral unless it is the accent. Never all dark: a big piece is light (L* ≥ 50) unless the accent is on the top.
     *    Coats are never pure white (ivory instead).
     * 2. Items: what the target actually wears per temperature band. No polo / jogger / windbreaker defaults.
     *    Skirts and dresses get their own shoes (long or ankle boots in the cold, loafers / mary janes / flats when mild).
     * 3. Rain: black or ivory rain boots, or black loafers; the umbrella is black / ivory or the accent, never the shoe color.
     *    "방수" only on the windbreaker.
     * 4. Slots come in wearing order: 아우터 → 상의 → 하의 (or 원피스) → 신발 → 가방 → 소품 (umbrella on rainy days, else a
     *    muffler or beanie when it's cold).
     */
    internal class Neutrals(val base: List<NamedColor>, val leather: List<NamedColor>, val denim: List<NamedColor>) {
        /** Rain boots: black or the lightest leather neutral (ivory / white), never a palette color. */
        val rainBoots get() = listOf(BLACK, light)
        val light get() = leather.maxBy { lab(it.hex).l }
        val all get() = base + leather + denim + BLACK + COAT_IVORY
    }

    private fun nc(name: String, hex: String) = NamedColor(name, hex)
    private val BLACK = nc("블랙", "#1E1E20")
    private val COAT_IVORY = nc("아이보리", "#F2EEE5")
    private val lightDenim = nc("연청", "#A7BCD6")
    private val midDenim = nc("중청", "#6F88AB")
    private val darkDenim = nc("진청", "#3C5276")

    private val seasonNeutrals = mapOf(
        "spring" to Neutrals(
            listOf(nc("아이보리", "#F6EEDC"), nc("크림 베이지", "#EBD9BC"), nc("카멜", "#C49A6C"), nc("밀크 브라운", "#9A6E4C"), nc("오트밀", "#E2D5BF")),
            listOf(nc("아이보리", "#F3EBDD"), nc("탄", "#C29265"), nc("브라운", "#7A5236")),
            listOf(lightDenim, midDenim)),
        "autumn" to Neutrals(
            listOf(nc("아이보리", "#F1E6CF"), nc("베이지", "#D8C2A0"), nc("카멜", "#B5844F"), nc("브라운", "#6E4A30"), nc("다크 브라운", "#42301F")),
            listOf(nc("아이보리", "#EFE5D2"), nc("탄", "#B07D4F"), nc("브라운", "#6B4630"), nc("다크 브라운", "#3E2C1F")),
            listOf(midDenim, darkDenim)),
        "summer" to Neutrals(
            listOf(nc("화이트", "#F7F7F5"), nc("라이트 그레이", "#D6D8DC"), nc("그레이", "#9B9FA7"), nc("네이비", "#2F3B5C"), nc("차콜", "#4A4D55")),
            listOf(nc("화이트", "#F4F4F2"), nc("그레이", "#9A9DA4"), nc("블랙", "#26272B")),
            listOf(lightDenim, midDenim)),
        "winter" to Neutrals(
            listOf(nc("블랙", "#1C1C1E"), nc("오프 화이트", "#F4F3EF"), nc("차콜", "#3A3C42"), nc("그레이", "#A3A6AD"), nc("네이비", "#1C2541")),
            listOf(nc("블랙", "#1C1C1E"), nc("오프 화이트", "#F4F3EF"), nc("그레이", "#9FA2A8")),
            listOf(darkDenim, nc("흑청", "#2A2C33"))),
    )

    internal fun neutralsOf(tone: Tone): Neutrals = seasonNeutrals[tone.season] ?: seasonNeutrals.getValue(if (tone.isWarm) "autumn" else "summer")

    /** Palette colors usable as the accent: clearly colored and not a near-copy of a neutral, softest first. */
    internal fun accentsOf(tone: Tone): List<NamedColor> {
        val neutral = neutralsOf(tone).all.map { lab(it.hex) }
        val byChroma = tone.bestColors.sortedBy { lab(it.hex).chroma }
        // No palette colors at all (bad content): fall back to the neutrals so callers never pick from an empty list.
        return byChroma.filter { c -> lab(c.hex).let { l -> l.chroma >= 15 && neutral.none { deltaE(it, l) < 12 } } }
            .ifEmpty { byChroma }.ifEmpty { neutralsOf(tone).base }
    }

    private val SKIRTS = setOf("skirt", "mini_skirt", "pleats", "long_skirt", "denim_skirt")
    private val DENIM = setOf("jeans", "bootcut", "denim_skirt", "denim_jacket", "shorts", "overall")
    private val NOT_FOR_RAIN = setOf("tweed", "mustang", "fleece")

    private class Band(
        val min: Int, val name: String, val outer: List<String>, val top: List<String>, val bottom: List<String>,
        val dresses: List<String>, val shoes: List<String>, val skirtShoes: List<String>, val bags: List<String>,
    )

    // Ordered warmest first; the first band whose [min] ≤ feels-like wins. Every entry is a CLOTHING_KINDS key.
    private val bands = listOf(
        Band(28, "민소매·린넨 날씨", emptyList(),
            listOf("crop_tee", "sleeveless", "blouse", "tee", "off_shoulder"),
            listOf("denim_skirt", "shorts", "mini_skirt", "wide", "long_skirt", "bermuda"),
            listOf("slip_dress", "mini_dress", "dress", "shirt_dress"),
            listOf("sandals", "slippers", "flats", "shoes"), listOf("sandals", "flats", "slippers"),
            listOf("shoulder_bag", "mini_bag", "crossbody", "bag")),
        Band(23, "반팔 날씨", emptyList(),
            listOf("tee", "crop_tee", "blouse", "sleeveless", "shirt"),
            listOf("denim_skirt", "jeans", "wide", "mini_skirt", "skirt", "pleats", "shorts"),
            listOf("dress", "mini_dress", "shirt_dress", "slip_dress"),
            listOf("shoes", "sandals", "loafers", "flats"), listOf("sandals", "flats", "mary_jane", "loafers"),
            listOf("shoulder_bag", "mini_bag", "crossbody", "bag")),
        Band(20, "얇은 긴팔·셔츠 날씨", emptyList(),
            listOf("blouse", "shirt", "longtee", "ribbed_top"),
            listOf("jeans", "slacks", "pleats", "mini_skirt", "long_skirt", "denim_skirt", "skirt"),
            listOf("shirt_dress", "dress", "mini_dress"),
            listOf("loafers", "shoes", "running", "platform"), listOf("loafers", "mary_jane", "flats"),
            listOf("shoulder_bag", "mini_bag", "bag", "crossbody")),
        Band(17, "가디건·니트 날씨", emptyList(),
            listOf("cardigan", "knit", "blouse", "shirt", "knit_vest", "sweatshirt"),
            listOf("jeans", "wide", "long_skirt", "pleats", "slacks", "denim_skirt", "bootcut"),
            listOf("knit_dress", "shirt_dress", "dress"),
            listOf("loafers", "platform", "shoes", "running"), listOf("loafers", "mary_jane", "platform"),
            listOf("shoulder_bag", "hobo", "mini_bag", "bag")),
        Band(12, "자켓·트렌치 날씨",
            listOf("trench", "tweed", "blazer", "jacket", "denim_jacket", "leather"),
            listOf("blouse", "shirt", "knit", "ribbed_top", "longtee"),
            listOf("mini_skirt", "pleats", "jeans", "slacks", "long_skirt", "denim_skirt", "bootcut"),
            listOf("dress", "shirt_dress", "mini_dress"),
            listOf("loafers", "ankle_boots", "platform", "shoes"), listOf("loafers", "mary_jane", "ankle_boots", "long_boots"),
            listOf("shoulder_bag", "mini_bag", "hobo", "bag")),
        Band(9, "코트·니트 날씨",
            listOf("short_coat", "coat", "leather", "duffle", "tweed"),
            listOf("knit", "turtleneck", "cardigan", "ribbed_top"),
            listOf("jeans", "long_skirt", "wide", "slacks", "bootcut", "mini_skirt", "pleats"),
            listOf("knit_dress"),
            listOf("ankle_boots", "loafers", "platform", "long_boots"), listOf("long_boots", "ankle_boots"),
            listOf("shoulder_bag", "hobo", "bag", "mini_bag")),
        Band(5, "울코트·두꺼운 니트 날씨",
            listOf("coat", "short_coat", "duffle", "mustang", "padding"),
            listOf("knit", "turtleneck", "cardigan"),
            listOf("jeans", "long_skirt", "wide", "slacks", "bootcut", "pleats"),
            listOf("knit_dress"),
            listOf("ankle_boots", "long_boots", "loafers", "platform"), listOf("long_boots", "ankle_boots"),
            listOf("shoulder_bag", "hobo", "bag")),
        Band(Int.MIN_VALUE, "패딩·머플러 날씨",
            listOf("long_padding", "coat", "padding", "mustang", "duffle"),
            listOf("knit", "turtleneck"),
            listOf("jeans", "wide", "long_skirt", "slacks", "bootcut"),
            listOf("knit_dress"),
            listOf("winter_boots", "long_boots", "ankle_boots"), listOf("long_boots", "winter_boots"),
            listOf("shoulder_bag", "hobo", "bag")),
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
        val feels = weather?.feelsLike?.takeIf { it.isFinite() }?.roundToInt() ?: monthlyFeels[date.monthValue - 1]
        val band = bands.first { feels >= it.min }
        val rainy = weather != null && weather.precipProb >= 50
        val strongUv = (weather?.uv ?: 0.0) >= 6.0
        fun pick(list: List<String>) = list[rnd.nextInt(list.size)]
        fun <T> pickOf(list: List<T>) = list[rnd.nextInt(list.size)]

        // Pieces (kinds) first, then colors.
        val outerKind = when {
            band.outer.isEmpty() -> null
            rainy -> pick(band.outer.filter { it !in NOT_FOR_RAIN } + if (band.min == 12) listOf("windbreaker") else emptyList())
            else -> pick(band.outer)
        }
        val dressKind = if (band.dresses.isNotEmpty() && rnd.nextInt(4) == 0) pick(band.dresses) else null
        val topKind = if (dressKind == null) pick(band.top) else null
        val bottomKind = if (dressKind == null) pick(band.bottom) else null
        val skirtLike = dressKind != null || bottomKind in SKIRTS
        val shoeKind = when {
            rainy -> pick(listOf("rain_boots", "rain_boots", "loafers"))
            skirtLike -> pick(band.skirtShoes)
            else -> pick(band.shoes)
        }
        val bagKind = pick(band.bags)
        val accKind = when {
            rainy -> "umbrella"
            feels < 3 -> pick(listOf("muffler", "beanie"))
            feels < 9 -> if (rnd.nextInt(4) == 0) "beanie" else "muffler"
            else -> null
        }

        // Colors: one accent, everything else neutral.
        val neutrals = neutralsOf(tone)
        val accents = accentsOf(tone)
        val accentIndex = rnd.nextInt(accents.size)
        val accent = accents[accentIndex]
        val soft = accentIndex < (accents.size + 1) / 2
        val mainLabel = if (dressKind != null) "원피스" else "상의"
        val r = rnd.nextInt(6)
        val accentOn = when {
            r == 0 && outerKind != null && soft -> "아우터"
            r == 1 -> "가방"
            r == 2 && accKind != null -> "소품"
            mainLabel == "원피스" && !soft -> "가방"
            else -> mainLabel
        }
        val used = mutableListOf<NamedColor>()
        val big = linkedMapOf<String, NamedColor>()
        fun neutralFor(label: String, kind: String): NamedColor {
            var from = if (kind in DENIM) neutrals.denim + neutrals.base.filter { lab(it.hex).l > 90 || lab(it.hex).l < 25 } else neutrals.base
            if (label == "아우터") from = from.map { if (lab(it.hex).l > 96) COAT_IVORY else it }
            // Rain: dark, mid or denim bottoms and dresses (no white / ivory). At most one white or off-white big piece.
            if (rainy && (label == "하의" || label == "원피스")) from = from.filter { lab(it.hex).l < 75 || it in neutrals.denim }
            if (big.values.any { lab(it.hex).l > 90 }) from = from.filter { lab(it.hex).l <= 90 }.ifEmpty { from }
            return pickOf(from.filter { it !in used }.ifEmpty { from }).also { used += it }
        }
        if (outerKind != null) big["아우터"] = if (accentOn == "아우터") accent else neutralFor("아우터", outerKind)
        if (dressKind != null) big["원피스"] = if (accentOn == "원피스") accent else neutralFor("원피스", dressKind)
        if (topKind != null) big["상의"] = if (accentOn == "상의") accent else neutralFor("상의", topKind)
        if (bottomKind != null) big["하의"] = if (accentOn == "하의") accent else neutralFor("하의", bottomKind)
        // Never all dark: lighten the first neutral inner piece (top, bottom, dress, then outer).
        val darkLabel = listOf("상의", "하의", "원피스", "아우터").firstOrNull { it in big && it != accentOn }
        if (accentOn != mainLabel && darkLabel != null && big.values.none { lab(it.hex).l >= 50 }) {
            val label = darkLabel
            val kind = mapOf("상의" to topKind, "하의" to bottomKind, "원피스" to dressKind, "아우터" to outerKind).getValue(label)!!
            val light = (if (kind in DENIM) neutrals.denim + neutrals.base else neutrals.base)
                .filter { !rainy || (label != "하의" && label != "원피스") || lab(it.hex).l < 75 }.maxBy { lab(it.hex).l }
            big[label] = if (label == "아우터" && lab(light.hex).l > 96) COAT_IVORY else light
        }
        val shoeColor = when {
            shoeKind == "rain_boots" -> pickOf(neutrals.rainBoots)
            rainy -> BLACK
            // Long boots and uggs never in white / ivory (they show dirt).
            shoeKind == "long_boots" || shoeKind == "winter_boots" -> pickOf(neutrals.leather.filter { lab(it.hex).l <= 85 })
            else -> pickOf(neutrals.leather)
        }
        val bagColor = if (accentOn == "가방") accent else pickOf(neutrals.leather)
        val accColor = when {
            accentOn == "소품" -> accent
            accKind == "umbrella" -> if (shoeColor == BLACK) neutrals.light else BLACK
            else -> pickOf(neutrals.base.filter { it !in big.values }.ifEmpty { neutrals.base })
        }
        val mood = closestMood(moods, tone, variant) ?: accent

        val mine = closet.filter { it.toneId == tone.id && it.verdict != Verdict.AVOID }
        fun ofCategory(category: String) = mine.filter {
            val shoe = it.category == "신발" || it.kind in SHOE_KINDS   // older shoe items were saved under 액세서리
            when (category) {
                "신발" -> shoe
                "가방" -> it.category == "액세서리" && (it.kind ?: kindOf(it.name, "")) in BAG_KINDS
                else -> it.category == category && !shoe
            }
        }
        fun slot(label: String, category: String, color: NamedColor, kind: String): OutfitSlot {
            val owned = ofCategory(category)
            if (owned.isEmpty()) {
                val noun = if (kind == "windbreaker") "방수 바람막이" else CLOTHING_KINDS.getValue(kind).first
                return OutfitSlot(label, color, "${color.name} $noun", kind = kind)
            }
            val item = owned[rnd.nextInt(owned.size)]
            return OutfitSlot(label, NamedColor(item.name, item.hex), "내 옷장의 '${item.name}'", fromCloset = true,
                kind = item.kind ?: kindOf(item.name, label), imageFile = item.imageFile)
        }

        // Wearing order: outer → top → bottom (or dress) → shoes → bag → umbrella / muffler / beanie.
        val slots = buildList {
            if (outerKind != null) add(slot("아우터", "아우터", big.getValue("아우터"), outerKind))
            if (dressKind != null) add(slot("원피스", "원피스", big.getValue("원피스"), dressKind))
            if (topKind != null) add(slot("상의", "상의", big.getValue("상의"), topKind))
            if (bottomKind != null) add(slot("하의", "하의", big.getValue("하의"), bottomKind))
            add(slot("신발", "신발", shoeColor, shoeKind))
            add(slot("가방", "가방", bagColor, bagKind))
            if (accKind != null) add(OutfitSlot("소품", accColor, "${accColor.name} ${CLOTHING_KINDS.getValue(accKind).first}", kind = accKind))
        }

        val closetLip = mine.filter { it.category == "립·메이크업" }.let { if (it.isEmpty()) null else it[rnd.nextInt(it.size)] }
        val m = tone.makeup
        val extras = buildList {
            if (rainy) add("우산")
            if (feels < 9 && accKind != "muffler") add("머플러")
            if (strongUv) { add(if (feels >= 23) "버킷햇" else "모자"); add("선글라스") }
        }
        val tip = when {
            rainy -> "비 소식이 있어요(강수확률 ${weather!!.precipProb}%). 우산 꼭 챙기세요."
            strongUv -> "자외선이 강해요. 모자나 선글라스를 챙기세요."
            weather != null && weather.max - weather.min >= 10 -> "일교차가 커요. 벗고 입기 쉬운 겉옷을 챙기세요."
            else -> "이달의 무드 컬러 '${mood.name}', 소품으로 살짝 더해 봐요."
        }
        val shortName = { s: OutfitSlot -> if (s.fromCloset) s.color.name else s.item }
        // "아이보리 니트 + 데님 스커트"; a dress look leads with the outer when there is one.
        val main = slots.filter { it.label == "상의" || it.label == "하의" || it.label == "원피스" }
        val headlineSlots = if (main.size < 2) slots.filter { it.label == "아우터" } + main else main
        return Outfit(
            layerLine = if (weather != null) "체감 ${feels}°C · ${band.name}" else "${date.monthValue}월 평균 기온 기준 · ${band.name}",
            slots = slots,
            lip = closetLip?.let { NamedColor(it.name, it.hex) } ?: m.lipColors.pick(rnd, accent),
            lipFromCloset = closetLip != null,
            eye = m.eyeColors.pick(rnd, neutrals.base.first()),
            blush = m.blushColors.pick(rnd, accent),
            metal = tone.accessory.metal,
            moodColor = mood,
            extras = extras,
            tip = tip,
            headline = headlineSlots.joinToString(" + ", transform = shortName),
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
        return ranked[Math.floorMod(variant, minOf(3, ranked.size))]
    }

    private fun List<NamedColor>.pick(rnd: Random, fallback: NamedColor) = if (isEmpty()) fallback else this[rnd.nextInt(size)]

    // ponytail: CIE76 distance — fine for picking among a handful of colors; swap in CIEDE2000 if ranking looks off.
    internal fun deltaE(a: Lab, b: Lab) = sqrt((a.l - b.l).let { it * it } + (a.a - b.a).let { it * it } + (a.b - b.b).let { it * it })

    /** Malformed hex (bad content or a closet entry) reads as mid grey instead of throwing; "#AARRGGBB" keeps its RGB. */
    internal fun lab(hex: String): Lab {
        val v = hex.removePrefix("#").toLongOrNull(16)?.toInt() ?: 0x808080
        return ColorMath.rgbToLab(Rgb(v shr 16 and 0xFF, v shr 8 and 0xFF, v and 0xFF))
    }
}
