package com.gun.personalcolor.feature.coordi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.mood.MoodCopy
import com.gun.personalcolor.core.ui.mood.SceneBed
import com.gun.personalcolor.core.weather.TodayWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.gun.personalcolor.core.designsystem.R as DsR

/** What the A scene shows: pieces (kind, ARGB) or none when locked, the sky and the words. */
internal data class MoodLook(val pieces: List<Pair<String, Int>>, val weather: String, val line: String, val caption: String, val locked: Boolean)

/** Today's look in the bed scene. The weather is the screen's real weather; night by the hour. */
internal fun moodLook(outfit: Outfit, tone: Tone, weather: TodayWeather?, hour: Int = java.time.LocalTime.now().hour): MoodLook {
    val pieces = outfit.slots.mapNotNull { s -> s.kind?.let { k -> runCatching { k to hexColor(s.color.hex).toArgb() }.getOrNull() } }
    val sky = MoodCopy.sky(weather?.condition, hour)
    val temp = weather?.temp?.roundToInt()
    return MoodLook(pieces, sky, MoodCopy.line(sky, weather?.condition, temp, MoodCopy.accent(tone, pieces)),
        MoodCopy.caption(sky, weather?.condition, temp), locked = false)
}

/** Locked: the scene with generic pieces under a veil. Never today's outfit. */
internal fun lockedMoodLook(weather: TodayWeather?, hour: Int = java.time.LocalTime.now().hour): MoodLook {
    val sky = MoodCopy.sky(weather?.condition, hour)
    return MoodLook(emptyList(), sky, "오늘의 룩이 도착했어요", MoodCopy.caption(sky, weather?.condition, weather?.temp?.roundToInt()).substringBefore(" · ") + " · today's look", locked = true)
}

/** Bed scene aspect (A base 760×1088). */
private const val BED_ASPECT = 760f / 1088f
private const val LIGHT_MS = 2000

/** The three light phases of the last couple of looks, so 다른 코디 보기 and back don't re-render. */
private val heroCache = LruCache<String, List<ImageBitmap>>(2)

/**
 * The 오늘의 코디 hero: today's look on the linen bed under window light. The light breathes slowly through its three
 * pre-rendered phases (crossfade, ~6 s a cycle) while the screen is resumed; a new look crossfades in the same way.
 */
@Composable
internal fun MoodHero(look: MoodLook, modifier: Modifier = Modifier, description: String? = null) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(BED_ASPECT).clip(androidx.compose.material3.MaterialTheme.shapes.extraLarge)
        .background(Color(0xFFEFE4D3)).semantics { if (description != null) contentDescription = description }) {
        val w = constraints.maxWidth; val h = constraints.maxHeight
        val frames by produceState<List<ImageBitmap>?>(heroCache.get(key(look, w, h)), look, w, h) {
            val k = key(look, w, h)
            value = heroCache.get(k) ?: withContext(Dispatchers.Default) {
                (0..2).map { p ->
                    SceneBed.render(context, look.pieces, look.weather, look.line, look.caption, w, h, p, look.locked, false, pill = null).asImageBitmap()
                }
            }.also { heroCache.put(k, it) }
        }
        var phase by remember { mutableIntStateOf(0) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { delay(LIGHT_MS.toLong()); phase = (phase + 1) % 3 } }
        }
        // The incoming frame fades in over the outgoing one, which stays opaque: no dip to the backdrop halfway.
        val target = frames?.get(phase)
        var under by remember { mutableStateOf<ImageBitmap?>(null) }
        var over by remember { mutableStateOf<ImageBitmap?>(null) }
        val fade = remember { Animatable(1f) }
        LaunchedEffect(target) {
            if (target == null || target === over) return@LaunchedEffect
            under = over; over = target
            fade.snapTo(0f); fade.animateTo(1f, tween(LIGHT_MS, easing = LinearEasing))
            under = null
        }
        under?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        over?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }, contentScale = ContentScale.Crop) }
    }
}

private fun key(l: MoodLook, w: Int, h: Int) = "${l.pieces}|${l.weather}|${l.line}|${l.caption}|${l.locked}|$w×$h"

/**
 * "TODAY'S LOOK" share image, 1080×1350: the bed scene as a film print on cream paper. The line sits on the linen above
 * the clothes; the caption and the app name are small on the paper below.
 * (At 4:5 the 760×1088 scene can't keep its words and the shoes, so its empty top is cut and the words are redrawn.)
 */
internal fun renderMoodShare(context: Context, look: MoodLook, brand: String): Bitmap {
    val width = 1080; val height = 1350
    val m = 40f; val band = 118f
    val pw = (width - 2 * m).toInt(); val ph = (height - m - band).toInt()
    val tall = (pw * 1088f / 760f).toInt()
    val cut = (tall - ph).toFloat() // all from the top, where the widget's words were
    val scene = SceneBed.render(context, look.pieces, look.weather, "", "", pw, tall, 0, look.locked, false, pill = null, rounded = false)
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawColor(0xFFF3EDE3.toInt())
    // paper grain from the scene's own grain tile
    runCatching { context.assets.open("mood/grain.webp").use(android.graphics.BitmapFactory::decodeStream) }.getOrNull()?.let { g ->
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply {
            shader = android.graphics.BitmapShader(g, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY); alpha = 40
        })
    }
    c.save()
    c.clipRect(m, m, m + pw, m + ph)
    c.drawBitmap(scene, m, m - cut, Paint(Paint.FILTER_BITMAP_FLAG))
    c.restore()
    scene.recycle()
    val ink = when (look.weather) { "night" -> 0xFFEDE6DA; "rain" -> 0xFF33323A; "cloudy" -> 0xFF45403B; else -> 0xFF4A3A2E }.toInt()
    val soft = when (look.weather) { "night" -> 0xD9CFC6B8; "rain" -> 0xD94E4D57; "cloudy" -> 0xD9625A53; else -> 0xD96E5848 }.toInt()
    val ko = runCatching { ResourcesCompat.getFont(context, DsR.font.nanum_myeongjo) }.getOrNull() ?: Typeface.SERIF
    val italic = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    val x = m + 56f
    p.typeface = italic; p.color = soft; p.textSize = 28f; p.letterSpacing = .14f
    c.drawText("TODAY'S LOOK", x, m + 84f, p)
    p.typeface = ko; p.color = ink; p.textSize = 64f; p.letterSpacing = 0f
    p.measureText(look.line).takeIf { it > 700f }?.let { p.textSize *= 700f / it }
    c.drawText(look.line, x, m + 170f, p)
    // the paper below the print
    val base = height - band / 2 + 10f
    p.typeface = italic; p.color = 0xFF7A6A5C.toInt(); p.textSize = 28f; p.letterSpacing = .02f
    c.drawText(look.caption, m + 8f, base, p)
    p.typeface = ko; p.textSize = 27f; p.letterSpacing = .04f
    c.drawText(brand, width - m - 8f - p.measureText(brand), base, p)
    return out
}
