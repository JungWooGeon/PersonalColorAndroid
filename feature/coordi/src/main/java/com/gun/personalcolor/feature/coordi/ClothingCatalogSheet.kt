package com.gun.personalcolor.feature.coordi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.ClothingIllustration
import com.gun.personalcolor.core.ui.hexColor

/** Reference only: every garment kind 오늘의 코디 can pick from, drawn in the user's best colors. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClothingCatalogSheet(tone: Tone?, onDismiss: () -> Unit) {
    val colors = tone?.bestColors?.map { it.hex }?.takeIf { it.isNotEmpty() } ?: listOf("#C19A6B", "#708238", "#1F2A44", "#F3E9D2")
    // (kind, label, color): colors rotate through the palette in list order.
    val groups = CLOTHING_KINDS.entries.mapIndexed { i, e -> Triple(e.key, e.value.first, colors[i % colors.size]) to e.value.second }
        .groupBy({ it.second }, { it.first })
    ModalBottomSheet(onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyVerticalGrid(GridCells.Adaptive(96.dp), contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("추천하는 옷 종류", style = MaterialTheme.typography.titleLarge)
                    Text("날씨와 내 톤에 맞춰 이 중에서 골라요", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            groups.forEach { (group, kinds) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("$group ${kinds.size}", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                items(kinds, key = { it.first }) { (kind, label, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            ClothingIllustration(kind, hexColor(color), Modifier.fillMaxWidth().aspectRatio(1f).padding(10.dp))
                        }
                        Text(label, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
