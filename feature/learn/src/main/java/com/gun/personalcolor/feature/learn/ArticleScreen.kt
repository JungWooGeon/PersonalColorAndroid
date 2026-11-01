package com.gun.personalcolor.feature.learn

import com.gun.personalcolor.core.designsystem.PropTile
import com.gun.personalcolor.core.ui.prop


import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.IconTile
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.aurora
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionHeader
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.Article
import com.gun.personalcolor.core.model.ArticleSection
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.ui.Bullet
import com.gun.personalcolor.core.ui.SwatchGrid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class ArticleUiState(val id: String, val article: Article?, val related: List<Article>)

@HiltViewModel
class ArticleViewModel @Inject constructor(savedState: SavedStateHandle, content: ContentRepository) : ViewModel() {
    val state: StateFlow<ArticleUiState> = MutableStateFlow(
        savedState.toRoute<ArticleRoute>().id.let { id ->
            val article = content.article(id)
            // Same category first, then the rest; 3 in total.
            val others = content.articles.filter { it.id != id }
            val related = (others.filter { it.category == article?.category } + others.filter { it.category != article?.category }).take(3)
            ArticleUiState(id, article, related)
        }
    )
}

@Composable
internal fun ArticleScreenRoute(
    onBack: () -> Unit, onArticle: (String) -> Unit, onAnalyze: () -> Unit, onQuiz: () -> Unit,
    vm: ArticleViewModel = hiltViewModel(),
) {
    TrackScreen("article")
    val state by vm.state.collectAsStateWithLifecycle()
    val analytics = LocalAnalytics.current
    LaunchedEffect(state.id) { analytics?.log("article_view", "id" to state.id) }
    ArticleScreen(state, onBack, onArticle, onAnalyze, onQuiz)
}

@Composable
fun ArticleScreen(state: ArticleUiState, onBack: () -> Unit, onArticle: (String) -> Unit, onAnalyze: () -> Unit, onQuiz: () -> Unit) {
    val article = state.article
    if (article == null) {
        PcScaffold("가이드", onBack) { padding ->
            EmptyState(Icons.Outlined.SearchOff, "가이드를 찾을 수 없어요", "삭제됐거나 주소가 바뀌었어요.", Modifier.padding(padding))
        }
        return
    }
    PcScaffold(article.title, onBack) { padding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding)) {
            Column(
                Modifier.readableWidth().padding(horizontal = ScreenPadding).padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Header(article)
                article.sections.forEach { Section(it) }
                CtaCard(onAnalyze, onQuiz)
                NativeAdCard()
                if (state.related.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("다른 가이드")
                        state.related.forEach { ArticleCard(it, onClick = { onArticle(it.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(article: Article) = Column {
    PropTile(article.prop, size = 64.dp)
    Spacer(Modifier.height(16.dp))
    Eyebrow("${article.category} · ${article.readMinutes}분 읽기")
    Spacer(Modifier.height(6.dp))
    Text(article.title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(10.dp))
    Text(article.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Section(section: ArticleSection) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(section.heading, style = MaterialTheme.typography.titleLarge)
    val body = MaterialTheme.typography.bodyLarge
    section.body.split("\n\n").filter { it.isNotBlank() }.forEach { Text(it.trim(), style = body) }
    if (section.bullets.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { section.bullets.forEach { Bullet(it) } }
    }
    if (section.colors.isNotEmpty()) SwatchGrid(section.colors, Modifier.padding(top = 4.dp), maxColumns = 6)
}

/** Brand aurora card that hands off to diagnosis (brand 틴티: no tone yet assumed). */
@Composable
private fun CtaCard(onAnalyze: () -> Unit, onQuiz: () -> Unit) {
    PcCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, elevation = 2) {
        Column(Modifier.aurora(PcTheme.colors).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("내 퍼스널컬러가 궁금하다면", style = MaterialTheme.typography.titleLarge)
                    Text("사진이나 질문으로 바로 알 수 있어요", style = Muted)
                }
                Tinty(Modifier.size(72.dp), expression = TintyExpression.Default, animated = false)
            }
            val analyze: @Composable (Modifier) -> Unit = { PcButton("사진으로 진단하기", onAnalyze, it, Icons.Outlined.CameraAlt) }
            val quiz: @Composable (Modifier) -> Unit = { PcOutlinedButton("질문으로 셀프 진단", onQuiz, it, Icons.Outlined.Quiz) }
            if (isWideWindow()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { analyze(Modifier.weight(1f)); quiz(Modifier.weight(1f)) }
            } else {
                analyze(Modifier.fillMaxWidth()); quiz(Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
private fun ArticlePreview() = PcTheme {
    val article = Article(
        "a", "기초", "퍼스널컬러란?", "내 얼굴을 더 생기 있어 보이게 하는 색을 찾는 방법이에요.", 3, "🎨",
        listOf(ArticleSection("4계절과 12톤", "첫 문단이에요.\n\n두 번째 문단이에요.", listOf("온도", "명도", "채도"),
            listOf(NamedColor("코랄", "#FF7F50"), NamedColor("로즈", "#E8A0B8")))),
    )
    ArticleScreen(ArticleUiState("a", article, previewArticles.take(2)), {}, {}, {}, {})
}
