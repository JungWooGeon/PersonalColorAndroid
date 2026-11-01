package com.gun.personalcolor.feature.colormatch

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.SEASON_KO
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import com.gun.personalcolor.core.model.CLOSET_CATEGORIES
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ColorMatchRoute(onTone: (String) -> Unit, onAnalyze: () -> Unit, onTones: () -> Unit, onCloset: () -> Unit, vm: ColorMatchViewModel = hiltViewModel()) {
    TrackScreen("color_match")
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val analytics = LocalAnalytics.current

    // One event per final pick, never per drag frame.
    LaunchedEffect(state.commit) {
        val v = state.verdict ?: return@LaunchedEffect
        if (state.commit > 0) analytics?.log("color_check", "verdict" to v.level.name, "tone" to state.refTone?.id.orEmpty(), "source" to state.source.name)
    }

    val album = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(vm::loadPhoto) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(vm::loadPhoto) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ColorMatchScreen(
        state = state,
        onCamera = {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(context.cacheDir, "color_check.jpg"))
            cameraUri = uri
            try { camera.launch(uri) } catch (_: ActivityNotFoundException) { vm.showError("카메라 앱이 없어요. 앨범에서 골라 주세요.") }
        },
        onAlbum = { album.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onManual = vm::startManual,
        onBackToPhoto = vm::backToPhoto,
        onPick = vm::pick,
        onPickEnd = vm::commit,
        onHsv = vm::setHsv,
        onSetMyTone = vm::setMyTone,
        onSetReferenceTone = vm::setReferenceTone,
        onTone = onTone,
        onAnalyze = onAnalyze,
        onTones = onTones,
        onCloset = onCloset,
        onSaveToCloset = { name, category ->
            vm.saveToCloset(name, category)
            analytics?.log("closet_save", "verdict" to state.verdict?.level?.name.orEmpty(), "category" to category)
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                if (snackbar.showSnackbar("옷장에 저장했어요", actionLabel = "보기", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) onCloset()
            }
        },
        snackbar = snackbar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorMatchScreen(
    state: ColorMatchUiState,
    onCamera: () -> Unit,
    onAlbum: () -> Unit,
    onManual: () -> Unit,
    onBackToPhoto: () -> Unit,
    onPick: (Float, Float, Boolean) -> Unit,
    onPickEnd: () -> Unit,
    onHsv: (List<Float>, Boolean) -> Unit,
    onSetMyTone: (String) -> Unit,
    onSetReferenceTone: (String) -> Unit,
    onTone: (String) -> Unit,
    onAnalyze: () -> Unit,
    onTones: () -> Unit,
    onCloset: () -> Unit,
    onSaveToCloset: (name: String, category: String) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    var saveSheet by rememberSaveable { mutableStateOf(false) }
    // null = closed; true = choose "my tone" (no tone yet); false = switch the reference tone on this screen.
    var sheetForMyTone by remember { mutableStateOf<Boolean?>(null) }
    val openSheet = { sheetForMyTone = state.myToneId == null }

    Box(Modifier.fillMaxSize()) {
    PcScaffold("컬러 체크", actions = {
        IconButton(onCloset) { Icon(Icons.Rounded.Checkroom, "내 옷장") }
    }) { pad ->
        val tone = state.refTone
        when {
            !state.ready -> Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) { CircularProgressIndicator() }
            tone == null -> NoToneState(pad, onAnalyze, onPickTone = openSheet, onTones = onTones)
            state.source == ColorSource.NONE -> Intro(pad, tone, state, onCamera, onAlbum, onManual, openSheet)
            else -> Checker(pad, tone, state, onCamera, onAlbum, onManual, onBackToPhoto, onPick, onPickEnd, onHsv, openSheet, onTone) { saveSheet = true }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    val v = state.verdict
    val c = state.color
    if (saveSheet && v != null && c != null) {
        SaveToClosetSheet(c.toHex(), v, onSave = { name, category -> onSaveToCloset(name, category); saveSheet = false }, onDismiss = { saveSheet = false })
    }

    sheetForMyTone?.let { forMyTone ->
        ToneSheet(
            tones = state.tones,
            selectedId = state.refTone?.id,
            myToneId = state.myToneId,
            title = if (forMyTone) "내 톤 고르기" else "기준 톤 고르기",
            onSelect = { id -> if (forMyTone) onSetMyTone(id) else onSetReferenceTone(id); sheetForMyTone = null },
            onDismiss = { sheetForMyTone = null },
        )
    }
}

// ---- States ----

@Composable
private fun NoToneState(pad: PaddingValues, onAnalyze: () -> Unit, onPickTone: () -> Unit, onTones: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
        EmptyState(
            icon = Icons.Rounded.Palette,
            title = "먼저 내 톤을 알려주세요",
            message = "내 톤에 어울리는지 알려줘요.",
            action = {
                Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PcButton("사진으로 진단하기", onAnalyze, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                    PcOutlinedButton("내 톤 직접 고르기", onPickTone, Modifier.fillMaxWidth(), icon = Icons.Rounded.Palette)
                    TextButton(onTones, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("12가지 톤 알아보기", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            },
        )
    }
}

@Composable
private fun Intro(
    pad: PaddingValues, tone: Tone, state: ColorMatchUiState,
    onCamera: () -> Unit, onAlbum: () -> Unit, onManual: () -> Unit, onToneChip: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)) {
        Column(
            Modifier.readableWidth().padding(horizontal = ScreenPadding).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReferenceToneChip(tone, onToneChip)
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                        tone.bestColors.take(5).forEach {
                            Swatch(it.hex, Modifier.size(40.dp).border(3.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape), CircleShape)
                        }
                    }
                    Text("이 색, 나한테 어울릴까?", style = MaterialTheme.typography.headlineSmall)
                    Text("${tone.nameKo}에 어울리는지 바로 알려줘요.", style = Muted)
                    Step(Icons.Rounded.PhotoCamera, "옷이나 립스틱 찍기")
                    Step(Icons.Rounded.TouchApp, "확인할 곳 누르기")
                    Step(Icons.Rounded.Verified, "어울림 결과 확인")
                }
            }
            state.error?.let { ErrorText(it) }
            if (state.photoLoading) {
                Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("사진 불러오는 중", style = Muted)
                }
            } else {
                PcButton("카메라로 찍기", onCamera, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                PcOutlinedButton("앨범에서 선택", onAlbum, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoLibrary)
                TextButton(onManual, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.Colorize, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("직접 색 고르기", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun Step(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Checker(
    pad: PaddingValues, tone: Tone, state: ColorMatchUiState,
    onCamera: () -> Unit, onAlbum: () -> Unit, onManual: () -> Unit, onBackToPhoto: () -> Unit,
    onPick: (Float, Float, Boolean) -> Unit, onPickEnd: () -> Unit, onHsv: (List<Float>, Boolean) -> Unit,
    onToneChip: () -> Unit, onTone: (String) -> Unit, onSave: () -> Unit,
) {
    val sources: @Composable () -> Unit = {
        SourceChips(state, onCamera, onAlbum, onManual, onBackToPhoto)
        state.error?.let { ErrorText(it) }
    }
    val result: @Composable () -> Unit = {
        val v = state.verdict
        val c = state.color
        if (v != null && c != null) Result(c, v, state.myToneId, onTone, onSave)
    }
    if (isWideWindow()) {
        Row(Modifier.fillMaxSize().padding(pad).padding(horizontal = ScreenPadding), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.source == ColorSource.MANUAL) Column(Modifier.verticalScroll(rememberScrollState())) { ManualPicker(state.hsv, onHsv) }
                else PhotoArea(state, onPick, onPickEnd, Modifier.weight(1f).fillMaxWidth())
                sources()
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReferenceToneChip(tone, onToneChip)
                result()
            }
        }
    } else {
        val maxPhoto = with(LocalDensity.current) { (LocalWindowInfo.current.containerSize.height * 0.45f).toDp() }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)) {
            Column(Modifier.readableWidth().padding(horizontal = ScreenPadding).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReferenceToneChip(tone, onToneChip)
                if (state.source == ColorSource.MANUAL) ManualPicker(state.hsv, onHsv)
                else {
                    val photo = state.photo
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val natural = if (photo == null) 240.dp else maxWidth * (photo.height.toFloat() / photo.width)
                        PhotoArea(state, onPick, onPickEnd, Modifier.fillMaxWidth().height(minOf(natural, maxPhoto)))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { sources() }
                result()
            }
        }
    }
}

@Composable
private fun SourceChips(state: ColorMatchUiState, onCamera: () -> Unit, onAlbum: () -> Unit, onManual: () -> Unit, onBackToPhoto: () -> Unit) {
    if (state.source == ColorSource.PHOTO) Text("누르거나 끌어서 색을 골라요", style = Muted)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onCamera, { Text("카메라", maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp)) })
        AssistChip(onAlbum, { Text("앨범", maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp)) })
        if (state.source == ColorSource.MANUAL && state.photo != null)
            AssistChip(onBackToPhoto, { Text("사진에서 고르기", maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)) })
        if (state.source == ColorSource.PHOTO)
            AssistChip(onManual, { Text("직접 색 고르기", maxLines = 1) }, leadingIcon = { Icon(Icons.Rounded.Colorize, null, Modifier.size(18.dp)) })
    }
}

