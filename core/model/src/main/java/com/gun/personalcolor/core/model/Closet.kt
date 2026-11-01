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

val CLOSET_CATEGORIES = listOf("상의", "하의", "아우터", "원피스", "립·메이크업", "액세서리", "기타")

/**
 * Garment kinds shared by 내 옷장 (what the user photographs) and 오늘의 코디 (what it recommends + its illustration).
 * key → (Korean label, closet category).
 */
val CLOTHING_KINDS: Map<String, Pair<String, String>> = linkedMapOf(
    "coat" to ("코트" to "아우터"), "trench" to ("트렌치코트" to "아우터"), "jacket" to ("자켓" to "아우터"),
    "padding" to ("패딩" to "아우터"), "cardigan" to ("가디건" to "상의"),
    "tee" to ("티셔츠" to "상의"), "shirt" to ("셔츠·블라우스" to "상의"), "knit" to ("니트" to "상의"),
    "sweatshirt" to ("맨투맨·후드" to "상의"), "sleeveless" to ("민소매" to "상의"),
    "pants" to ("팬츠" to "하의"), "jeans" to ("데님" to "하의"), "skirt" to ("스커트" to "하의"), "shorts" to ("쇼츠" to "하의"),
    "dress" to ("원피스" to "원피스"),
    "scarf" to ("스카프·머플러" to "액세서리"), "bag" to ("가방" to "액세서리"), "hat" to ("모자" to "액세서리"),
    "shoes" to ("신발" to "액세서리"), "socks" to ("양말" to "액세서리"), "umbrella" to ("우산" to "액세서리"),
)
