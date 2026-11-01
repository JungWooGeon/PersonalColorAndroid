package com.gun.personalcolor.feature.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.gun.personalcolor.core.designsystem.PcTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.model.Accessory
import com.gun.personalcolor.core.model.Fashion
import com.gun.personalcolor.core.model.Hair
import com.gun.personalcolor.core.model.HistoryEntry
import com.gun.personalcolor.core.model.Makeup
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.ResultSource
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.TonePercent
import com.gun.personalcolor.core.ui.ToneAvatar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val ResultSource.label get() = if (this == ResultSource.AI) "AI 사진 진단" else "셀프 진단"

internal fun HistoryEntry.dateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
internal val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
internal val DAY_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm")
internal val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월")

/** "비슷한 톤 · 여름 뮤트, 가을 뮤트" — the runner-up candidates by name only (no percentages shown to users), or null. */
internal fun HistoryEntry.runnersUp(tones: Map<String, Tone>): String? =
    top.drop(1).take(2).mapNotNull { p -> tones[p.toneId]?.nameKo }
        .joinToString(", ").ifEmpty { null }?.let { "비슷한 톤 · $it" }

/** Avatar + tone name + one or two small lines + chevron. */
@Composable
internal fun ToneEntryRow(tone: Tone, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier, extra: String? = null) {
    Surface(onClick = onClick, modifier = modifier, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ToneAvatar(tone, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tone.nameKo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (extra != null) Text(extra, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PcTheme.colors.ink3)
        }
    }
}

/** Two equal buttons side by side; stacked when there's not enough room for single-line labels. */
@Composable
internal fun ButtonPair(
    modifier: Modifier = Modifier,
    stackBelow: Dp = 300.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < stackBelow) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.weight(1f)); second(Modifier.weight(1f))
            }
        }
    }
}

/** Fake data for @Preview only. */
internal fun previewState(withData: Boolean): MyUiState {
    fun t(id: String, season: String, name: String, vararg hex: String): Tone {
        val c = hex.mapIndexed { i, h -> NamedColor("컬러 ${i + 1}", h) }
        return Tone(id, season, name, id, listOf("화사한", "맑은", "산뜻한"), "", "", c, c, Makeup("", c, "", c, "", c, ""),
            Hair(c, ""), Fashion("", emptyList()), Accessory("골드", ""), "")
    }
    val tones = listOf(
        t("spring_light", "spring", "봄 라이트", "#FFCBA4", "#F8A98E", "#F7B2B7", "#FFE5A0", "#B5E3D8"),
        t("summer_mute", "summer", "여름 뮤트", "#B8A9C9", "#9DB4C0", "#C9A9A6", "#8E9AAF", "#D8C3D1"),
        t("autumn_mute", "autumn", "가을 뮤트", "#C19A6B", "#A0785A", "#8A9A5B", "#D2B48C", "#B5651D"),
    ).associateBy { it.id }
    if (!withData) return MyUiState(loading = false, tones = tones)
    val now = System.currentTimeMillis()
    val entries = (0 until 5).map { i ->
        HistoryEntry(if (i % 2 == 0) "summer_mute" else "spring_light", if (i % 2 == 0) ResultSource.AI else ResultSource.QUIZ,
            now - i * 9L * 24 * 3600 * 1000,
            listOf(TonePercent("summer_mute", 0.52), TonePercent("autumn_mute", 0.21), TonePercent("spring_light", 0.15)))
    }
    return MyUiState(false, tones, tones["summer_mute"], tones.values.toList(), entries)
}
