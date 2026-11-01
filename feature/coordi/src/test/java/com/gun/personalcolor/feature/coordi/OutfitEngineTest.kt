package com.gun.personalcolor.feature.coordi

import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.BAG_KINDS
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.SHOE_KINDS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.weather.TodayWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate

class OutfitEngineTest {
    private val tone = Tone(
        id = "autumn_true", season = "autumn", nameKo = "가을 웜", nameEn = "Autumn True",
        keywords = emptyList(), summary = "", characteristics = "",
        bestColors = listOf(
            NamedColor("테라코타", "#C0674A"), NamedColor("머스터드", "#D2A23A"), NamedColor("카멜", "#C19A6B"),
            NamedColor("올리브 그린", "#708238"), NamedColor("초콜릿 브라운", "#5A3A2A"), NamedColor("웜 아이보리", "#F3E9D2"),
        ),
        worstColors = listOf(NamedColor("네온 핑크", "#FF4FA3"), NamedColor("아이시 블루", "#D6ECF7")),
        makeup = Makeup("", listOf(NamedColor("브릭", "#A0453A")), "", listOf(NamedColor("브론즈", "#8C6239")), "",
            listOf(NamedColor("코랄", "#E9765B")), ""),
        hair = Hair(emptyList(), ""), fashion = Fashion("", emptyList()), accessory = Accessory("골드", ""), avoidTip = "",
    )
    private val moods = listOf(NamedColor("아이스 블루", "#CFE0EA"), NamedColor("카멜 브라운", "#A9794C"), NamedColor("네온 핑크", "#FF4FA3"))
    private val date = LocalDate.of(2026, 11, 1)

    private fun weather(feels: Double, rain: Int = 0, uv: Double = 2.0) =
        TodayWeather("마포구 망원동", feels, feels, feels - 3, feels + 3, rain, "맑음", 0, uv, 5.0, 0L)

    @Test fun deterministicPerDay() {
        val a = OutfitEngine.build(tone, weather(14.0), date, moods)
        assertEquals(a, OutfitEngine.build(tone, weather(14.0), date, moods))
        val variants = (0..5).map { OutfitEngine.build(tone, weather(14.0), date, moods, variant = it) }.toSet()
        assertTrue("variants should differ", variants.size > 1)
    }

    @Test fun layeringFollowsFeelsLike() {
        assertTrue(OutfitEngine.build(tone, weather(30.0), date, moods).slots.none { it.label == "아우터" })
        val cold = OutfitEngine.build(tone, weather(2.0), date, moods)
        assertTrue(cold.slots.first { it.label == "아우터" }.kind in setOf("long_padding", "coat", "padding", "mustang", "duffle"))
        assertTrue(cold.slots.any { it.label == "소품" && it.kind in setOf("muffler", "beanie") })
        assertTrue(OutfitEngine.build(tone, weather(14.0), date, moods).layerLine.contains("자켓·트렌치"))
    }

    @Test fun rainAndUv() {
        val rainy = OutfitEngine.build(tone, weather(15.0, rain = 70), date, moods)
        assertTrue("우산" in rainy.extras)
        assertTrue(rainy.slots.first { it.label == "아우터" }.kind in setOf("trench", "blazer", "jacket", "denim_jacket", "leather", "windbreaker"))
        // "방수" only where it makes sense.
        for (v in 0..20) OutfitEngine.build(tone, weather(7.0, rain = 70), date, moods, variant = v).slots
            .forEach { assertTrue(it.item, "방수" !in it.item || it.kind == "windbreaker") }
        assertTrue(rainy.tip.contains("70%"))
        val sunny = OutfitEngine.build(tone, weather(25.0, uv = 8.0), date, moods)
        assertTrue("선글라스" in sunny.extras)
    }

    @Test fun seasonDefaultsWithoutWeather() {
        val winter = OutfitEngine.build(tone, null, LocalDate.of(2026, 1, 10), moods)
        assertTrue(winter.layerLine.startsWith("1월"))
        assertTrue(winter.slots.any { it.label == "아우터" })
    }

