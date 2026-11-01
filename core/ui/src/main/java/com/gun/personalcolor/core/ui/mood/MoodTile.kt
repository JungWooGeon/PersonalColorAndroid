package com.gun.personalcolor.core.ui.mood

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.R
import com.gun.personalcolor.core.ui.drawClothing
import com.gun.personalcolor.core.ui.drawClothingShadow
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 감성 룩북 serif for quiet labels (kind names). */
val Myeongjo = FontFamily(Font(R.font.nanum_myeongjo))

// Scene A bases at half size (380×544, ~0.8 MB each); tiles show square windows of it.
private val bases = ConcurrentHashMap<Boolean, ImageBitmap>()
// Square windows (x, y, side) in the half-size base, picked to catch a leaf/window shadow without a hard highlight in the middle.
private val windows = listOf(intArrayOf(10, 40, 210), intArrayOf(150, 110, 210), intArrayOf(40, 250, 210), intArrayOf(160, 310, 210))

private fun loadBase(ctx: Context, night: Boolean): ImageBitmap? = bases[night] ?: runCatching {
    ctx.assets.open(if (night) "mood/A_night_base.webp" else "mood/A_sunny_base.webp").use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })!!.asImageBitmap()
    }
}.getOrNull()?.also { bases[night] = it }

/**
 * Linen-and-window-light backdrop for a garment tile: a crop of the A scene (sunny by day, warm night in dark theme),
 * veiled so it stays quiet behind small art. [seed] picks the crop so a grid doesn't repeat the same shadow.
 */
@Composable
fun Modifier.moodBackdrop(seed: Int = 0): Modifier {
    val night = PcTheme.colors.isDark
    val ctx = LocalContext.current.applicationContext
    val base by produceState(bases[night], night) { if (value == null) value = withContext(Dispatchers.IO) { loadBase(ctx, night) } }
    val w = windows[Math.floorMod(seed, windows.size)]
    return drawBehind {
        val aspect = size.width / size.height
        drawRect(if (night) Color(0xFF2E2826) else Color(0xFFF4E9DC))
        base?.let {
            // Same aspect as the tile (wide previews get a wider window), kept inside the bitmap.
            val sw = (w[2] * maxOf(1f, aspect)).toInt().coerceAtMost(it.width)
            val sh = (sw / aspect).toInt().coerceAtMost(it.height)
            val src = IntOffset(w[0].coerceAtMost(it.width - sw), w[1].coerceAtMost(it.height - sh))
            drawImage(it, src, IntSize(sw, sh), dstSize = IntSize(size.width.toInt() + 1, size.height.toInt() + 1))
        }
        if (night) {
            drawRect(Color(0xFFB9ADA5), blendMode = BlendMode.Multiply) // cool lavender → dim, low-saturation warm night
            drawRect(Color(0xFF3A2E28).copy(alpha = 0.28f), blendMode = BlendMode.Saturation)
        } else {
            drawRect(Color(0xFFF8F1E8).copy(alpha = if (aspect > 1.3f) 0.55f else 0.42f)) // soften the sun so the garment leads; wide crops catch more streaks
        }
        // Gentle warm light from the window (top-left), settling a touch darker toward the bottom.
        drawRect(Brush.radialGradient(listOf(Color(if (night) 0x26FFC89A else 0x40FFF6E6), Color.Transparent), Offset(size.width * 0.15f, 0f), size.maxDimension * 1.1f))
        drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xFF5A3E2E).copy(alpha = if (night) 0.18f else 0.08f)))
    }
}

/** Garment art lying on the tile with a soft contact shadow (art itself unchanged). */
@Composable
fun ShadowedGarment(kind: String, color: Color, modifier: Modifier = Modifier) {
    val shadow = if (PcTheme.colors.isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF5A3A28).copy(alpha = 0.3f)
    Canvas(modifier) {
        val side = size.minDimension
        translate(side * 0.012f, side * 0.03f) { drawClothingShadow(kind, shadow, blur = 5f) }
        drawClothing(kind, color)
    }
}
