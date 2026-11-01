package com.gun.personalcolor.feature.my

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
import com.gun.personalcolor.core.designsystem.ListRow
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
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val style: Style = Style.TODAY,
    val dark: Boolean = false,
    val showName: Boolean = true,
    /** The user's own tone; daily auto wallpaper needs it. */
    val myTone: Tone? = null,
    val autoEnabled: Boolean = false,
    val autoTarget: WallpaperTarget = WallpaperTarget.BOTH,
)

@HiltViewModel
class WallpaperViewModel @Inject constructor(
    savedState: SavedStateHandle,
    content: ContentRepository,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WallpaperUiState(tones = content.tones))
    val state = _state.asStateFlow()

    init {
        val routeToneId = savedState.toRoute<WallpaperRoute>().toneId
        viewModelScope.launch {
            // Route tone → my tone → first tone (the user can switch with the chips).
            val id = routeToneId ?: prefs.preferences.first().myToneId
            _state.update { it.copy(tone = content.toneOrNull(id) ?: content.tones.firstOrNull()) }
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
    fun setStyle(style: Style) {
        _state.update { it.copy(style = style) }
        // The daily wallpaper follows the chosen style from the next change on.
        val s = _state.value
        if (s.autoEnabled) viewModelScope.launch { prefs.setAutoWallpaper(true, style.name, s.autoTarget.name) }
    }
    fun setAuto(enabled: Boolean, target: WallpaperTarget) = viewModelScope.launch {
        prefs.setAutoWallpaper(enabled, _state.value.style.name, target.name)
    }
    fun setDark(dark: Boolean) = _state.update { it.copy(dark = dark) }
    fun setShowName(show: Boolean) = _state.update { it.copy(showName = show) }
}

private val Style.label get() = when (this) {
    Style.TODAY -> "오늘의 컬러"
    Style.PALETTE_CARD -> "컬러 카드"
    Style.SEASON -> "계절 무드"
    Style.STRIPES -> "스트라이프"
    Style.GRID -> "그리드"
    Style.GRADIENT -> "그라데이션"
    Style.DOTS -> "도트"
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

    fun fullSize(s: WallpaperUiState, tone: Tone) = WallpaperRenderer.render(tone, s.style, size.width, size.height, s.dark, s.showName)

    fun setAuto(enabled: Boolean, target: WallpaperTarget) {
        val my = state.myTone ?: return
        if (!enabled) {
            vm.setAuto(false, target)
            AutoWallpaperScheduler.cancel(context)
            analytics?.log("auto_wallpaper", "on" to "false")
            return
        }
        busy = true
        scope.launch {
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val ok = AutoWallpaperScheduler.apply(context, my, state.style, target, night)
            busy = false
            if (ok) {
                vm.setAuto(true, target)
                AutoWallpaperScheduler.schedule(context)
                analytics?.log("auto_wallpaper", "on" to "true", "style" to state.style.name.lowercase(), "target" to target.name.lowercase())
                snackbar.showSnackbar("오늘의 컬러로 바꿨어요. 내일부터 매일 자동으로 바뀌어요")
            } else {
                snackbar.showSnackbar(
                    if (canSetWallpaper(context)) "배경화면을 설정하지 못했어요. 잠시 후 다시 시도해 주세요"
                    else "이 기기에서는 앱이 배경화면을 바꿀 수 없어요"
                )
            }
        }
    }

    WallpaperScreen(
        state = state, screen = size, snackbar = snackbar, busy = busy, onBack = onBack,
        onStyle = vm::setStyle, onTone = vm::select, onDark = vm::setDark, onShowName = vm::setShowName,
        onAuto = { setAuto(it, state.autoTarget) },
        onTarget = { t -> if (state.autoEnabled) setAuto(true, t) else vm.setAuto(false, t) },
        onSave = save@{
            val s = state; val tone = s.tone ?: return@save
            analytics?.log("wallpaper_save", "style" to s.style.name.lowercase(), "tone" to tone.id)
            saveImage("personal-color-wallpaper") { fullSize(s, tone) }
        },
        onApply = apply@{
            val s = state; val tone = s.tone ?: return@apply
            busy = true
            scope.launch {
                val ok = canSetWallpaper(context) && withContext(Dispatchers.Default) {
                    try { android.app.WallpaperManager.getInstance(context).setBitmap(fullSize(s, tone)); true } catch (e: Exception) { false }
                }
                busy = false
                analytics?.log("wallpaper_set", "style" to s.style.name.lowercase(), "ok" to ok.toString())
                snackbar.showSnackbar(if (ok) "배경화면으로 설정했어요" else "배경화면을 설정하지 못했어요. 갤러리에 저장해 직접 설정해 보세요")
            }
        },
    )
}

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
    onShowName: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onTarget: (WallpaperTarget) -> Unit,
    onSave: () -> Unit,
    onApply: () -> Unit,
) {
    PcScaffold(
        "배경화면 만들기", onBack,
        bottomBar = {
            BottomActions {
                ButtonPair(stackBelow = 360.dp,
                    first = { PcOutlinedButton("갤러리에 저장", onSave, it, enabled = state.tone != null) },
                    second = { PcButton("배경화면으로 설정", onApply, it, enabled = state.tone != null && !busy) })
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            val options: @Composable () -> Unit = { Options(state, busy, onStyle, onTone, onDark, onShowName, onAuto, onTarget) }
            if (isWideWindow()) {
                Row(
                    Modifier.fillMaxSize().padding(pad).readableWidth(1040.dp).padding(horizontal = ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        PhonePreview(state, screen, Modifier.fillMaxHeight().aspectRatio(screen.width / screen.height.toFloat(), matchHeightConstraintsFirst = true))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) { options() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad).readableWidth()
                        .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    PhonePreview(state, screen, Modifier.widthIn(max = 240.dp).fillMaxWidth(0.62f).aspectRatio(screen.width / screen.height.toFloat()))
                    options()
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = pad.calculateBottomPadding()).padding(16.dp))
        }
    }
}

/** Small live render inside a phone-shaped frame, with a mock clock showing the calm area. */
@Composable
private fun PhonePreview(state: WallpaperUiState, screen: IntSize, modifier: Modifier) {
    val tone = state.tone
    val image by produceState<ImageBitmap?>(null, tone, state.style, state.dark, state.showName) {
        if (tone == null) return@produceState
        val w = 300; val h = (w * screen.height.toFloat() / screen.width).toInt()
        value = withContext(Dispatchers.Default) { WallpaperRenderer.render(tone, state.style, w, h, state.dark, state.showName).asImageBitmap() }
    }
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).border(6.dp, MaterialTheme.colorScheme.onSurface, shape)) {
        image?.let { Image(it, contentDescription = "배경화면 미리보기", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text("9:41", style = MaterialTheme.typography.headlineMedium, color = if (state.dark) Color.White else Color(0xFF1D1B1A),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp))
    }
}

