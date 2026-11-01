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

/** The bed scene's 4:5 hero view (A_layout.json "hero" crop, 760×950). */
private const val BED_ASPECT = 760f / 950f
private const val LIGHT_MS = 2000

/** The three light phases of the last couple of looks, so 다른 코디 보기 and back don't re-render. */
private val heroCache = LruCache<String, List<ImageBitmap>>(2)

/**
 * The 오늘의 코디 hero: today's look on the linen bed under window light. The light breathes slowly through its three
 * pre-rendered phases (crossfade, ~6 s a cycle) while the screen is resumed; a new look crossfades in the same way.
 */
@Composable
internal fun MoodHero(look: MoodLook, modifier: Modifier = Modifier, description: String? = null, onShown: (MoodLook) -> Unit = {}) {
    val context = LocalContext.current
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(BED_ASPECT).clip(androidx.compose.material3.MaterialTheme.shapes.extraLarge)
        .background(Color(0xFFEFE4D3)).semantics { if (description != null) contentDescription = description }) {
        val w = constraints.maxWidth; val h = constraints.maxHeight
        // (look, its frames): the old look keeps breathing until the new one is rendered.
        val shown by produceState(heroCache.get(key(look, w, h))?.let { look to it }, look, w, h) {
            if (w <= 0 || h <= 0) return@produceState // not laid out yet: a 0-px bitmap would throw
            val k = key(look, w, h)
            val frames = heroCache.get(k) ?: withContext(Dispatchers.Default) {
                try {
                    (0..2).map { p ->
                        SceneBed.render(context, look.pieces, look.weather, look.line, look.caption, w, h, p, look.locked, false,
                            pill = null, hero = true).asImageBitmap()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Throwable) { // OOM on a huge window, a broken asset…: keep the previous scene (or the backdrop)
                    android.util.Log.w("MoodHero", "render failed", e); null
                }
            }?.also { heroCache.put(k, it) } ?: return@produceState
            value = look to frames
        }
        val frames = shown?.second
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
            shown?.first?.let(onShown) // the words below change as the new scene starts to fade in
            fade.snapTo(0f); fade.animateTo(1f, tween(LIGHT_MS, easing = LinearEasing))
            under = null
        }
        // A new look whose scene is already on screen (same pieces and words): nothing fades, the words just follow.
        LaunchedEffect(shown?.first) { if (over != null && shown?.second?.any { it === over } == true) shown?.first?.let(onShown) }
        under?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        over?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }, contentScale = ContentScale.Crop) }
    }
}

private fun key(l: MoodLook, w: Int, h: Int) = "${l.pieces}|${l.weather}|${l.line}|${l.caption}|${l.locked}|$w×$h"

/**
 * "TODAY'S LOOK" share image, 1080×1350: the 4:5 hero view of the bed scene as a print on cream paper, the line and
 * "TODAY'S LOOK" on the linen, and the caption and the app name small on the paper below.
 */
internal fun renderMoodShare(context: Context, look: MoodLook, brand: String): Bitmap {
    val width = 1080; val height = 1350
    val m = 48f
    val pw = (width - 2 * m).toInt(); val ph = (pw * 950f / 760f).toInt()
    val scene = SceneBed.render(context, look.pieces, look.weather, look.line, "TODAY'S LOOK", pw, ph, 0, look.locked, false,
        pill = null, rounded = false, hero = true)
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
    c.drawBitmap(scene, m, m, Paint(Paint.FILTER_BITMAP_FLAG))
    scene.recycle()
    val ko = runCatching { ResourcesCompat.getFont(context, DsR.font.nanum_myeongjo) }.getOrNull() ?: Typeface.SERIF
    val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    val base = (m + ph + height) / 2 + 9f // centered on the paper below the print
    p.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); p.color = 0xFF7A6A5C.toInt(); p.textSize = 26f; p.letterSpacing = .02f
    c.drawText(look.caption, m + 4f, base, p)
    p.typeface = ko; p.textSize = 25f; p.letterSpacing = .04f
    c.drawText(brand, width - m - 4f - p.measureText(brand), base, p)
    return out
}
