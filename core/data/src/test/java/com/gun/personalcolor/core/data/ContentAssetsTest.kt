package com.gun.personalcolor.core.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The bundled JSON must parse with the release models, and every color must be a valid "#RRGGBB" (screens parse them). */
class ContentAssetsTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun asset(name: String) = File("src/main/assets/$name").readText()
    private val hex = Regex("^#[0-9A-Fa-f]{6}$")

    @Test fun tonesParseWithTwelveUniqueIdsAndValidColors() {
        val tones = json.decodeFromString<ContentRepository.TonesFile>(asset("tones.json")).tones
        assertEquals(
            setOf("spring_light", "spring_true", "spring_bright", "summer_light", "summer_true", "summer_mute",
                "autumn_mute", "autumn_true", "autumn_deep", "winter_bright", "winter_true", "winter_deep"),
            tones.map { it.id }.toSet(),
        )
        assertEquals(12, tones.size)
        tones.forEach { t ->
            assertTrue(t.id, t.bestColors.isNotEmpty())
            val colors = t.bestColors + t.worstColors + t.makeup.lipColors + t.makeup.eyeColors + t.makeup.blushColors + t.hair.colors
            colors.forEach { assertTrue("${t.id}: ${it.hex}", hex.matches(it.hex)) }
        }
    }

    @Test fun quizParsesWithOptions() {
        val quiz = json.decodeFromString<ContentRepository.QuizFile>(asset("quiz.json")).questions
        assertTrue(quiz.isNotEmpty())
        quiz.forEach { assertTrue(it.id, it.options.isNotEmpty()) }
    }

    @Test fun articlesParseWithUniqueIdsAndValidColors() {
        val articles = json.decodeFromString<ContentRepository.ArticlesFile>(asset("articles.json")).articles
        assertTrue(articles.isNotEmpty())
        assertEquals(articles.size, articles.map { it.id }.toSet().size)
        articles.flatMap { a -> a.sections.flatMap { it.colors } }.forEach { assertTrue(it.hex, hex.matches(it.hex)) }
    }
}
