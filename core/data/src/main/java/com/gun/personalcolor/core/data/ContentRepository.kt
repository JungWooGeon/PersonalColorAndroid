package com.gun.personalcolor.core.data

import android.content.Context
import android.util.Log
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.QuizQuestion
import com.gun.personalcolor.core.model.Tone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Static content bundled in assets. Edit the JSON files to change texts — no code change needed. */
@Singleton
class ContentRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun read(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    val tones: List<Tone> by lazy { json.decodeFromString<TonesFile>(read("tones.json")).tones }
    val quiz: List<QuizQuestion> by lazy { json.decodeFromString<QuizFile>(read("quiz.json")).questions }

    val articles: List<Article> by lazy { json.decodeFromString<ArticlesFile>(read("articles.json")).articles }

    /** Unknown ids (stale data, bad deep link) fall back to the first tone instead of crashing. */
    fun tone(id: String): Tone = toneOrNull(id) ?: tones.first().also { Log.w("Content", "unknown tone id: $id") }
    fun toneOrNull(id: String?): Tone? = tones.firstOrNull { it.id == id }
    fun article(id: String): Article? = articles.firstOrNull { it.id == id }

    @Serializable internal data class TonesFile(val tones: List<Tone>)
    @Serializable internal data class ArticlesFile(val articles: List<Article>)
    @Serializable internal data class QuizFile(val questions: List<QuizQuestion>)
}
