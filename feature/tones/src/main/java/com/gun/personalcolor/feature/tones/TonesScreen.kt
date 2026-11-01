package com.gun.personalcolor.feature.tones

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.SEASON_KO
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.ToneCard

private const val ALL = "all"
private const val FAVORITES = "favorites"

@Composable
fun TonesScreen(state: TonesUiState, onTone: (String) -> Unit, onToggleFavorite: (String) -> Unit) {
    TrackScreen("tones")
    var filter by rememberSaveable { mutableStateOf(ALL) }
    PcScaffold("컬러 도감") { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.fillMaxSize().readableWidth(1040.dp),
            contentPadding = PaddingValues(
                start = ScreenPadding, end = ScreenPadding,
                top = pad.calculateTopPadding() + 4.dp, bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            fullSpan("filters") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4계절을 명도·채도로 나눈 12가지 톤이에요", style = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val options = listOf(ALL to "전체") + SEASON_KO.toList() + (FAVORITES to "★ 즐겨찾기")
                        options.forEach { (key, label) ->
                            FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label, maxLines = 1) })
                        }
                    }
                }
            }
            when (filter) {
                ALL -> state.tones.groupBy { it.season }.forEach { (season, tones) ->
                    fullSpan("h_$season") {
                        Text(SEASON_KO[season].orEmpty(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                    toneCards(tones, state, onTone, onToggleFavorite)
                    // One in-feed ad between the summer and autumn sections.
                    if (season == "summer") fullSpan("ad") { NativeAdCard(Modifier.padding(top = 12.dp)) }
                }
                FAVORITES -> {
                    val favs = state.tones.filter { it.id in state.favorites }
                    if (favs.isEmpty()) fullSpan("empty") {
                        EmptyState(Icons.Outlined.StarBorder, "즐겨찾는 톤이 없어요", "카드의 ☆를 눌러 모아요")
                    }
                    else toneCards(favs, state, onTone, onToggleFavorite)
                }
                else -> toneCards(state.tones.filter { it.season == filter }, state, onTone, onToggleFavorite)
            }
        }
    }
}

private fun LazyGridScope.fullSpan(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.toneCards(tones: List<Tone>, state: TonesUiState, onTone: (String) -> Unit, onToggleFavorite: (String) -> Unit) =
    items(tones, key = { it.id }) { tone ->
        ToneCard(tone, onClick = { onTone(tone.id) }, trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tone.id == state.myToneId) Tag("내 톤", color = MaterialTheme.colorScheme.primaryContainer)
                FavoriteButton(tone.id in state.favorites) { onToggleFavorite(tone.id) }
            }
        })
    }

@Preview
@Composable
private fun TonesPreview() = PcTheme {
    TonesScreen(TonesUiState(previewTones(), "spring_light", setOf("summer_light")), {}, {})
}