@Composable
private fun Options(
    state: WallpaperUiState,
    busy: Boolean,
    onStyle: (Style) -> Unit,
    onTone: (Tone) -> Unit,
    onDark: (Boolean) -> Unit,
    onShowName: (Boolean) -> Unit,
    onAuto: (Boolean) -> Unit,
    onTarget: (WallpaperTarget) -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Option("스타일") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Style.entries.forEach { s ->
                FilterChip(selected = state.style == s, onClick = { onStyle(s) }, label = { Text(s.label, maxLines = 1, softWrap = false) })
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
    Option("배경") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("라이트", "다크").forEachIndexed { i, label ->
                SegmentedButton(selected = state.dark == (i == 1), onClick = { onDark(i == 1) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        ListRow(if (state.style == Style.TODAY) "날짜 표시" else if (state.style == Style.PALETTE_CARD) "컬러 이름 표시" else "톤 이름 표시",
            subtitle = "아래쪽에 작게 들어가요", onClick = { onShowName(!state.showName) },
            trailing = { Switch(checked = state.showName, onCheckedChange = null) })
    }
    AutoWallpaperCard(state, busy, onAuto, onTarget)
}

@Composable
private fun AutoWallpaperCard(state: WallpaperUiState, busy: Boolean, onAuto: (Boolean) -> Unit, onTarget: (WallpaperTarget) -> Unit) {
    val available = state.myTone != null && !busy
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            ListRow(
                "매일 오늘의 컬러로 바꾸기",
                subtitle = state.myTone?.let { "자정이 지나면 ${it.nameKo}의 오늘의 컬러로 바뀌어요" } ?: "내 톤을 먼저 정하면 쓸 수 있어요",
                onClick = if (available) {{ onAuto(!state.autoEnabled) }} else null,
                trailing = { Switch(checked = state.autoEnabled, onCheckedChange = null, enabled = available) },
            )
            if (state.myTone != null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    WallpaperTarget.entries.forEachIndexed { i, t ->
                        SegmentedButton(selected = state.autoTarget == t, onClick = { onTarget(t) }, enabled = !busy,
                            shape = SegmentedButtonDefaults.itemShape(i, WallpaperTarget.entries.size)) {
                            Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Option(label: String, content: @Composable () -> Unit) = Column {
    Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    Spacer(Modifier.height(10.dp))
    content()
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun WallpaperPreview() = PcTheme {
    val tones = previewState(false).tones.values.toList()
    WallpaperScreen(WallpaperUiState(tones, tones[1]), IntSize(1080, 2400), remember { SnackbarHostState() }, false,
        {}, {}, {}, {}, {}, {}, {}, {}, {})
}