    @Test fun moodClosestToPaletteAvoidsWorst() {
        assertEquals("카멜 브라운", OutfitEngine.closestMood(moods, tone)!!.name)
    }

    @Test fun prefersClosetItems() {
        val closet = listOf(
            ClosetItem(1, "#C19A6B", "카멜 니트", "상의", "autumn_true", Verdict.GOOD, 90),
            ClosetItem(2, "#FF4FA3", "핑크 셔츠", "상의", "autumn_true", Verdict.AVOID, 10),
        )
        // Feels 18 → 니트 band; skip variants that pick a dress instead of top+bottom.
        val outfit = (0..10).map { OutfitEngine.build(tone, weather(18.0), date, moods, closet, it) }.first { o -> o.slots.any { it.label == "상의" } }
        val top = outfit.slots.first { it.label == "상의" }
        assertTrue(top.fromCloset)
        assertEquals("내 옷장의 '카멜 니트'", top.item)
        assertNotEquals("핑크 셔츠", top.color.name)
    }

    @Test fun kindOfLabelsAndSynonyms() {
        // Every catalog label (what a closet item is usually named after) maps back to its own kind.
        CLOTHING_KINDS.forEach { (kind, v) -> assertEquals(v.first, kind, kindOf("카멜 ${v.first}", "")) }
        val synonyms = mapOf(
            "울코트" to "coat", "숏패딩" to "padding", "방수 바람막이" to "windbreaker", "가죽 자켓" to "leather", "플리스" to "fleece",
            "패딩 조끼" to "vest", "니트 조끼" to "knit_vest", "터틀넥 니트" to "turtleneck", "폴로 셔츠" to "polo", "얇은 긴팔 티" to "longtee",
            "린넨 반팔 셔츠" to "shirt", "청바지" to "jeans", "청치마" to "denim_skirt", "조거 팬츠" to "jogger", "운동화" to "running",
            "플랫슈즈" to "flats", "방한 부츠" to "winter_boots", "장화" to "rain_boots", "목도리" to "muffler", "헤어 집게핀" to "hair_clip",
            "부클 자켓" to "tweed", "통굽 메리제인" to "platform", "가방" to "bag",
        )
        synonyms.forEach { (text, kind) -> assertEquals(text, kind, kindOf("카멜 $text", "")) }
        assertEquals("knit", kindOf("내 옷장의 '카멜 니트'", "상의"))
        assertEquals("tee", kindOf("최애 상의", "상의"))
        assertEquals("crossbody", kindOf("최애 소품", "포인트")) // unknown accessories still get a picture
        assertEquals(null, kindOf("최애 소품", ""))
    }

    @Test fun kindList() {
        assertEquals(93, CLOTHING_KINDS.size)
        assertEquals(listOf("아우터", "상의", "하의", "원피스", "신발", "액세서리"), CLOTHING_KINDS.values.map { it.second }.distinct())
        assertEquals(14, SHOE_KINDS.size)
        assertTrue(CLOTHING_KINDS.keys.containsAll(BAG_KINDS))
    }

    @Test fun everyGeneratedSlotHasKnownKind() {
        for (feels in -5..32 step 3) for (rain in listOf(0, 80)) for (v in 0..12) {
            val outfit = OutfitEngine.build(tone, weather(feels.toDouble(), rain), date, moods, variant = v)
            outfit.slots.forEach { assertTrue(it.item, it.kind in CLOTHING_KINDS) }
            assertTrue(outfit.slots.any { it.label == "신발" } && outfit.slots.any { it.label == "가방" })
            assertTrue("headline skips shoes", outfit.slots.first { it.label == "신발" }.item !in outfit.headline)
        }
    }

    @Test fun rainyDayShoes() {
        val kinds = (0..30).map { OutfitEngine.build(tone, weather(15.0, rain = 70), date, moods, variant = it).slots.first { s -> s.label == "신발" }.kind }.toSet()
        assertTrue(kinds.toString(), "rain_boots" in kinds && kinds.all { it in setOf("rain_boots", "loafers") })
    }

