package com.gun.personalcolor.feature.colormatch

import com.gun.personalcolor.core.designsystem.PropTile
import com.gun.personalcolor.core.designsystem.TintyProp
import androidx.compose.material3.Surface
import androidx.compose.foundation.horizontalScroll
import com.gun.personalcolor.core.ui.ClothingIllustration
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.colors
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.foundation.layout.height
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyLoading
import com.gun.personalcolor.core.ui.tintyColors
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.hexColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/** Full-screen "옷 추가" shown over 내 옷장 (no separate route). */
@Composable
internal fun ClosetAddRoute(onClose: () -> Unit, vm: ClosetAddViewModel = hiltViewModel()) {
    TrackScreen("closet_add")
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    LaunchedEffect(Unit) { vm.reset() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            analytics?.log("closet_add", "mode" to state.mode.name, "kind" to state.kind.orEmpty(), "verdict" to state.verdict?.level?.name.orEmpty())
            onClose()
        }
    }
    BackHandler(onBack = onClose)

    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::loadPhoto) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(vm::loadPhoto) }

    ClosetAddScreen(
        state = state,
        onClose = onClose,
        onMode = vm::setMode,
        onCamera = {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(context.cacheDir, "closet_photo.jpg"))
            cameraUri = uri
            try { camera.launch(uri) } catch (_: ActivityNotFoundException) { vm.showError("카메라 앱이 없어요. 앨범에서 골라 주세요.") }
        },
        onAlbum = { album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onPickPoint = vm::pickPoint,
        onAutoColor = vm::autoColor,
        onHsv = vm::setHsv,
        onHex = vm::setManualHex,
        onKind = vm::setKind,
        onName = vm::setName,
        onSave = vm::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClosetAddScreen(
    state: ClosetAddUiState,
    onClose: () -> Unit,
    onMode: (AddMode) -> Unit,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
    onPickPoint: (Float, Float) -> Unit,
    onAutoColor: () -> Unit,
    onHsv: (List<Float>) -> Unit,
    onHex: (String) -> Unit,
    onKind: (String) -> Unit,
    onName: (String) -> Unit,
    onSave: () -> Unit,
) {
    PcScaffold("옷 추가", onClose, bottomBar = {
        BottomActions {
            PcButton("옷장에 저장", onSave, Modifier.fillMaxWidth(), icon = Icons.Rounded.Check, enabled = state.canSave)
            if (state.color != null && state.kind == null)
                Text("어떤 옷인지 골라 주세요", style = Muted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }) { pad ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).readableWidth()
                .padding(horizontal = ScreenPadding).padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(AddMode.PHOTO to "사진으로 추가", AddMode.MANUAL to "직접 고르기").forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = state.mode == m, onClick = { onMode(m) }, shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            if (state.mode == AddMode.PHOTO) PhotoPart(state, onCamera, onAlbum, onPickPoint, onAutoColor)
            else ManualPart(state, onHsv, onHex)
            state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

            val color = state.color
            if (color != null && !state.processing) {
                SectionCard("어떤 옷인가요?") {
                    KindChips(state.kind, onKind)
                    OutlinedTextField(
                        value = state.name, onValueChange = onName, label = { Text("이름") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                VerdictCard(color.toHex(), state)
            }
        }
    }
}

@Composable
private fun PhotoPart(state: ClosetAddUiState, onCamera: () -> Unit, onAlbum: () -> Unit, onPickPoint: (Float, Float) -> Unit, onAutoColor: () -> Unit) {
    val photo = state.photo
    Box(
        Modifier.fillMaxWidth().aspectRatio(if (isWideWindow()) 1.6f else 1f).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        Alignment.Center,
    ) {
        when {
            state.processing -> TintyLoading(
                Modifier.padding(16.dp), "배경을 지우고 있어요\n처음엔 조금 걸릴 수 있어요",
                remember(state.tone) { state.tone?.tintyColors ?: TintyColors.Brand }, size = 112.dp, prop = TintyProp.Hanger,
            )
            photo != null -> CutoutPicker(photo, state.point, state.photoColor, onPickPoint)
            else -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropTile(TintyProp.Hanger, remember(state.tone) { state.tone?.tintyColors ?: TintyColors.Brand }, size = 72.dp)
                Spacer(Modifier.height(4.dp))
                Text("내 옷을 찍어 보세요", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("밝은 곳에서 옷 전체가 나오게 찍으면\n배경을 깔끔하게 지워 줘요", style = Muted, textAlign = TextAlign.Center)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (photo == null) PcButton("사진 찍기", onCamera, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera, enabled = !state.processing)
        else PcOutlinedButton("다시 찍기", onCamera, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera, enabled = !state.processing)
        PcOutlinedButton("앨범", onAlbum, Modifier.weight(1f), icon = Icons.Rounded.PhotoLibrary, enabled = !state.processing)
    }
    if (photo != null && !state.processing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    state.cutFailed -> "배경을 지우지 못했어요. 옷을 눌러 색을 골라 주세요"
                    state.point == null -> "색이 다르면 옷을 눌러 골라 주세요"
                    else -> "누른 곳의 색이에요"
                },
                style = Muted, modifier = Modifier.weight(1f),
            )
            if (state.point != null) TextButton(onAutoColor) {
                Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("자동", maxLines = 1)
            }
        }
    }
}

/** Cut-out on a light checkerboard; tap to pick a color point. */
@Composable
private fun CutoutPicker(photo: Bitmap, point: Pair<Float, Float>?, picked: com.gun.personalcolor.core.analysis.Rgb?, onPick: (Float, Float) -> Unit) {
    val image = remember(photo) { photo.asImageBitmap() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth.toFloat(); val boxH = constraints.maxHeight.toFloat()
        val inset = 16f * (boxW / maxWidth.value).coerceAtLeast(1f)
        val scale = min((boxW - 2 * inset) / photo.width, (boxH - 2 * inset) / photo.height)
        val dw = photo.width * scale; val dh = photo.height * scale
        val left = (boxW - dw) / 2; val top = (boxH - dh) / 2
        Canvas(
            Modifier.fillMaxSize().pointerInput(photo, boxW, boxH) {
                detectTapGestures { o -> onPick((o.x - left) / dw, (o.y - top) / dh) }
            },
        ) {
            drawChecker(size)
            drawImage(
                image, dstOffset = androidx.compose.ui.unit.IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = androidx.compose.ui.unit.IntSize(dw.roundToInt(), dh.roundToInt()),
            )
            point?.let { (fx, fy) ->
                val p = Offset(left + fx * dw, top + fy * dh)
                val r = 16.dp.toPx()
                drawCircle(Color.Black.copy(alpha = 0.35f), r + 4.dp.toPx(), p, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White, r + 2.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                drawCircle(picked?.let { Color(it.r, it.g, it.b) } ?: Color.White, r, p, style = Stroke(5.dp.toPx()))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChecker(size: Size) {
    val cell = 12.dp.toPx()
    drawRect(Color(0xFFF7F7F7))
    var y = 0; var py = 0f
    while (py < size.height) {
        var x = 0; var px = 0f
        while (px < size.width) {
            if ((x + y) % 2 == 0) drawRect(Color(0xFFE6E6E6), Offset(px, py), Size(cell, cell))
            x++; px += cell
        }
        y++; py += cell
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualPart(state: ClosetAddUiState, onHsv: (List<Float>) -> Unit, onHex: (String) -> Unit) {
    val hex = state.manualColor.toHex()
    // Placeholder art for the chosen kind + color (same slot as tiles use).
    Box(
        Modifier.fillMaxWidth().aspectRatio(if (isWideWindow()) 3f else 2f).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        Alignment.Center,
    ) { GarmentThumb(ClosetItem(0, hex, "", "", "", com.gun.personalcolor.core.model.Verdict.OK, 0, kind = state.kind), null, Modifier.fillMaxHeight(0.7f).aspectRatio(1f)) }
    state.tone?.let { tone ->
        SectionCard("${tone.nameKo} BEST 컬러") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tone.bestColors.forEach { c ->
                    val selected = c.hex.equals(hex, ignoreCase = true)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(hexColor(c.hex))
                            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(onClickLabel = c.name) { onHex(c.hex) },
                    )
                }
            }
        }
    }
    ManualPicker(state.hsv) { hsv, _ -> onHsv(hsv) }
}

@Composable
private fun VerdictCard(hex: String, state: ClosetAddUiState) {
    SectionCard(null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Swatch(hex, Modifier.size(56.dp), MaterialTheme.shapes.medium)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val v = state.verdict
                if (v != null) {
                    VerdictBadge(v.level)
                    Text(v.level.label, style = MaterialTheme.typography.titleMedium, color = v.level.ds.colors().first, maxLines = 1)
                } else {
                    Text(hex, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("내 톤을 정하면 어울리는지도 알려줘요", style = Muted)
                }
            }
        }
        state.verdict?.let { v ->
            Text(v.reason, style = MaterialTheme.typography.bodyMedium)
            state.tone?.let { Text("${it.nameKo} 기준", style = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

private val KIND_GROUPS = CLOTHING_KINDS.entries.groupBy({ it.value.second }, { it.key })

/** Single-select kind picker: a category row (상의, 하의, 신발 …), then that category's kinds. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KindChips(selected: String?, onSelect: (String) -> Unit) {
    var group by rememberSaveable(selected == null) {
        mutableStateOf(selected?.let { CLOTHING_KINDS[it]?.second } ?: KIND_GROUPS.keys.first())
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KIND_GROUPS.keys.forEach { g ->
                val on = g == group
                Surface(
                    onClick = { group = g }, shape = RoundedCornerShape(50),
                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(g, Modifier.heightIn(min = 36.dp).padding(horizontal = 14.dp, vertical = 8.dp), maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KIND_GROUPS[group].orEmpty().forEach { key ->
                FilterChip(key == selected, { onSelect(key) }, { Text(CLOTHING_KINDS[key]!!.first, maxLines = 1) }, shape = RoundedCornerShape(50))
            }
        }
    }
}

// ---- Garment images ----

/**
 * The garment's picture: the cut-out photo when there is one, else a stand-in for color-only items.
 * Items without a photo show the example illustration of their kind in their color (or a swatch if the kind is unknown).
 */
@Composable
internal fun GarmentThumb(item: ClosetItem, photo: File?, modifier: Modifier = Modifier, maxSide: Int = 512) {
    val bmp = photo?.let { rememberClosetImage(it, maxSide) }
    if (bmp != null) Image(bmp, item.name, modifier, contentScale = ContentScale.Fit)
    else if (item.kind != null) ClothingIllustration(item.kind!!, hexColor(item.hex), modifier)
    else Swatch(item.hex, modifier, MaterialTheme.shapes.small)
}

// ponytail: small in-memory cache so grid scrolling doesn't re-decode; bounded at ~32 MB.
private val imageCache = object : LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
}

@Composable
private fun rememberClosetImage(file: File, maxSide: Int): ImageBitmap? {
    val key = "${file.path}@$maxSide"
    return produceState(imageCache.get(key), key) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, o)
                var sample = 1
                while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= maxSide) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }?.also { imageCache.put(key, it) }
    }.value
}

@Preview(showBackground = true)
@Composable
private fun ClosetAddPreview() = PcTheme {
    ClosetAddScreen(
        ClosetAddUiState(mode = AddMode.MANUAL, kind = "knit", name = "베이지 니트"),
        {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {},
    )
}
