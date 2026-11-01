package com.gun.personalcolor.feature.learn

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable data object LearnRoute
@Serializable data class ArticleRoute(val id: String)

/** 퍼스널컬러 가이드: article list + article detail. */
fun NavGraphBuilder.learnScreens(onBack: () -> Unit, onArticle: (String) -> Unit, onAnalyze: () -> Unit, onQuiz: () -> Unit) {
    composable<LearnRoute> { LearnScreenRoute(onBack, onArticle) }
    composable<ArticleRoute> { ArticleScreenRoute(onBack, onArticle, onAnalyze, onQuiz) }
}
