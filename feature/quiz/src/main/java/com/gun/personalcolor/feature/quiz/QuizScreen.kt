package com.gun.personalcolor.feature.quiz

import androidx.compose.runtime.LaunchedEffect
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.TintyProp
import com.gun.personalcolor.core.designsystem.Tinty
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.StrokeCap
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val done by vm.done.collectAsStateWithLifecycle()
    LaunchedEffect(done) {
        if (!done) return@LaunchedEffect
        vm.resultShown()
        ads?.onResult(activity, onResult) ?: onResult()
    }
    val back = { if (!vm.undo()) onBack() }
    BackHandler(onBack = back)
    QuizScreen(vm.questions, answers.size, back, vm::answer)
}

@Composable
internal fun QuizScreen(questions: List<QuizQuestion>, answered: Int, onBack: () -> Unit, onAnswer: (QuizOption) -> Unit) {
    PcScaffold("셀프 진단", onBack) { pad ->
        val index = answered.coerceAtMost(questions.lastIndex)
        Column(Modifier.fillMaxSize().padding(pad).readableWidth(600.dp).padding(horizontal = ScreenPadding)) {
            val progress by animateFloatAsState(answered / questions.size.coerceAtLeast(1).toFloat(), tween(PcMotion.BASE, easing = PcMotion.Standard), label = "progress")
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text("${index + 1} / ${questions.size}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
            }
            // All answered: the result (and maybe an ad) is on its way.
            if (answered >= questions.size) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { TintyLoading(message = "결과를 정리하고 있어요", size = 156.dp, prop = TintyProp.Cards) }
            } else {
                AnimatedContent(
                    index,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
                    },
                    label = "question",
                ) { i -> Question(i, questions[i], onAnswer) }
            }
        }
    }
}

@Composable
private fun Question(index: Int, q: QuizQuestion, onAnswer: (QuizOption) -> Unit) {
    // Tap shows the pick for a beat before moving on, so the choice is visible.
    var picked by remember(q) { mutableStateOf<QuizOption?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("질문 ${index + 1}")
                Text(q.text, style = MaterialTheme.typography.headlineSmall)
            }
            // Same 틴티 on every question (still), holding the question cards.
            Tinty(Modifier.size(84.dp), expression = if (index % 3 == 2) TintyExpression.Happy else TintyExpression.Thinking, animated = false, prop = TintyProp.Cards)
        }
        if (q.hint.isNotBlank()) Text(q.hint, style = Muted)
        Spacer(Modifier.height(8.dp))
        q.options.forEachIndexed { i, o ->
            val selected = picked == o
            val cs = MaterialTheme.colorScheme
            PcCard(
                Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .then(if (selected) Modifier.border(2.dp, cs.primary, MaterialTheme.shapes.large) else Modifier),
                onClick = {
                    if (picked == null) { picked = o; scope.launch { delay(PcMotion.FAST.toLong()); onAnswer(o) } }
                },
                color = if (selected) cs.primaryContainer else cs.surfaceContainer,
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).background(if (selected) cs.primary else cs.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${'A' + i}", style = MaterialTheme.typography.labelLarge,
                            color = if (selected) cs.onPrimary else cs.onPrimaryContainer)
                    }
                    Text(o.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp),
                        color = if (selected) cs.onPrimaryContainer else cs.onSurface)
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
