package com.gun.personalcolor.core.model

import kotlinx.serialization.Serializable

/** A guide article (퍼스널컬러 가이드). Content: core/data/src/main/assets/articles.json. */
@Serializable data class Article(
    val id: String,
    val category: String,         // e.g. "기초", "셀프 체크", "스타일링"
    val title: String,
    val summary: String,
    val readMinutes: Int,
    val emoji: String = "",
    val sections: List<ArticleSection>,
)

/** [body] paragraphs are separated by blank lines. [bullets] render as a list under the body. */
@Serializable data class ArticleSection(
    val heading: String,
    val body: String = "",
    val bullets: List<String> = emptyList(),
    val colors: List<NamedColor> = emptyList(),
)
