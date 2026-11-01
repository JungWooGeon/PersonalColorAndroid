package com.gun.personalcolor.core.model

import kotlinx.serialization.Serializable

enum class Verdict { GOOD, OK, AVOID }

/**
 * An item in "내 옷장". [imageFile] is the cut-out garment photo (PNG, transparent background) stored in the app's
 * private files dir (`filesDir/closet/<id>.png`) — on device only. Null for color-only items saved from 컬러 체크.
 */
@Serializable data class ClosetItem(
    val id: Long,                 // creation time millis; unique enough for a personal list
    val hex: String,              // "#RRGGBB"
    val name: String,             // user label, e.g. "베이지 니트"
    val category: String,         // one of CLOSET_CATEGORIES
    val toneId: String,           // reference tone at check time
    val verdict: Verdict,
    val score: Int,               // 0..100
    val imageFile: String? = null,  // file name under filesDir/closet/, e.g. "1793500000000.png"
    /** Garment kind for outfit matching, one of [CLOTHING_KINDS] keys (e.g. "knit"); null = unknown. */
    val kind: String? = null,
)

val CLOSET_CATEGORIES = listOf("상의", "하의", "아우터", "원피스", "신발", "립·메이크업", "액세서리", "기타")

private fun kinds(category: String, vararg p: Pair<String, String>) = p.map { (k, label) -> k to (label to category) }

/**
 * Garment kinds shared by 내 옷장 (what the user photographs) and 오늘의 코디 (what it recommends + its illustration).
 * key → (Korean label, closet category). Keys are stored in user data: never rename one.
 */
val CLOTHING_KINDS: Map<String, Pair<String, String>> = linkedMapOf(
    *kinds("아우터",
        "coat" to "롱 울코트", "short_coat" to "하프코트", "duffle" to "떡볶이 코트", "trench" to "트렌치코트",
        "jacket" to "크롭 자켓", "blazer" to "오버핏 블레이저", "tweed" to "트위드 자켓", "denim_jacket" to "크롭 청자켓",
        "leather" to "레더 자켓", "mustang" to "무스탕", "windbreaker" to "바람막이", "zipup" to "후드 집업",
        "fleece" to "뽀글이", "vest" to "패딩 베스트", "padding" to "크롭 패딩", "long_padding" to "롱패딩", "bolero" to "볼레로",
    ).toTypedArray(),
    *kinds("상의",
        "tee" to "반팔 티셔츠", "longtee" to "긴팔 티셔츠", "crop_tee" to "크롭 티", "ribbed_top" to "골지 탑",
        "shirt" to "셔츠", "blouse" to "블라우스", "polo" to "카라 니트", "off_shoulder" to "오프숄더", "knit" to "니트",
        "turtleneck" to "터틀넥", "cardigan" to "크롭 가디건", "sweatshirt" to "맨투맨", "hoodie" to "후드티",
        "sleeveless" to "민소매", "knit_vest" to "니트 베스트", "bustier" to "뷔스티에",
    ).toTypedArray(),
    *kinds("하의",
        "pants" to "코튼 팬츠", "slacks" to "슬랙스", "wide" to "와이드 팬츠", "jeans" to "데님 팬츠", "bootcut" to "부츠컷",
        "jogger" to "스웨트 팬츠", "cargo" to "카고 팬츠", "leggings" to "레깅스", "shorts" to "쇼츠", "bermuda" to "버뮤다 팬츠",
        "skirt" to "미디 스커트", "mini_skirt" to "미니스커트", "pleats" to "플리츠 스커트", "long_skirt" to "롱스커트",
        "denim_skirt" to "데님 스커트",
    ).toTypedArray(),
    *kinds("원피스",
        "dress" to "원피스", "mini_dress" to "미니 원피스", "shirt_dress" to "셔츠 원피스", "knit_dress" to "니트 원피스",
        "slip_dress" to "슬립 원피스", "jumpsuit" to "점프수트", "overall" to "멜빵",
    ).toTypedArray(),
    *kinds("신발",
        "shoes" to "스니커즈", "running" to "레트로 스니커즈", "platform" to "통굽 로퍼", "loafers" to "로퍼",
        "heels" to "힐·펌프스", "flats" to "발레리나 플랫", "mary_jane" to "메리제인", "ankle_boots" to "앵클부츠",
        "long_boots" to "롱부츠", "western" to "웨스턴 부츠", "winter_boots" to "어그 부츠", "rain_boots" to "레인부츠",
        "sandals" to "샌들", "slippers" to "뮬·슬리퍼",
    ).toTypedArray(),
    *kinds("액세서리",
        "bag" to "토트백", "shoulder_bag" to "숄더백", "hobo" to "호보백", "mini_bag" to "미니백", "crossbody" to "크로스백",
        "backpack" to "백팩", "clutch" to "클러치", "hat" to "볼캡", "beanie" to "비니", "bucket_hat" to "버킷햇",
        "beret" to "베레모", "scarf" to "스카프", "muffler" to "머플러", "gloves" to "장갑", "belt" to "벨트",
        "ribbon" to "리본", "hair_clip" to "헤어 집게", "sunglasses" to "선글라스", "earrings" to "귀걸이",
        "necklace" to "목걸이", "watch" to "시계", "socks" to "양말", "leg_warmers" to "레그워머", "umbrella" to "우산",
    ).toTypedArray(),
)

/** Shoe kinds (closet category "신발"); older items may still sit under 액세서리. */
val SHOE_KINDS: Set<String> = CLOTHING_KINDS.filterValues { it.second == "신발" }.keys

/** Bag kinds (closet category "액세서리"). */
val BAG_KINDS: Set<String> = setOf("bag", "shoulder_bag", "hobo", "mini_bag", "crossbody", "backpack", "clutch")
