package com.gun.personalcolor.feature.my

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcSnackbarHost
import com.gun.personalcolor.core.designsystem.elevation
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.WallpaperRenderer
import com.gun.personalcolor.core.ui.WallpaperRenderer.Style
import com.gun.personalcolor.core.ui.contentColorOn
import com.gun.personalcolor.core.ui.hexColor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WallpaperUiState(
    val tones: List<Tone> = emptyList(),
    val tone: Tone? = null,
    val style: Style = Style.TINTY,
    val dark: Boolean = false,
    /** The user's own tone; daily auto wallpaper needs it. */
    val myTone: Tone? = null,
    val autoEnabled: Boolean = false,
    val autoTarget: WallpaperTarget = WallpaperTarget.BOTH,
    /** "움직이는 배경화면" mode. */
    val live: Boolean = false,
) {
    val showDark get() = dark && style.supportsDark
    /** 틴티 first: it's the brand look; the rest keep their order. */
    val styles get() = Style.entries.sortedBy { it != Style.TINTY }.filter { !live || it.animated }
}

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(WallpaperUiState(tones = content.tones))
    val state = _state.asStateFlow()

    init {
        val routeToneId = savedState.toRoute<WallpaperRoute>().toneId
        viewModelScope.launch {
            // Route tone → my tone → first tone. Open on the look currently in use (live, or daily auto).
            val p = prefs.preferences.first()
            val live = isLiveWallpaperActive(context)
            val choice = LiveWallpaperPrefs.read(context)
            val (style, dark) = when {
                live -> choice.style to choice.dark
                p.autoWallpaper -> decodeStyle(p.autoWallpaperStyle)
                else -> Style.TINTY to false
            }
            val tone = content.toneOrNull(routeToneId ?: choice.toneId.takeIf { live } ?: p.myToneId) ?: content.tones.firstOrNull()
            _state.update { it.copy(tone = tone, style = style, dark = dark, live = live) }
        }
        viewModelScope.launch {
            prefs.preferences.collect { p ->
                _state.update {
                    it.copy(
                        myTone = content.toneOrNull(p.myToneId),
                        autoEnabled = p.autoWallpaper,
                        autoTarget = WallpaperTarget.entries.firstOrNull { t -> t.name == p.autoWallpaperTarget } ?: WallpaperTarget.BOTH,
                    )
                }
            }
        }
    }

    fun select(tone: Tone) = _state.update { it.copy(tone = tone) }
    fun setLive(live: Boolean) = _state.update { it.copy(live = live, style = if (live && !it.style.animated) Style.TINTY else it.style) }

    /** Saves the look for the live wallpaper service; the daily photo wallpaper turns off (it would replace the live one). */
    internal fun saveLive(): LiveChoice {
        val s = _state.value
        val choice = LiveChoice(s.style, s.showDark, s.tone?.id?.takeIf { it != s.myTone?.id })
        LiveWallpaperPrefs.write(context, choice)
        if (s.autoEnabled) viewModelScope.launch { prefs.setAutoWallpaper(false, encodeStyle(s.style, s.dark), s.autoTarget.name) }
        return choice
    }
    fun setStyle(style: Style) = change { it.copy(style = style) }
    fun setDark(dark: Boolean) = change { it.copy(dark = dark) }
    fun setAuto(enabled: Boolean, target: WallpaperTarget) = viewModelScope.launch {
        val s = _state.value
        prefs.setAutoWallpaper(enabled, encodeStyle(s.style, s.dark), target.name)
    }

    /** The daily wallpaper follows the chosen look from the next change on. */
    private fun change(block: (WallpaperUiState) -> WallpaperUiState) {
        _state.update(block)
        val s = _state.value
        if (s.autoEnabled && !s.live) viewModelScope.launch { prefs.setAutoWallpaper(true, encodeStyle(s.style, s.dark), s.autoTarget.name) }
    }
}

internal val Style.label get() = when (this) {
    Style.AURORA -> "오로라"
    Style.GLOW -> "글로우"
    Style.WAVE -> "웨이브"
    Style.PALETTE -> "팔레트"
    Style.MINIMAL -> "미니멀"
    Style.BOKEH -> "보케"
    Style.TINTY -> "틴티"
}

@Composable
private fun screenSize(): IntSize {
    val context = LocalContext.current
    return remember(context) { screenSizePx(context).let { (w, h) -> IntSize(w, h) } }
}

