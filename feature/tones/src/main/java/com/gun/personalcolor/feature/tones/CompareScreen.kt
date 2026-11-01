package com.gun.personalcolor.feature.tones

import com.gun.personalcolor.core.ui.accent
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.Verdict
import com.gun.personalcolor.core.designsystem.VerdictBadge
import com.gun.personalcolor.core.ui.tintyColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.Bullet
import com.gun.personalcolor.core.ui.ColorDots
import com.gun.personalcolor.core.ui.SEASON_KO
import com.gun.personalcolor.core.ui.SwatchGrid
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo
import kotlin.math.sqrt

@Composable
fun CompareScreen(tones: List<Tone>, initialA: String, initialB: String?, onBack: () -> Unit) {
    TrackScreen("tone_compare")
    var aId by rememberSaveable { mutableStateOf(initialA) }
    var bId by rememberSaveable { mutableStateOf(initialB ?: OPPOSITE[initialA] ?: tones.firstOrNull { it.id != initialA }?.id) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "a" | "b"
    val a = tones.firstOrNull { it.id == aId }
    val b = tones.firstOrNull { it.id == bId }

    PcScaffold("톤 비교", onBack) { pad ->
        if (a == null || b == null) return@PcScaffold
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
                .readableWidth().padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Selector("A", a, Modifier.weight(1f)) { picking = "a" }
                Selector("B", b, Modifier.weight(1f)) { picking = "b" }
            }
            SideBySide(null, a, b) { t ->
                // Each 틴티 holds up its tone's signature color.
                Tinty(Modifier.size(88.dp), remember(t) { t.tintyColors }, animated = false, prop = TintyProp.ColorCard, propColor = remember(t) { t.accent })
                Text(t.nameKo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${t.seasonKo} · ${t.temperatureKo}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            SideBySide("키워드", a, b) { t -> Keywords(t.keywords) }
            SideBySide("어울리는 컬러", a, b, badge = Verdict.BEST) { t -> Swatches(t.bestColors) }
            SideBySide("피하면 좋은 컬러", a, b, badge = Verdict.WORST) { t -> Swatches(t.worstColors) }
            SideBySide("립 컬러", a, b) { t -> Swatches(t.makeup.lipColors) }
            SideBySide("헤어 컬러", a, b) { t -> Swatches(t.hair.colors) }
            SideBySide("액세서리", a, b) { t -> Text(t.accessory.metal, style = MaterialTheme.typography.bodyLarge) }
            Differences(a, b)
            NativeAdCard()
        }
    }

    picking?.let { which ->
        TonePicker(tones, selected = if (which == "a") aId else bId, onDismiss = { picking = null }) {
            if (which == "a") aId = it else bId = it
            picking = null
        }
    }
}

@Composable
private fun Selector(label: String, tone: Tone, modifier: Modifier, onClick: () -> Unit) {
    PcCard(modifier, onClick = onClick, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.heightIn(min = 60.dp).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneAvatar(tone, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("톤 $label", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Text(tone.nameKo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "톤 $label 바꾸기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One card with the same content for tone A (left) and B (right), always two equal columns. */
@Composable
private fun SideBySide(title: String?, a: Tone, b: Tone, badge: Verdict? = null, content: @Composable ColumnScope.(Tone) -> Unit) {
    SectionCard(title, action = badge?.let { { VerdictBadge(it) } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(a, b).forEach { t ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { content(t) }
            }
        }
    }
}

@Composable
private fun Swatches(colors: List<NamedColor>) = SwatchGrid(colors, minColumns = 3, minCell = 40.dp, showNames = false)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Keywords(keywords: List<String>) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        keywords.forEach { Tag("#$it") }
    }

@Composable
private fun Differences(a: Tone, b: Tone) {
    val shared = sharedColors(a.bestColors, b.bestColors)
    SectionCard("한눈에 비교") {
        Bullet(
            if (a.isWarm == b.isWarm) "두 톤 모두 ${a.temperatureKo}이라 어울리는 색의 온도가 같아요."
            else "${a.temperatureKo}과 ${b.temperatureKo}, 노란 기운과 푸른 기운이 달라요."
        )
        Bullet(
            if (a.season == b.season) "같은 ${a.seasonKo} 계열이라 분위기가 비슷해요."
            else "${a.seasonKo}과 ${b.seasonKo}, 계절이 달라 분위기가 꽤 달라요."
        )
        Bullet(
            if (a.accessory.metal == b.accessory.metal) "액세서리는 둘 다 ${a.accessory.metal}가 잘 어울려요."
            else "액세서리는 ${a.nameKo}엔 ${a.accessory.metal}, ${b.nameKo}엔 ${b.accessory.metal}가 어울려요."
        )
        if (shared.isEmpty()) Bullet("겹치는 BEST 컬러는 없어요.")
        else {
            Bullet("겹치는 BEST 컬러: ${shared.joinToString { it.name }}")
            ColorDots(shared, size = 22.dp)
        }
    }
}

/** A's best colors that also appear (same or very close) in B's best colors. */
internal fun sharedColors(a: List<NamedColor>, b: List<NamedColor>): List<NamedColor> =
    a.filter { ca -> b.any { cb -> rgbDistance(ca.hex, cb.hex) < 40.0 } }

private fun rgbDistance(h1: String, h2: String): Double {
    // Bad hex in content → treat as "not shared" rather than crash.
    val x = h1.removePrefix("#").toLongOrNull(16) ?: return Double.MAX_VALUE
    val y = h2.removePrefix("#").toLongOrNull(16) ?: return Double.MAX_VALUE
    fun ch(v: Long, shift: Int) = ((v shr shift) and 0xFF).toDouble()
    val dr = ch(x, 16) - ch(y, 16); val dg = ch(x, 8) - ch(y, 8); val db = ch(x, 0) - ch(y, 0)
    return sqrt(dr * dr + dg * dg + db * db)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TonePicker(tones: List<Tone>, selected: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp)) {
            tones.groupBy { it.season }.forEach { (season, group) ->
                item(key = season) {
                    Eyebrow(SEASON_KO[season].orEmpty(), Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(group, key = { it.id }) { t ->
                    ToneRow(t, onClick = { onPick(t.id) }, trailing = {
                        if (t.id == selected) Icon(Icons.Rounded.Check, "선택됨", tint = MaterialTheme.colorScheme.primary)
                    })
                }
            }
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun ComparePreview() = PcTheme { CompareScreen(previewTones(), "spring_light", null, {}) }
