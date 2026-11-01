package com.gun.personalcolor.feature.quiz

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.LocalAdManager
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.TintyLoading
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.QuizOption
import com.gun.personalcolor.core.model.QuizQuestion

@Composable
internal fun QuizRoute(onBack: () -> Unit, onResult: () -> Unit, vm: QuizViewModel = hiltViewModel()) {
    TrackScreen("quiz")
    val answers by vm.answers.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val ads = LocalAdManager.current
    val back = { if (!vm.undo()) onBack() }
    BackHandler(onBack = back)
    QuizScreen(vm.questions, answers.size, back) { o ->
        vm.answer(o) { ads?.onResult(activity, onResult) ?: onResult() }
    }
}

@Composable
internal fun QuizScreen(questions: List<QuizQuestion>, answered: Int, onBack: () -> Unit, onAnswer: (QuizOption) -> Unit) {
    PcScaffold("셀프 진단", onBack) { pad ->
        val index = answered.coerceAtMost(questions.lastIndex)
        Column(Modifier.fillMaxSize().padding(pad).readableWidth(600.dp).padding(horizontal = ScreenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { answered / questions.size.toFloat() },
                    modifier = Modifier.weight(1f).height(6.dp),
                    drawStopIndicator = {},
                )
                Text("${index + 1} / ${questions.size}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
            }
            // All answered: the result (and maybe an ad) is on its way.
            if (answered >= questions.size) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { TintyLoading(message = "결과를 정리하고 있어요") }
            } else {
                AnimatedContent(
                    index,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                    },
                    label = "question",
                ) { i -> Question(questions[i], onAnswer) }
            }
        }
    }
}

@Composable
private fun Question(q: QuizQuestion, onAnswer: (QuizOption) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(q.text, style = MaterialTheme.typography.headlineSmall)
        if (q.hint.isNotBlank()) Text(q.hint, style = Muted)
        Spacer(Modifier.height(8.dp))
        q.options.forEachIndexed { i, o ->
            Surface(
                onClick = { onAnswer(o) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                        Text("${'A' + i}", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(o.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun QuizPreview() = PcTheme {
    QuizScreen(listOf(QuizQuestion("q", "햇볕에 오래 있으면 피부가 어떻게 되나요?", "평소를 떠올려 보세요", listOf(QuizOption("빨갛게 달아올라요", -1.0, 0.0, 0.0), QuizOption("까맣게 타요", 1.0, 0.0, 0.0)))), 0, {}, {})
}