@Composable
internal fun WallpaperRoute(onBack: () -> Unit, vm: WallpaperViewModel = hiltViewModel()) {
    TrackScreen("wallpaper")
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val saveImage = rememberImageSave(snackbar)
    val size = screenSize()
    var busy by remember { mutableStateOf(false) }

    fun setAuto(enabled: Boolean, target: WallpaperTarget) {
        val my = state.myTone ?: return
        if (!enabled) {
            vm.setAuto(false, target)
            if (!isLiveWallpaperActive(context)) AutoWallpaperScheduler.cancel(context) // live mode still refreshes the lock screen
            analytics?.log("auto_wallpaper", "on" to "false")
            return
        }
        busy = true
        scope.launch {
            val ok = AutoWallpaperScheduler.apply(context, my, state.style, state.showDark, target)
            busy = false
            if (ok) {
                vm.setAuto(true, target)
                AutoWallpaperScheduler.schedule(context)
                analytics?.log("auto_wallpaper", "on" to "true", "style" to state.style.name.lowercase(), "target" to target.name.lowercase())
                snackbar.showSnackbar("오늘의 배경화면으로 바꿨어요. 내일부터 매일 바뀌어요")
            } else {
                snackbar.showSnackbar(
                    if (canSetWallpaper(context)) "배경화면을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요"
                    else "이 기기에선 배경화면을 바꿀 수 없어요"
                )
            }
        }
    }

    // After the system preview: if ours is now the wallpaper, give the lock screen the matching still and keep it fresh daily.
    fun finishLive(tone: Tone, choice: LiveChoice) {
        busy = true
        scope.launch {
            val ok = setLiveLockFrame(context, tone, choice)
            AutoWallpaperScheduler.schedule(context)
            refreshWidgets(context)
            busy = false
            analytics?.log("live_wallpaper_set", "style" to choice.style.name.lowercase(), "lock" to ok.toString())
            snackbar.showSnackbar(if (ok) "움직이는 배경화면을 적용했어요" else "적용했어요. 잠금 화면은 바꾸지 못했어요")
        }
    }
    var pending by remember { mutableStateOf<Pair<Tone, LiveChoice>?>(null) }
    val liveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val (tone, choice) = pending ?: return@rememberLauncherForActivityResult
        pending = null
        if (isLiveWallpaperActive(context)) finishLive(tone, choice) // otherwise the user backed out
    }

    WallpaperScreen(
        state = state, screen = size, snackbar = snackbar, busy = busy, onBack = onBack,
        onStyle = vm::setStyle, onTone = vm::select, onDark = vm::setDark, onLive = vm::setLive,
        onApplyLive = live@{
            val tone = state.tone ?: return@live
            val choice = vm.saveLive()
            if (isLiveWallpaperActive(context)) return@live finishLive(tone, choice) // already ours: it redraws with the new look
            pending = tone to choice
            val launched = listOf(liveWallpaperIntent(context), Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)).any {
                try { liveLauncher.launch(it); true } catch (e: ActivityNotFoundException) { false }
            }
            if (!launched) {
                pending = null
                scope.launch { snackbar.showSnackbar("이 기기에선 움직이는 배경화면을 쓸 수 없어요") }
            }
        },
        onAuto = { setAuto(it, state.autoTarget) },
        onTarget = { t -> if (state.autoEnabled) setAuto(true, t) else vm.setAuto(false, t) },
        onSave = save@{
            val s = state; val tone = s.tone ?: return@save
            analytics?.log("wallpaper_save", "style" to s.style.name.lowercase(), "tone" to tone.id)
            saveImage("personal-color-wallpaper") { WallpaperRenderer.render(tone, s.style, size.width, size.height, s.showDark) }
        },
        onApply = apply@{ target ->
            val s = state; val tone = s.tone ?: return@apply
            busy = true
            scope.launch {
                val ok = setWallpaper(context, target) { w, h -> WallpaperRenderer.render(tone, s.style, w, h, s.showDark) }
                busy = false
                analytics?.log("wallpaper_set", "style" to s.style.name.lowercase(), "target" to target.name.lowercase(), "ok" to ok.toString())
                snackbar.showSnackbar(if (ok) "배경화면으로 설정했어요" else "설정하지 못했어요. 저장한 뒤 직접 설정해 주세요")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WallpaperScreen(
    state: WallpaperUiState,
    screen: IntSize,
    snackbar: SnackbarHostState,
    busy: Boolean,
    onBack: () -> Unit,
    onStyle: (Style) -> Unit,
    onTone: (Tone) -> Unit,
    onDark: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onTarget: (WallpaperTarget) -> Unit,
    onSave: () -> Unit,
    onApply: (WallpaperTarget) -> Unit,
    onLive: (Boolean) -> Unit = {},
    onApplyLive: () -> Unit = {},
) {
    var picking by remember { mutableStateOf(false) }
    PcScaffold(
        "배경화면 만들기", onBack,
        bottomBar = {
            BottomActions {
                if (state.live) {
                    PcButton("움직이는 배경화면 적용", onApplyLive, Modifier.fillMaxWidth(), enabled = state.tone != null && !busy)
                } else {
                    ButtonPair(stackBelow = 320.dp,
                        first = { PcOutlinedButton("저장", onSave, it, enabled = state.tone != null) },
                        second = { PcButton("배경화면으로 설정", { picking = true }, it, enabled = state.tone != null && !busy) })
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            val ratio = screen.width / screen.height.toFloat()
            if (isWideWindow()) {
                Row(
                    Modifier.fillMaxSize().padding(pad).readableWidth(1040.dp).padding(horizontal = ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        PhonePreview(state, ratio, Modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        ModeSwitch(state.live, onLive)
                        Options(state, ratio, busy, onStyle, onTone, onDark, onAuto, onTarget)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).readableWidth()
                        .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ModeSwitch(state.live, onLive)
                    PhonePreview(state, ratio, Modifier.widthIn(max = 260.dp).fillMaxWidth(0.66f).aspectRatio(ratio))
                    Options(state, ratio, busy, onStyle, onTone, onDark, onAuto, onTarget)
                }
            }
            PcSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = pad.calculateBottomPadding()).padding(16.dp))
        }
    }
    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
                Text("어디에 설정할까요?", style = MaterialTheme.typography.titleLarge, maxLines = 1,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                WallpaperTarget.entries.forEach { t ->
                    val icon = when (t) {
                        WallpaperTarget.HOME -> Icons.Outlined.Home
                        WallpaperTarget.LOCK -> Icons.Outlined.Lock
                        WallpaperTarget.BOTH -> Icons.Outlined.Wallpaper
                    }
                    ListRow(t.label, icon = icon, onClick = { picking = false; onApply(t) })
                }
            }
        }
    }
}

/** Renders [style] small, off the main thread. */
@Composable
private fun rememberRender(tone: Tone?, style: Style, dark: Boolean, width: Int, ratio: Float): ImageBitmap? =
    produceState<ImageBitmap?>(null, tone, style, dark, width, ratio) {
        if (tone == null) return@produceState
        value = withContext(Dispatchers.Default) { WallpaperRenderer.render(tone, style, width, (width / ratio).toInt(), dark).asImageBitmap() }
    }.value

/** Live render inside a phone-shaped frame, with a mock clock showing the calm area. */
@Composable
private fun PhonePreview(state: WallpaperUiState, ratio: Float, modifier: Modifier) {
    val tone = state.tone
    val image = rememberRender(tone, state.style, state.showDark, 480, ratio)
    val clock = when {
        state.showDark -> Color.White
        tone != null && (state.style == Style.PALETTE || state.style == Style.MINIMAL) ->
            contentColorOn(hexColor(WallpaperRenderer.todayColor(tone).hex))
        else -> Color(0xFF1D1B1A)
    }
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.elevation(shape, PcTheme.colors.isDark, 2).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        .border(5.dp, Color(0xFF1C1C1E), shape)) {
        if (state.live && tone != null) LiveArt(tone, state.style, state.showDark, Modifier.fillMaxSize())
        else image?.let { Image(it, contentDescription = "배경화면 미리보기", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text("9:41", fontSize = 40.sp, fontWeight = FontWeight.Light, color = clock, maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp))
    }
}

/** The live wallpaper's own drawing code, running in Compose. Only the draw phase re-runs each frame. */
@Composable
private fun LiveArt(tone: Tone, style: Style, dark: Boolean, modifier: Modifier) {
    val time = produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1e9f }
    }
    val live = remember { WallpaperRenderer.Live() }
    Canvas(modifier) {
        val t = time.value
        drawIntoCanvas { WallpaperRenderer.draw(it.nativeCanvas, tone, style, size.width.toInt(), size.height.toInt(), dark, t = t, live = live) }
    }
}