    @Test fun closetShoesFillShoeSlot() {
        // Saved before 신발 existed: category 액세서리, kind loafers. Must count as shoes, never as the point item.
        val closet = listOf(ClosetItem(1, "#5A3A2A", "브라운 로퍼", "액세서리", "autumn_true", Verdict.GOOD, 90, kind = "loafers"))
        val outfit = OutfitEngine.build(tone, weather(14.0), date, moods, closet)
        assertEquals("내 옷장의 '브라운 로퍼'", outfit.slots.first { it.label == "신발" }.item)
        assertTrue(outfit.slots.first { it.label == "가방" }.fromCloset.not())
    }

    @Test fun closetSlotCarriesKindAndPhoto() {
        val closet = listOf(ClosetItem(1, "#C19A6B", "최애 상의", "상의", "autumn_true", Verdict.GOOD, 90, imageFile = "1.png", kind = "sweatshirt"))
        val top = (0..10).map { OutfitEngine.build(tone, weather(18.0), date, moods, closet, it) }
            .firstNotNullOf { o -> o.slots.firstOrNull { it.label == "상의" } }
        assertEquals("sweatshirt", top.kind)
        assertEquals("1.png", top.imageFile)
    }

    @Test fun everySlotHasArtAndRainyColorsDiffer() {
        val closet = listOf(ClosetItem(id = 1, name = "진주 반지", hex = "#EDE6DA", category = "액세서리", toneId = tone.id, verdict = Verdict.GOOD, score = 90))
        for (feels in listOf(-5.0, 3.0, 7.0, 10.0, 14.0, 18.0, 21.0, 25.0, 30.0)) for (rain in listOf(0, 80)) for (v in 0..11) {
            listOf(emptyList(), closet).forEach { items ->
                val o = OutfitEngine.build(tone, weather(feels, rain), date.plusDays(v.toLong()), moods, items, v)
                o.slots.forEach { assertTrue("${it.item} has no art", it.kind != null && it.kind in CLOTHING_KINDS) }
                if (rain > 0) assertNotEquals(o.slots.first { it.label == "신발" }.color, o.slots.first { it.label == "소품" }.color)
            }
        }
    }

    // --- Styling rules over all 12 real tones (see the rule block in OutfitEngine) ---

