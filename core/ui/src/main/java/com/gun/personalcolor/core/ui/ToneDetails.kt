package com.gun.personalcolor.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.ui.graphics.vector.ImageVector
import com.gun.personalcolor.core.model.Article
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.Verdict
import com.gun.personalcolor.core.designsystem.VerdictBadge
import com.gun.personalcolor.core.designsystem.isExpandedWindow
import com.gun.personalcolor.core.model.Tone

/** Full guide for a tone as cards. Two columns on expanded windows. Shared by tone detail and result screens. */
@Composable
fun ToneDetails(tone: Tone, modifier: Modifier = Modifier) {
    val cards: List<@Composable () -> Unit> = listOf(
        { SectionCard("특징") { tone.characteristics.lines().filter { it.isNotBlank() }.forEach { Bullet(it.trim()) } } },
        { SectionCard("어울리는 컬러", action = { VerdictBadge(Verdict.BEST) }) { SwatchGrid(tone.bestColors) } },
        { SectionCard("피하면 좋은 컬러", action = { VerdictBadge(Verdict.WORST) }) { SwatchGrid(tone.worstColors); Text(tone.avoidTip, style = Muted) } },
        {
            SectionCard("메이크업") {
                Text(tone.makeup.base, style = MaterialTheme.typography.bodyMedium)
                Sub("립"); SwatchGrid(tone.makeup.lipColors); Text(tone.makeup.lipTip, style = Muted)
                Sub("아이"); SwatchGrid(tone.makeup.eyeColors); Text(tone.makeup.eyeTip, style = Muted)
                Sub("블러셔"); SwatchGrid(tone.makeup.blushColors); Text(tone.makeup.blushTip, style = Muted)
            }
        },
        { SectionCard("헤어 컬러") { SwatchGrid(tone.hair.colors); Text(tone.hair.tip, style = Muted) } },
        {
            SectionCard("코디") {
                Text(tone.fashion.tip, style = MaterialTheme.typography.bodyMedium)
                tone.fashion.styling.forEach { Bullet(it) }
            }
        },
        { SectionCard("액세서리 · ${tone.accessory.metal}") { Text(tone.accessory.tip, style = MaterialTheme.typography.bodyMedium) } },
    )
    if (isExpandedWindow()) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            listOf(cards.filterIndexed { i, _ -> i % 2 == 0 }, cards.filterIndexed { i, _ -> i % 2 == 1 }).forEach { col ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) { col.forEach { it() } }
            }
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) { cards.forEach { it() } }
    }
}

@Composable
fun Bullet(text: String) = Row {
    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Sub(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 6.dp))

/** Line icon per guide category (no emoji in UI). */
val Article.icon: ImageVector get() = when (category) {
    "진단 팁" -> Icons.Outlined.CameraAlt
    "스타일링" -> Icons.Outlined.Checkroom
    else -> Icons.Outlined.AutoStories
}
