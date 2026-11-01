package com.gun.personalcolor.feature.analyze

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.TintyLoading
import com.gun.personalcolor.core.vision.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Optional step after a guided capture: the user taps the white paper in the photo so the engine can
 * white-balance against it. [onDone] gets the normalized point on the upright photo, or null to skip.
 */
@Composable
internal fun WhiteRefScreen(uri: Uri, onRetake: () -> Unit, onDone: (Offset?) -> Unit) {
    val context = LocalContext.current
    // Same decoder (EXIF-upright) as the engine, so normalized coordinates match.
    val bitmap by produceState<Bitmap?>(null, uri) { value = withContext(Dispatchers.IO) { BitmapLoader.load(context, uri) } }
    var marker by rememberSaveable(uri, stateSaver = OffsetSaver) { mutableStateOf<Offset?>(null) }
    val sample = remember(bitmap, marker) {
        val b = bitmap; val m = marker
        if (b == null || m == null) null else sampleAt(b, m)
    }

    // Photo screens are always dark (like the camera), so buttons use the dark palette on black.
    PcTheme(darkTheme = true) { CompositionLocalProvider(LocalContentColor provides Color.White) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val insets = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            val photo = @Composable { m: Modifier -> Photo(bitmap, marker, { marker = it }, m) }
            val panel = @Composable { m: Modifier -> Panel(sample, onRetake, { onDone(null) }, { onDone(marker) }, m) }
            if (maxWidth > maxHeight) {
                Row(insets) {
                    photo(Modifier.weight(1f).fillMaxHeight())
                    panel(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp))
                }
            } else {
                Column(insets) {
                    photo(Modifier.weight(1f).fillMaxWidth())
                    panel(Modifier.fillMaxWidth().padding(20.dp))
                }
            }
        }
    } }
}

@Composable
private fun Photo(bitmap: Bitmap?, marker: Offset?, onTap: (Offset) -> Unit, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap == null) { TintyLoading(size = 112.dp); return@Box }
        // The box has the photo's aspect ratio, so tap position / size = normalized image coordinates.
        Box(
            Modifier.aspectRatio(bitmap.width.toFloat() / bitmap.height).pointerInput(Unit) {
                detectTapGestures { p -> onTap(Offset(p.x / size.width, p.y / size.height)) }
            },
        ) {
            Image(remember(bitmap) { bitmap.asImageBitmap() }, "촬영한 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            if (marker != null) Canvas(Modifier.fillMaxSize()) {
                val c = Offset(marker.x * size.width, marker.y * size.height)
                drawCircle(Color.Black, 16.dp.toPx(), c, style = Stroke(5.dp.toPx()))
                drawCircle(Color.White, 16.dp.toPx(), c, style = Stroke(2.5.dp.toPx()))
            }
        }
    }
}

@Composable
private fun Panel(sample: Rgb?, onRetake: () -> Unit, onSkip: () -> Unit, onConfirm: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRetake) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "다시 찍기") }
            Text("흰 종이로 조명 맞추기", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            "사진 속 흰 종이를 누르면 더 정확해져요",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f),
        )
        if (sample != null) {
            val lab = ColorMath.rgbToLab(sample)
            val white = lab.l >= 60 && lab.chroma <= 20
            Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).background(Color(sample.r, sample.g, sample.b), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (white) "좋아요, 흰 종이예요" else "흰 종이 부분을 눌러 주세요",
                    style = MaterialTheme.typography.bodyMedium, color = if (white) CameraGreen else CameraWarn,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        PcButton("여기가 흰 종이예요", onConfirm, Modifier.fillMaxWidth(), enabled = sample != null)
        PcOutlinedButton("건너뛰기", onSkip, Modifier.fillMaxWidth())
    }
}

/** Average color around normalized point [p] (radius ~1% of the long side). */
private fun sampleAt(b: Bitmap, p: Offset): Rgb {
    val r = maxOf(2, (maxOf(b.width, b.height) * 0.01f).roundToInt())
    val x0 = ((p.x * b.width).roundToInt() - r).coerceIn(0, b.width - 1)
    val y0 = ((p.y * b.height).roundToInt() - r).coerceIn(0, b.height - 1)
    val w = minOf(2 * r + 1, b.width - x0); val h = minOf(2 * r + 1, b.height - y0)
    val px = IntArray(w * h).also { b.getPixels(it, 0, w, x0, y0, w, h) }
    // patch() takes the center in normalized coordinates of the given pixel block.
    return patch(px, w, h, Offset(0.5f, 0.5f), r)
}

private val OffsetSaver = androidx.compose.runtime.saveable.Saver<Offset?, List<Float>>(
    save = { it?.let { o -> listOf(o.x, o.y) } ?: emptyList() },
    restore = { if (it.size == 2) Offset(it[0], it[1]) else null },
)
