package com.gun.personalcolor.core.model

import kotlinx.serialization.Serializable

@Serializable data class NamedColor(val name: String, val hex: String)

@Serializable data class Makeup(
    val base: String,
    val lipColors: List<NamedColor>, val lipTip: String,
    val eyeColors: List<NamedColor>, val eyeTip: String,
    val blushColors: List<NamedColor>, val blushTip: String,
)

@Serializable data class Hair(val colors: List<NamedColor>, val tip: String)
@Serializable data class Fashion(val tip: String, val styling: List<String>)
@Serializable data class Accessory(val metal: String, val tip: String)

/** One of the 12 tones. Content comes from core/data/src/main/assets/tones.json. */
@Serializable data class Tone(
    val id: String,
    val season: String,           // spring | summer | autumn | winter
    val nameKo: String,
    val nameEn: String,
    val keywords: List<String>,
    val summary: String,
    val characteristics: String,
    val bestColors: List<NamedColor>,
    val worstColors: List<NamedColor>,
    val makeup: Makeup,
    val hair: Hair,
    val fashion: Fashion,
    val accessory: Accessory,
    val avoidTip: String,
) {
    val isWarm get() = season == "spring" || season == "autumn"
}

@Serializable data class QuizOption(val text: String, val warmth: Double, val value: Double, val clarity: Double)
@Serializable data class QuizQuestion(val id: String, val text: String, val hint: String = "", val options: List<QuizOption>)