@Composable
private fun ReferenceToneChip(tone: Tone, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text("기준 톤: ${tone.nameKo}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { ToneAvatar(tone, 18.dp) },
        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, "기준 톤 바꾸기", Modifier.size(18.dp)) },
    )
}

@Composable
private fun ErrorText(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

// ---- Photo picker ----

@Composable
private fun PhotoArea(state: ColorMatchUiState, onPick: (Float, Float, Boolean) -> Unit, onPickEnd: () -> Unit, modifier: Modifier) {
    val photo = state.photo
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerHighest), Alignment.Center) {
        if (photo != null) PhotoPicker(photo, state.pointX, state.pointY, state.color, onPick, onPickEnd)
        if (state.photoLoading) CircularProgressIndicator()
    }
}

@Composable
private fun PhotoPicker(photo: Bitmap, fx: Float, fy: Float, picked: Rgb?, onPick: (Float, Float, Boolean) -> Unit, onPickEnd: () -> Unit) {
    val image = remember(photo) { photo.asImageBitmap() }
    val pick by rememberUpdatedState(onPick)
    val end by rememberUpdatedState(onPickEnd)
    var dragging by remember { mutableStateOf(false) }
    val ringColor = picked?.let { Color(it.r, it.g, it.b) } ?: Color.White
    val sampleR = remember(photo) { sampleRadius(photo) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        // Fit (letterbox) the photo and remember where it lands so touches map to bitmap pixels.
        val scale = min(boxW / photo.width, boxH / photo.height)
        val dw = photo.width * scale
        val dh = photo.height * scale
        val left = (boxW - dw) / 2
        val top = (boxH - dh) / 2
        val toPhoto = { o: Offset -> ((o.x - left) / dw) to ((o.y - top) / dh) }

        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(photo, boxW, boxH) { detectTapGestures { val (x, y) = toPhoto(it); pick(x, y, true) } }
                .pointerInput(photo, boxW, boxH) {
                    detectDragGestures(
                        onDragStart = { dragging = true; val (x, y) = toPhoto(it); pick(x, y, false) },
                        onDragEnd = { dragging = false; end() },
                        onDragCancel = { dragging = false; end() },
                        onDrag = { change, _ -> change.consume(); val (x, y) = toPhoto(change.position); pick(x, y, false) },
                    )
                },
        ) {
            drawImage(
                image, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.Medium,
            )
            val p = Offset(left + fx * dw, top + fy * dh)
            val ring = 18.dp.toPx()
            drawCircle(Color.Black.copy(alpha = 0.35f), ring + 5.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            drawCircle(Color.White, ring + 3.dp.toPx(), p, style = Stroke(2.dp.toPx()))
            drawCircle(ringColor, ring, p, style = Stroke(6.dp.toPx()))
            drawCircle(Color.White, ring - 3.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
            val tick = 5.dp.toPx()
            drawLine(Color.White, p.copy(x = p.x - tick), p.copy(x = p.x + tick), 1.5.dp.toPx())
            drawLine(Color.White, p.copy(y = p.y - tick), p.copy(y = p.y + tick), 1.5.dp.toPx())

            if (dragging) {
                // Magnifier loupe above the finger (below it near the top edge), 3× the on-screen size.
                val r = 48.dp.toPx()
                val gap = 64.dp.toPx()
                val c = Offset(p.x.coerceIn(r, size.width - r), if (p.y - gap - r >= 0) p.y - gap else p.y + gap)
                val zoom = 3f
                val half = (r / zoom / scale).roundToInt().coerceAtLeast(1)
                val srcW = min(2 * half, photo.width)
                val srcH = min(2 * half, photo.height)
                val bx = (fx * (photo.width - 1)).roundToInt()
                val by = (fy * (photo.height - 1)).roundToInt()
                val sx = (bx - half).coerceIn(0, photo.width - srcW)
                val sy = (by - half).coerceIn(0, photo.height - srcH)
                val k = zoom * scale // bitmap px → loupe px
                val dst = Offset(c.x - half * k, c.y - half * k)
                clipPath(Path().apply { addOval(Rect(c, r)) }) {
                    drawCircle(Color.Gray, r, c)
                    drawImage(
                        image, srcOffset = IntOffset(sx, sy), srcSize = IntSize(srcW, srcH),
                        dstOffset = IntOffset(dst.x.roundToInt(), dst.y.roundToInt()),
                        dstSize = IntSize((srcW * k).roundToInt(), (srcH * k).roundToInt()), filterQuality = FilterQuality.None,
                    )
                    val m = Offset(dst.x + (bx - sx + 0.5f) * k, dst.y + (by - sy + 0.5f) * k)
                    drawCircle(Color.White, sampleR * k, m, style = Stroke(2.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.4f), sampleR * k + 1.5.dp.toPx(), m, style = Stroke(1.dp.toPx()))
                }
                drawCircle(ringColor, r, c, style = Stroke(5.dp.toPx()))
                drawCircle(Color.White, r + 2.5.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

// ---- Manual picker ----

@Composable
internal fun ManualPicker(hsv: List<Float>, onHsv: (List<Float>, Boolean) -> Unit) {
    val (h, s, v) = hsv
    fun hsvColor(h: Float, s: Float, v: Float) = Color.hsv(h.coerceIn(0f, 360f), s, v)
    SectionCard("직접 색 고르기") {
        Text("슬라이더로 색을 맞춰요", style = Muted)
        GradientSlider("색상", h, 0f..360f, Brush.horizontalGradient(listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsvColor(it, 1f, 1f) }),
            { onHsv(listOf(it, s, v), false) }) { onHsv(hsv, true) }
        GradientSlider("채도", s, 0f..1f, Brush.horizontalGradient(listOf(hsvColor(h, 0f, v), hsvColor(h, 1f, v))),
            { onHsv(listOf(h, it, v), false) }) { onHsv(hsv, true) }
        GradientSlider("밝기", v, 0f..1f, Brush.horizontalGradient(listOf(Color.Black, hsvColor(h, s, 1f))),
            { onHsv(listOf(h, s, it), false) }) { onHsv(hsv, true) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, brush: Brush, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value, onValueChange = onChange, valueRange = range, onValueChangeFinished = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            thumb = {
                Box(Modifier.size(26.dp).border(2.dp, Color.Black.copy(alpha = 0.25f), CircleShape).padding(2.dp).background(Color.White, CircleShape))
            },
            track = { Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(brush)) },
        )
    }
}

// ---- Result ----

@Composable
private fun Result(color: Rgb, v: ColorVerdict, myToneId: String?, onTone: (String) -> Unit, onSave: () -> Unit) {
    val accent = levelColors(v.level).accent
    val hex = color.toHex()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(hex, Modifier.size(88.dp), RoundedCornerShape(20.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerdictBadge(v.level)
                    Text(hex, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }
            Text(v.reason, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("어울림 점수", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("${v.score}점", style = MaterialTheme.typography.titleSmall, color = accent)
            }
            LinearProgressIndicator(
                progress = { v.score / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
            )
            PcOutlinedButton("옷장에 저장", onSave, Modifier.fillMaxWidth(), icon = Icons.Rounded.BookmarkAdd)
        }

        SectionCard("비슷한 BEST 컬러") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                v.nearestBest.forEach { c ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Swatch(c.hex, Modifier.fillMaxWidth().height(56.dp))
                        Text(c.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        SectionCard("이 색이 어울리는 톤") {
            Column {
                v.topTones.forEachIndexed { i, m -> ToneRow(i + 1, m.tone, m.tone.id == myToneId) { onTone(m.tone.id) } }
            }
        }

        Row(Modifier.padding(horizontal = 4.dp)) {
            Icon(Icons.Outlined.Info, null, Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text("조명에 따라 실제 색과 다를 수 있어요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToneRow(rank: Int, tone: Tone, isMine: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 56.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$rank", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
            ToneAvatar(tone, 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tone.nameKo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tone.seasonKo} · ${tone.temperatureKo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (isMine) { Tag("내 톤", color = MaterialTheme.colorScheme.primaryContainer); Spacer(Modifier.width(4.dp)) }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal data class LevelColors(val container: Color, val onContainer: Color, val accent: Color)

@Composable
internal fun levelColors(level: MatchLevel): LevelColors {
    val cs = MaterialTheme.colorScheme
    return when (level) {
        MatchLevel.GOOD -> LevelColors(cs.tertiaryContainer, cs.onTertiaryContainer, cs.tertiary)
        MatchLevel.NEUTRAL -> LevelColors(cs.secondaryContainer, cs.onSecondaryContainer, cs.primary)
        MatchLevel.AVOID -> LevelColors(cs.errorContainer, cs.onErrorContainer, cs.error)
    }
}

/** Colored pill with the verdict; [small] for grid tiles. */
@Composable
internal fun VerdictBadge(level: MatchLevel, modifier: Modifier = Modifier, small: Boolean = false, text: String = level.label) {
    val c = levelColors(level)
    val icon = when (level) { MatchLevel.GOOD -> Icons.Rounded.Check; MatchLevel.NEUTRAL -> Icons.Rounded.HorizontalRule; MatchLevel.AVOID -> Icons.Rounded.Close }
    Surface(modifier, shape = RoundedCornerShape(50), color = c.container, contentColor = c.onContainer) {
        Row(
            if (small) Modifier.padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp) else Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(if (small) 14.dp else 18.dp))
            Spacer(Modifier.width(if (small) 4.dp else 6.dp))
            Text(text, style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveToClosetSheet(hex: String, v: ColorVerdict, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var category by rememberSaveable { mutableStateOf(CLOSET_CATEGORIES.first()) }
    var name by rememberSaveable { mutableStateOf("${v.nameHint ?: hex} $category") }
    var edited by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.readableWidth().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("옷장에 저장", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(hex, Modifier.size(56.dp), RoundedCornerShape(16.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerdictBadge(v.level)
                    Text("$hex · ${v.score}점", style = Muted, maxLines = 1)
                }
            }
            ClosetFields(
                name = name, onName = { name = it; edited = true },
                category = category,
                onCategory = { new -> if (!edited) name = "${v.nameHint ?: hex} $new"; category = new },
            )
            PcButton("저장", { onSave(name.ifBlank { "${v.nameHint ?: hex} $category" }, category) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.BookmarkAdd)
            Text("사진 없이 색만 저장돼요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Tone sheet ----

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ToneSheet(tones: List<Tone>, selectedId: String?, myToneId: String?, title: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.readableWidth().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            listOf("spring", "summer", "autumn", "winter").forEach { season ->
                val group = tones.filter { it.season == season }
                if (group.isEmpty()) return@forEach
                Text("${SEASON_KO[season]} · ${group.first().temperatureKo}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.forEach { t ->
                        FilterChip(
                            selected = t.id == selectedId,
                            onClick = { onSelect(t.id) },
                            label = { Text(if (t.id == myToneId) "${t.nameKo} (내 톤)" else t.nameKo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { ToneAvatar(t, 18.dp) },
                        )
                    }
                }
            }
        }
    }
}

// ---- Preview ----

private val previewTone = Tone(
    "autumn_mute", "autumn", "가을 뮤트", "Autumn Muted", emptyList(), "", "",
    listOf(NamedColor("테라코타", "#C0603A"), NamedColor("올리브", "#8A7650"), NamedColor("모카", "#6B4E3D")),
    listOf(NamedColor("핫핑크", "#FF2D95")),
    Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""), Hair(emptyList(), ""), Fashion("", emptyList()), Accessory("", ""), "",
)

@Preview(showBackground = true)
@Composable
private fun ColorMatchManualPreview() = PcTheme {
    val color = Rgb(194, 98, 60)
    ColorMatchScreen(
        state = ColorMatchUiState(
            ready = true, tones = listOf(previewTone), myToneId = previewTone.id, refTone = previewTone,
            source = ColorSource.MANUAL, color = color, verdict = ColorMatcher.evaluate(color, previewTone, listOf(previewTone)),
        ),
        onCamera = {}, onAlbum = {}, onManual = {}, onBackToPhoto = {}, onPick = { _, _, _ -> }, onPickEnd = {}, onHsv = { _, _ -> },
        onSetMyTone = {}, onSetReferenceTone = {}, onTone = {}, onAnalyze = {}, onTones = {}, onCloset = {}, onSaveToCloset = { _, _ -> },
    )
}
