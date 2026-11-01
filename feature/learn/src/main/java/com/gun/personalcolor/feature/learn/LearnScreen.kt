package com.gun.personalcolor.feature.learn

import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.TintyHeader
import com.gun.personalcolor.core.ui.icon

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.PcCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal val CATEGORIES = listOf("기초", "진단 팁", "스타일링")

data class LearnUiState(val articles: List<Article>, val category: String? = null) {
    val visible get() = if (category == null) articles else articles.filter { it.category == category }
}

@HiltViewModel
class LearnViewModel @Inject constructor(content: ContentRepository) : ViewModel() {
    private val _state = MutableStateFlow(LearnUiState(content.articles))
    val state: StateFlow<LearnUiState> = _state.asStateFlow()
    fun select(category: String?) = _state.update { it.copy(category = category) }
}

@Composable
internal fun LearnScreenRoute(onBack: () -> Unit, onArticle: (String) -> Unit, vm: LearnViewModel = hiltViewModel()) {
    TrackScreen("learn")
    val state by vm.state.collectAsStateWithLifecycle()
    LearnScreen(state, onBack, vm::select, onArticle)
}

@Composable
fun LearnScreen(state: LearnUiState, onBack: () -> Unit, onCategory: (String?) -> Unit, onArticle: (String) -> Unit) {
    PcScaffold("퍼스널컬러 가이드", onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            modifier = Modifier.fillMaxSize().readableWidth(1040.dp),
            contentPadding = PaddingValues(
                start = ScreenPadding, end = ScreenPadding,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                TintyHeader("틴티의 컬러 노트", "알아 두면 옷 고르기가 쉬워져요", TintyProp.Book)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf<String?>(null) + CATEGORIES).forEach { c ->
                        FilterChip(
                            selected = state.category == c,
                            onClick = { onCategory(c) },
                            label = { Text(c ?: "전체", maxLines = 1) },
                            shape = RoundedCornerShape(50),
                            border = if (state.category == c) null else FilterChipDefaults.filterChipBorder(true, false, borderColor = MaterialTheme.colorScheme.outlineVariant),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
            // One in-feed ad after the first 4 articles (only when the list is long enough to hold it).
            val (head, tail) = state.visible.take(4) to state.visible.drop(4)
            items(head, key = { it.id }) { ArticleCard(it, onClick = { onArticle(it.id) }) }
            if (tail.isNotEmpty()) item(key = "ad", span = { GridItemSpan(maxLineSpan) }) { NativeAdCard() }
            items(tail, key = { it.id }) { ArticleCard(it, onClick = { onArticle(it.id) }) }
        }
    }
}

/** Card used in the list and in "다른 가이드". */
@Composable
internal fun ArticleCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PcCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(article.icon, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    "${article.category} · ${article.readMinutes}분",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
                Text(article.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

internal val previewArticles = List(5) { i ->
    Article("a$i", CATEGORIES[i % 3], "퍼스널컬러 가이드 예시 제목 $i", "미리보기용 요약 문장이에요. 두 줄까지 보여줘요.", 3, "🎨", emptyList())
}

@Preview
@Composable
private fun LearnPreview() = PcTheme { LearnScreen(LearnUiState(previewArticles), {}, {}, {}) }
