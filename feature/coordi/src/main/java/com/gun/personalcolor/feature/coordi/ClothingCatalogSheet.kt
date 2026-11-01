package com.gun.personalcolor.feature.coordi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.mood.Myeongjo
import com.gun.personalcolor.core.ui.mood.ShadowedGarment
import com.gun.personalcolor.core.ui.mood.moodBackdrop
import com.gun.personalcolor.core.ui.tintyColors

private val DENIM_KINDS = setOf("jeans", "bootcut", "denim_skirt", "denim_jacket", "shorts", "overall")

/**
 * A styled, wearable color per kind (deterministic by catalog index): the tone's neutrals like OutfitEngine — denim kinds in
 * denim, shoes / bags in leather, coats never white — and every 5th non-shoe tile one of the tone's softer accents.
 */
private fun catalogColor(tone: Tone?, i: Int, kind: String, group: String): NamedColor {
    tone ?: return listOf("#F1E6CF" to "아이보리", "#B5844F" to "카멜", "#6E4A30" to "브라운", "#3A3C42" to "차콜")
        .let { it[i % it.size] }.let { (hex, name) -> NamedColor(name, hex) }
    val n = OutfitEngine.neutralsOf(tone)
    val soft = OutfitEngine.accentsOf(tone).let { it.take((it.size + 1) / 2) }
    val list = when {
        i % 5 == 2 && group != "신발" && kind !in DENIM_KINDS && soft.isNotEmpty() -> return soft[(i / 5) % soft.size]
        kind in DENIM_KINDS -> n.denim
        group == "신발" || group == "가방" -> n.leather
        group == "아우터" -> n.base.filter { it.name != "화이트" }
        else -> n.base
    }
    return list[i % list.size]
}

/** Reference only: every garment kind 오늘의 코디 can pick from, on the mood backdrop, in styled neutrals with a few soft accents. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClothingCatalogSheet(tone: Tone?, onDismiss: () -> Unit) {
    val groups = CLOTHING_KINDS.entries.mapIndexed { i, e -> Triple(e.key, e.value.first, catalogColor(tone, i, e.key, e.value.second)) to e.value.second }
        .groupBy({ it.second }, { it.first })
    val tinty = remember(tone) { tone?.tintyColors ?: TintyColors.Brand }
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyVerticalGrid(GridCells.Adaptive(100.dp), contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("추천하는 옷 종류", style = MaterialTheme.typography.titleLarge)
                        Text("날씨와 내 톤에 맞춰 이 중에서 골라요", style = Muted)
                    }
                    Tinty(Modifier.size(56.dp), tinty, TintyExpression.Happy, animated = false)
                }
            }
            groups.forEach { (group, kinds) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(group)
                        Spacer(Modifier.width(6.dp))
                        Text("${kinds.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(kinds, key = { it.first }) { (kind, label, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShadowedGarment(kind, hexColor(color.hex), Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                            .moodBackdrop(kind.hashCode()).padding(14.dp))
                        Text(label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge.copy(fontFamily = Myeongjo),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        Text(color.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