    private val tones: List<Tone> by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val file = listOf("../../core/data/src/main/assets/tones.json", "core/data/src/main/assets/tones.json").map(::File).first { it.exists() }
        json.parseToJsonElement(file.readText()).let { (it as JsonObject).getValue("tones").jsonArray }
            .map { json.decodeFromJsonElement(Tone.serializer(), it) }
    }

    private val ORDER = listOf("아우터", "상의", "하의", "원피스", "신발", "가방", "소품")
    private val OFF_TARGET = setOf("polo", "jogger", "windbreaker", "running", "hoodie", "fleece", "zipup", "cargo", "backpack", "hat")

    @Test fun stylingRulesHoldForEveryToneAndWeather() {
        assertEquals(12, tones.size)
        val seen = mutableSetOf<String>()
        for (t in tones) {
            val neutral = OutfitEngine.neutralsOf(t).all.map { it.hex }.toSet()
            for (feels in -5..32) for (rain in listOf(0, 80)) for (v in 0..11) {
                val o = OutfitEngine.build(t, weather(feels.toDouble(), rain), date, moods, variant = v)
                val desc = "${t.id} $feels°C rain=$rain v$v: ${o.slots.map { it.item }}"
                val accents = o.slots.map { it.color.hex }.filter { it !in neutral }.toSet()
                assertTrue("≤2 accent colors: $desc", accents.size <= 2)
                assertTrue("≤1 loud color: $desc", accents.count { OutfitEngine.lab(it).chroma > 45 } <= 1)
                val shoes = o.slots.single { it.label == "신발" }
                val point = o.slots.single { it.label == "가방" }
                val acc = o.slots.singleOrNull { it.label == "소품" }
                val big = o.slots.filter { it.label in setOf("아우터", "상의", "하의", "원피스") }
                val accentPieces = o.slots.filter { it.color.hex !in neutral }
                if (shoes.color.hex !in neutral || point.color.hex !in neutral || (acc != null && acc.color.hex !in neutral))
                    assertTrue("shoes/bag colored only as the single accent: $desc", accentPieces.map { it.color.hex }.toSet().size == 1 &&
                        accentPieces.none { it.label in setOf("아우터", "상의", "하의", "원피스") })
                assertTrue("shoes are neutral: $desc", shoes.color.hex in neutral)
                o.slots.forEach { s ->
                    assertTrue("known kind: $desc", s.kind in CLOTHING_KINDS)
                    assertTrue("off-target default ${s.kind}: $desc",
                        s.kind !in OFF_TARGET || (s.kind == "windbreaker" && rain > 0) || (s.kind == "running" && feels in 17..22))
                    seen += s.kind!!
                }
                val idx = o.slots.map { ORDER.indexOf(it.label) }
                assertEquals("wearing order: $desc", idx.sorted(), idx)
                assertTrue("no -1: $desc", -1 !in idx)
                assertTrue("always a bag: $desc", point.kind in BAG_KINDS)
                assertTrue("never all dark: $desc", big.any { OutfitEngine.lab(it.color.hex).l >= 50 } ||
                    o.slots.single { it.label == "상의" || it.label == "원피스" }.color.hex !in neutral)
                o.slots.firstOrNull { it.label == "아우터" }?.let { assertNotEquals("no pure white coat: $desc", "#FFFFFF", it.color.hex.uppercase()) }
                assertTrue("no pure white: $desc", o.slots.none { it.color.hex.uppercase() == "#FFFFFF" })
                assertTrue("≤1 white big piece: $desc", big.count { OutfitEngine.lab(it.color.hex).l > 90 } <= 1)
                if (shoes.kind in setOf("long_boots", "winter_boots")) assertTrue("no white boots: $desc", OutfitEngine.lab(shoes.color.hex).l <= 85)
                if (rain > 0) {
                    o.slots.filter { it.label == "하의" || it.label == "원피스" }.forEach {
                        assertTrue("rain: no white/ivory bottoms: $desc", it.color.hex !in neutral || it.color.name in setOf("연청", "중청", "진청", "흑청") || OutfitEngine.lab(it.color.hex).l < 75)
                    }
                    assertEquals("umbrella: $desc", "umbrella", acc?.kind)
                    assertTrue("rainy shoes: $desc", shoes.kind == "rain_boots" || (shoes.kind == "loafers" && shoes.item.startsWith("블랙")))
                    assertTrue("no colored boots: $desc", shoes.color.hex in neutral)
                    assertTrue("umbrella black/ivory or the accent: $desc", acc!!.color.hex !in neutral ||
                        acc.color.name in setOf("블랙", "아이보리", "화이트", "오프 화이트"))
                    assertNotEquals("umbrella ≠ shoes: $desc", shoes.color.hex, acc.color.hex)
                } else if (feels < 9) assertTrue("muffler or beanie when cold: $desc", acc?.kind in setOf("muffler", "beanie"))
                else assertEquals("no 소품: $desc", null, acc)
                assertTrue("headline: $desc", o.headline.isNotBlank() && o.headline.count { it == '+' } <= 1 && shoes.item !in o.headline)
                if (feels < 12) o.slots.firstOrNull { it.label == "하의" && it.kind in setOf("mini_skirt", "pleats", "long_skirt") }
                    ?.let { assertTrue("skirt in the cold wants boots: $desc", shoes.kind in setOf("long_boots", "ankle_boots", "winter_boots", "rain_boots", "loafers")) }
            }
        }
        // Variety: the target's staples all show up somewhere.
        listOf("muffler", "beanie", "coat", "short_coat", "duffle", "trench", "tweed", "blazer", "cardigan", "blouse", "crop_tee", "slip_dress",
            "denim_skirt", "long_skirt", "long_boots", "mary_jane", "loafers", "sandals", "flats", "shoulder_bag", "umbrella")
            .forEach { assertTrue("never picked: $it", it in seen) }
    }
}