@Composable
private fun ModeSwitch(live: Boolean, onLive: (Boolean) -> Unit) {
    val narrow = LocalConfiguration.current.screenWidthDp < 360
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(true to if (narrow) "움직이는" else "움직이는 배경화면", false to if (narrow) "사진" else "사진 배경화면").forEachIndexed { i, (value, label) ->
            SegmentedButton(selected = live == value, onClick = { onLive(value) }, shape = SegmentedButtonDefaults.itemShape(i, 2), icon = {}) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StyleThumb(tone: Tone?, style: Style, dark: Boolean, ratio: Float, selected: Boolean, onClick: () -> Unit) {
    val image = rememberRender(tone, style, dark && style.supportsDark, 140, ratio)
    val shape = MaterialTheme.shapes.small
    val cs = MaterialTheme.colorScheme
    Column(Modifier.width(72.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // Selected: violet ring with a small gap, like a focus ring.
        Box(Modifier.border(2.dp, if (selected) cs.primary else Color.Transparent, RoundedCornerShape(15.dp)).padding(3.dp)) {
            Box(
                Modifier.width(56.dp).aspectRatio(ratio).clip(shape).background(cs.surfaceVariant).border(1.dp, cs.outlineVariant, shape),
            ) {
                image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(style.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) cs.primary else cs.onSurfaceVariant)
    }
}

@Composable
private fun Options(
    state: WallpaperUiState,
    ratio: Float,
    busy: Boolean,
    onStyle: (Style) -> Unit,
    onTone: (Tone) -> Unit,
    onDark: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onTarget: (WallpaperTarget) -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Option("스타일") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.styles) { s -> StyleThumb(state.tone, s, state.dark, ratio, state.style == s) { onStyle(s) } }
        }
    }
    if (state.style.supportsDark) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("라이트", "다크").forEachIndexed { i, label ->
                SegmentedButton(selected = state.dark == (i == 1), onClick = { onDark(i == 1) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
    Option("톤") {
        val start = state.tones.indexOf(state.tone).coerceAtLeast(0)
        LazyRow(state = rememberLazyListState(start), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.tones, key = { it.id }) { t ->
                FilterChip(
                    selected = state.tone?.id == t.id, onClick = { onTone(t) },
                    label = { Text(t.nameKo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { ToneAvatar(t, 18.dp) },
                )
            }
        }
    }
    if (state.live) LiveInfoCard() else AutoWallpaperCard(state, busy, onAuto, onTarget)
}

@Composable
private fun LiveInfoCard() = PcCard(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(Icons.Outlined.Lock, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("매일 오늘의 컬러로 바뀌어요", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("잠금 화면에는 같은 디자인이 사진으로 적용돼요", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AutoWallpaperCard(state: WallpaperUiState, busy: Boolean, onAuto: (Boolean) -> Unit, onTarget: (WallpaperTarget) -> Unit) {
    val available = state.myTone != null && !busy
    PcCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Outlined.Autorenew, size = 40.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("매일 자동으로 바꾸기", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (state.myTone != null) "매일 오늘의 컬러로 새 배경화면을 만들어요" else "내 톤을 먼저 설정하면 쓸 수 있어요",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = state.autoEnabled, onCheckedChange = { onAuto(it) }, enabled = available)
            }
            if (state.myTone != null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    WallpaperTarget.entries.forEachIndexed { i, t ->
                        SegmentedButton(selected = state.autoTarget == t, onClick = { onTarget(t) }, enabled = !busy,
                            shape = SegmentedButtonDefaults.itemShape(i, WallpaperTarget.entries.size)) {
                            Text(t.short, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Option(label: String, content: @Composable () -> Unit) = Column {
    Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    Spacer(Modifier.height(10.dp))
    content()
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun WallpaperPreview() = PcTheme {
    val tones = previewState(false).tones.values.toList()
    WallpaperScreen(WallpaperUiState(tones, tones[1], myTone = tones[1]), IntSize(1080, 2400), remember { SnackbarHostState() }, false,
        {}, {}, {}, {}, {}, {}, {}, {})
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun LiveWallpaperPreview() = PcTheme {
    val tones = previewState(false).tones.values.toList()
    WallpaperScreen(WallpaperUiState(tones, tones[1], myTone = tones[1], live = true), IntSize(1080, 2400), remember { SnackbarHostState() }, false,
        {}, {}, {}, {}, {}, {}, {}, {})
}

/** Every style, light and dark, rendered synchronously for the IDE preview. */
@Preview(widthDp = 420, heightDp = 420)
@Composable
private fun StylesPreview() = PcTheme {
    val tone = previewState(false).tones.values.first()
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false, true).forEach { dark ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Style.entries.forEach { s ->
                    val img = remember(s, dark) { WallpaperRenderer.render(tone, s, 216, 480, dark).asImageBitmap() }
                    Image(img, s.label, Modifier.width(62.dp).aspectRatio(216 / 480f).clip(RoundedCornerShape(10.dp)))
                }
            }
        }
    }
}
