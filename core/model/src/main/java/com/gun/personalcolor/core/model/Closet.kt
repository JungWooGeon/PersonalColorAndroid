package com.gun.personalcolor.core.model

import kotlinx.serialization.Serializable

enum class Verdict { GOOD, OK, AVOID }

/** A color-check result saved to "내 옷장". Only the color is stored — never the photo. */
@Serializable data class ClosetItem(
    val id: Long,                 // creation time millis; unique enough for a personal list
    val hex: String,              // "#RRGGBB"
    val name: String,             // user label, e.g. "베이지 니트"
    val category: String,         // one of CLOSET_CATEGORIES
    val toneId: String,           // reference tone at check time
    val verdict: Verdict,
    val score: Int,               // 0..100
)

val CLOSET_CATEGORIES = listOf("상의", "하의", "아우터", "원피스", "립·메이크업", "액세서리", "기타")
