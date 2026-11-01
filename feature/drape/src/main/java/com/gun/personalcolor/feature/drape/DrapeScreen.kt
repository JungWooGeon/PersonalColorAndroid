package com.gun.personalcolor.feature.drape

import androidx.compose.foundation.Canvas
import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analytics.LocalAnalytics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.domain.DiagnosisSession
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.vision.FacePhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DrapeViewModel @Inject constructor(session: DiagnosisSession, val content: ContentRepository) : ViewModel() {
    val diagnosis = session.current
}

// MediaPipe face-mesh lip contours: outer edge of the lips and the mouth opening.
private val LIP_OUTER = listOf(61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291, 375, 321, 405, 314, 17, 84, 181, 91, 146)
private val LIP_INNER = listOf(78, 191, 80, 81, 82, 13, 312, 311, 310, 415, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95)

/** Virtual try-on on the diagnosis photo: a fabric "cape" under the chin, or lipstick (reward-gated). Tool screen: no ads. */
@Composable
internal fun DrapeRoute(onBack: () -> Unit, vm: DrapeViewModel = hiltViewModel()) {
    TrackScreen("drape")
    val d by vm.diagnosis.collectAsStateWithLifecycle()
    val photo = d?.photo ?: run { LaunchedEffect(Unit) { onBack() }; return }
    val top = vm.content.tone(d!!.top.toneId)
    val others = remember(top) { vm.content.tones.filter { it.id != top.id } }

    // Palette groups. Drape: my best, my worst, then other tones' best. Lip: my lips, worst, then other tones' lips.
    val drapeGroups = remember(top) { listOf("베스트" to top.bestColors, "워스트" to top.worstColors) + others.map { it.nameKo to it.bestColors } }
    val lipGroups = remember(top) {
        listOf("내 톤 추천" to top.makeup.lipColors, "워스트 립" to top.worstColors.take(4)) + others.map { it.nameKo to it.makeup.lipColors }
    }
    var lip by rememberSaveable { mutableStateOf(false) }
    var drapeGroup by rememberSaveable { mutableIntStateOf(0) }
    var lipGroup by rememberSaveable { mutableIntStateOf(0) }
    var drapeColor by remember { mutableStateOf<NamedColor?>(top.bestColors.first()) }
    var lipColor by remember { mutableStateOf<NamedColor?>(top.makeup.lipColors.firstOrNull()) }
    var intensity by rememberSaveable { mutableFloatStateOf(0.65f) }
    val gate = rememberRewardGate()
    val analytics = LocalAnalytics.current

    val groups = if (lip) lipGroups else drapeGroups
    val group = if (lip) lipGroup else drapeGroup
    val selected = if (lip) lipColor else drapeColor
    val select: (NamedColor?) -> Unit = { c ->
        if (lip) {
            lipColor = c
            if (c != null) analytics?.log("lip_tryon", "color" to c.name)
        } else drapeColor = c
    }
    val selectGroup: (Int) -> Unit = { i ->
        if (lip) lipGroup = i else drapeGroup = i
        select(groups[i].second.firstOrNull())
    }

    PcScaffold("가상 체험", onBack) { pad ->
        val wide = isWideWindow()
        val controls: @Composable (Modifier) -> Unit = { m ->
            Column(m.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    SegmentedButton(selected = !lip, onClick = { lip = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                        Text("천 드레이핑", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    SegmentedButton(
                        selected = lip,
                        onClick = {
                            if (!lip) gate(RewardRequest(key = "lip_tryon", title = "광고 보고 체험할까요?",
                                message = "한 번 보면 앱을 닫을 때까지 자유롭게 써요.", action = { lip = true }))
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("립 체험", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Row(Modifier.padding(horizontal = 20.dp).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(selected?.name ?: "원본 사진", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("길게 누르면 원본", style = Muted, maxLines = 1, modifier = Modifier.padding(start = 12.dp))
                }
                Text(
                    if (lip) "안색이 생기 있어 보이는 립이 어울리는 색이에요" else "얼굴이 맑고 환해 보이는 색이 어울리는 색이에요",
                    style = Muted, modifier = Modifier.padding(horizontal = 20.dp),
                )
                val chip: @Composable (Int) -> Unit = { i ->
                    FilterChip(selected = group == i, onClick = { selectGroup(i) }, label = { Text(groups[i].first, maxLines = 1) })
                }
                if (wide) {
                    // Room to wrap: everything visible at once.
                    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { groups.indices.forEach { chip(it) } }
                    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Dot(null, selected == null) { select(null) }
                        groups[group].second.forEach { c -> Dot(c, c == selected) { select(c) } }
                    }
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(groups.size) { chip(it) }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { Dot(null, selected == null) { select(null) } }
                        items(groups[group].second) { c -> Dot(c, c == selected) { select(c) } }
                    }
                }
                if (lip) {
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("연하게", style = Muted, maxLines = 1)
                        Slider(intensity, { intensity = it }, Modifier.weight(1f).padding(horizontal = 12.dp),
                            enabled = selected != null, valueRange = 0.3f..1f)
                        Text("진하게", style = Muted, maxLines = 1)
                    }
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize().padding(pad)) {
                TryOn(photo, lip, selected, intensity, Modifier.weight(1f).fillMaxHeight().padding(16.dp))
                // Centered vertically; scrolls if a short landscape window can't fit it.
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    controls(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding())
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(pad)) {
                TryOn(photo, lip, selected, intensity, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp))
                controls(Modifier.fillMaxWidth().navigationBarsPadding())
            }
        }
    }
}

/** Photo with the cape or lip color on top. Press and hold to see the original. */
@Composable
private fun TryOn(photo: FacePhoto, lip: Boolean, color: NamedColor?, intensity: Float, modifier: Modifier) {
    val image = remember(photo) { photo.bitmap.asImageBitmap() }
    var holding by remember { mutableStateOf(false) }
    Canvas(
        modifier.clip(MaterialTheme.shapes.large).pointerInput(Unit) {
            detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false })
        },
    ) {
        val r = drawFitted(image)
        if (color == null || holding) return@Canvas
        fun pt(i: Int) = photo.point(i, r)
        if (lip) {
            val path = Path().apply {
                smoothLoop(LIP_OUTER.map(::pt)); smoothLoop(LIP_INNER.map(::pt))
                fillType = PathFillType.EvenOdd
            }
            drawLip(path, hexColor(color.hex), intensity, feather = (pt(291) - pt(61)).getDistance() * 0.025f)
        } else drawCape(photo, r, hexColor(color.hex))
    }
}

/** Closed curve through the midpoints of [p], using each landmark as a control point, so the contour has no corners. */
private fun Path.smoothLoop(p: List<Offset>) {
    fun mid(i: Int) = (p[i % p.size] + p[(i + 1) % p.size]) / 2f
    moveTo(mid(0).x, mid(0).y)
    for (i in 1..p.size) p[i % p.size].let { c -> mid(i).let { e -> quadraticTo(c.x, c.y, e.x, e.y) } }
    close()
}

/**
 * Lipstick that keeps the lip texture: a hue/saturation pass (Color blend keeps the photo's luminance, so highlights and
 * creases stay) plus a light multiply for depth, both with a blurred edge so it fades into the skin.
 */
private fun DrawScope.drawLip(path: Path, color: Color, intensity: Float, feather: Float) {
    // Color/Multiply blends need API 29 (pre-Q MULTIPLY is PorterDuff modulate, which eats alpha); Overlay alone below that.
    val passes = if (Build.VERSION.SDK_INT >= 29) listOf(BlendMode.Color to 0.85f, BlendMode.Multiply to 0.35f) else listOf(BlendMode.Overlay to 0.9f)
    // ponytail: BlurMaskFilter is ignored on hardware canvases before API 28, edge is just un-feathered there.
    val blur = BlurMaskFilter(feather.coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
    drawIntoCanvas { canvas ->
        passes.forEach { (mode, a) ->
            val paint = Paint().apply {
                this.color = color.copy(alpha = intensity * a)
                blendMode = mode
                asFrameworkPaint().apply { isAntiAlias = true; maskFilter = blur }
            }
            canvas.drawPath(path, paint)
        }
    }
}

/** Color dot; null = "no cape" (original photo), drawn as a split dot. */
@Composable
private fun Dot(c: NamedColor?, selected: Boolean, onClick: () -> Unit) {
    val m = Modifier.size(48.dp).clip(CircleShape)
        .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
        .clickable(onClick = onClick)
    if (c == null) {
        Box(m.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.HideImage, "원본 사진", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else Swatch(c.hex, m, CircleShape)
}
