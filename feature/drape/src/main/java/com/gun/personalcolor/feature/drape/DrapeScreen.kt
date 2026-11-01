package com.gun.personalcolor.feature.drape

import com.gun.personalcolor.core.designsystem.TintyProp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.Verdict
import com.gun.personalcolor.core.designsystem.VerdictBadge
import com.gun.personalcolor.core.designsystem.TintyLoading
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.DiagnosisSession
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.vision.FacePhoto
import com.gun.personalcolor.core.vision.PersonSegmenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Person mask for the current photo. [mask] null after loading = segmentation failed (draw without cutout). */
data class Cutout(val loading: Boolean, val mask: ImageBitmap? = null)

@HiltViewModel
class DrapeViewModel @Inject constructor(
    session: DiagnosisSession,
    val content: ContentRepository,
    segmenter: PersonSegmenter,
) : ViewModel() {
    val diagnosis = session.current

    /** Computed once per diagnosis photo and kept while this screen lives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val cutout: StateFlow<Cutout> = session.current.map { it?.photo }.distinctUntilChanged()
        .transformLatest { photo ->
            if (photo == null) return@transformLatest emit(Cutout(false))
            emit(Cutout(true))
            emit(Cutout(false, withContext(Dispatchers.Default) { segmenter.personMask(photo.bitmap)?.asImageBitmap() }))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Cutout(true))
}

/** Virtual draping (BEST vs WORST side by side) and lip try-on (reward-gated) on the diagnosis photo. Tool screen: no ads. */
@Composable
internal fun DrapeRoute(onBack: () -> Unit, vm: DrapeViewModel = hiltViewModel()) {
    TrackScreen("drape")
    val d by vm.diagnosis.collectAsStateWithLifecycle()
    val cutout by vm.cutout.collectAsStateWithLifecycle()
    val diagnosis = d
    // No diagnosis photo (process death) or a tone id missing from content → leave instead of crashing.
    val photo = diagnosis?.photo
    val top = diagnosis?.let { vm.content.toneOrNull(it.top.toneId) }
    if (photo == null || top == null) { LaunchedEffect(Unit) { onBack() }; return }
    val others = remember(top) { vm.content.tones.filter { it.id != top.id } }
    val lipGroups = remember(top) {
        listOf("내 톤 추천" to top.makeup.lipColors, "WORST 립" to top.worstColors.take(4)) + others.map { it.nameKo to it.makeup.lipColors }
    }

    var lip by rememberSaveable { mutableStateOf(false) }
    var original by rememberSaveable { mutableStateOf(false) }
    var best by remember(top) { mutableStateOf(top.bestColors.firstOrNull()) }
    var compare by rememberSaveable { mutableIntStateOf(-1) } // -1 = my WORST, else index into [others]
    var right by remember(top) { mutableStateOf(top.worstColors.firstOrNull()) }
    var lipGroup by rememberSaveable { mutableIntStateOf(0) }
    var lipColor by remember(top) { mutableStateOf(top.makeup.lipColors.firstOrNull()) }
    var intensity by rememberSaveable { mutableFloatStateOf(0.65f) }
    var glossy by rememberSaveable { mutableStateOf(false) }
    // Lipstick is rendered into the photo's own pixels off the main thread; the last patch stays up while the next renders.
    val makeup by produceState<LipMakeup?>(null, photo, lip) {
        if (lip) value = withContext(Dispatchers.Default) { runCatching { LipMakeup.of(photo) }.getOrNull() }
    }
    val lipPatch by produceState<LipPatch?>(null, makeup, lipColor, intensity, glossy) {
        val m = makeup ?: return@produceState
        value = lipColor?.let { c -> withContext(Dispatchers.Default) { runCatching { m.render(c.hex, intensity, glossy) }.getOrNull() } }
    }
    val gate = rememberRewardGate()
    val analytics = LocalAnalytics.current

    val rightTone = others.getOrNull(compare)
    val rightLabel = rightTone?.nameKo ?: "WORST"
    val rightColors = rightTone?.bestColors ?: top.worstColors
    val bg = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF2E2C2B) else Color(0xFFE8E6E3)
    val neutralCloth = if (bg.luminance() < 0.5f) Color(0xFF4A4746) else Color(0xFFC9C6C2)

    PcScaffold("가상 체험", onBack, actions = {
        IconToggleButton(checked = original, onCheckedChange = { original = it }) {
            Icon(if (original) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "원본 보기")
        }
    }) { pad ->
        val wide = isWideWindow()
        val modeSwitch: @Composable () -> Unit = {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !lip, onClick = { lip = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text("컬러 드레이핑", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                SegmentedButton(
                    selected = lip,
                    onClick = {
                        if (!lip) gate(RewardRequest(key = "lip_tryon", title = "광고 보고 립 체험하기",
                            message = "짧은 광고 하나만 보면 무료예요.", action = { lip = true }))
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("립 체험", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        val panel: @Composable (String, NamedColor?, Boolean, Modifier, (DrawScope.(androidx.compose.ui.geometry.Rect) -> Unit)?) -> Unit =
            { badge, color, cloth, m, extra ->
                // Lip try-on has no cloth to compare → a squarer panel so the face, not the grey cape, fills it.
                val ratio = if (extra != null) 1f else 3f / 4f
                Column(m, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(ratio).clip(MaterialTheme.shapes.large)) {
                        Surface(Modifier.fillMaxSize(), color = bg) {}
                        if (cutout.loading) {
                            // One Tinty for the side-by-side drape panels (drawn over the row below); the lip panel gets its own.
                            if (extra != null) TintyLoading(Modifier.align(Alignment.Center), size = 112.dp, prop = TintyProp.Fabric)
                        } else {
                            DrapedPhoto(
                                photo, cutout.mask, bg,
                                cloth = if (cloth) color?.let { hexColor(it.hex) } else neutralCloth,
                                original = original, modifier = Modifier.fillMaxSize(), extra = extra ?: {},
                            )
                        }
                        if (badge.isNotEmpty() && !original) Badge(badge, Modifier.align(Alignment.TopStart).padding(10.dp))
                    }
                    Text(color?.name ?: "", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }

        val drapePanels: @Composable (Modifier) -> Unit = { m ->
            Box(m) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    panel("BEST", best, true, Modifier.weight(1f), null)
                    panel(rightLabel, right, true, Modifier.weight(1f), null)
                }
                if (cutout.loading) TintyLoading(Modifier.align(Alignment.Center), size = 112.dp, prop = TintyProp.Fabric)
            }
        }
        val drapeControls: @Composable () -> Unit = {
            Text("얼굴빛이 더 맑아 보이는 쪽이 잘 어울리는 색이에요", style = Muted)
            SectionCard(null) {
                ColorRow("BEST", top.bestColors, best, { best = it })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ColorRow(rightLabel, rightColors, right, { right = it }) {
                    CompareMenu(top, others, compare) { i ->
                        compare = i
                        right = (others.getOrNull(i)?.bestColors ?: top.worstColors).firstOrNull()
                    }
                }
            }
        }

        val lipPanel: @Composable (Modifier) -> Unit = { m ->
            panel("", lipColor, false, m) { r -> lipPatch?.draw(this, r) }
        }
        val lipControls: @Composable () -> Unit = {
            Text(lipHint(lipGroup), style = Muted)
            SectionCard(null) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lipGroups.size) { i ->
                        FilterChip(selected = lipGroup == i, onClick = { lipGroup = i; lipColor = lipGroups[i].second.firstOrNull() },
                            label = { Text(lipGroups[i].first, maxLines = 1) })
                    }
                }
                DotRow(lipGroups.getOrNull(lipGroup)?.second.orEmpty(), lipColor) { c ->
                    lipColor = c
                    analytics?.log("lip_tryon", "color" to c.name)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("질감", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    SingleChoiceSegmentedButtonRow {
                        listOf("매트", "글로시").forEachIndexed { i, label ->
                            SegmentedButton(selected = glossy == (i == 1), onClick = { glossy = i == 1 },
                                shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label, maxLines = 1) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("연하게", style = Muted, maxLines = 1)
                    Slider(intensity, { intensity = it }, Modifier.weight(1f).padding(horizontal = 12.dp), valueRange = 0.3f..1f)
                    Text("진하게", style = Muted, maxLines = 1)
                }
            }
        }

        val scroll = rememberScrollState()
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(pad).readableWidth(1100.dp).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Panels sized by height so both fit a short landscape window.
                Box(Modifier.weight(1.2f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
                    if (lip) lipPanel(Modifier.widthIn(max = 420.dp).fillMaxWidth())
                    else drapePanels(Modifier.widthIn(max = 640.dp).fillMaxWidth())
                }
                Column(
                    Modifier.weight(1f).verticalScroll(scroll).padding(vertical = 16.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    modeSwitch()
                    if (lip) lipControls() else drapeControls()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(pad).readableWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                modeSwitch()
                if (lip) lipPanel(Modifier.fillMaxWidth()) else drapePanels(Modifier.fillMaxWidth())
                if (lip) lipControls() else drapeControls()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Lip try-on hint for the chip group in [lipGroups] order: 0 my picks, 1 my WORST, then other tones. */
internal fun lipHint(group: Int): String = when (group) {
    0 -> "생기 있어 보이는 색이 잘 어울려요"
    1 -> "얼굴이 칙칙해 보일 수 있어요"
    else -> "다른 톤에 어울리는 립이에요. 내 톤과 비교해 보세요"
}

/** BEST / WORST verdict pill, or a neutral pill with another tone's name. */
@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    when (text) {
        "BEST" -> VerdictBadge(Verdict.BEST, modifier)
        "WORST" -> VerdictBadge(Verdict.WORST, modifier)
        else -> Surface(modifier, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
        }
    }
}

/** Small label (+ optional trailing control) over a row of color dots. */
@Composable
private fun ColorRow(label: String, colors: List<NamedColor>, selected: NamedColor?, onSelect: (NamedColor) -> Unit, trailing: @Composable () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Badge(label) }
            trailing()
        }
        DotRow(colors, selected, onSelect)
    }
}

@Composable
private fun DotRow(colors: List<NamedColor>, selected: NamedColor?, onSelect: (NamedColor) -> Unit) {
    LazyRow(contentPadding = PaddingValues(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(colors) { c -> Dot(c, c == selected) { onSelect(c) } }
    }
}

/** "다른 톤과 비교" dropdown: my WORST colors or another tone's BEST colors on the right panel. */
@Composable
private fun CompareMenu(top: Tone, others: List<Tone>, compare: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text("다른 톤과 비교", maxLines = 1)
            Icon(Icons.Outlined.ArrowDropDown, null)
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("${top.nameKo} WORST", maxLines = 1) }, onClick = { open = false; onPick(-1) },
                trailingIcon = if (compare == -1) ({ Icon(Icons.Rounded.Check, "선택됨") }) else null)
            others.forEachIndexed { i, t ->
                DropdownMenuItem(text = { Text("${t.nameKo} BEST", maxLines = 1) }, onClick = { open = false; onPick(i) },
                    trailingIcon = if (compare == i) ({ Icon(Icons.Rounded.Check, "선택됨") }) else null)
            }
        }
    }
}

/** Color chip; the selected one gets an accent ring with a small gap. */
@Composable
private fun Dot(c: NamedColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .then(if (selected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClickLabel = c.name, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(5.dp),
    ) { Swatch(c.hex, Modifier.fillMaxSize(), CircleShape) }
}
