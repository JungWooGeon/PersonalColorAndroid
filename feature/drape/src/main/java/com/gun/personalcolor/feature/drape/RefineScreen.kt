package com.gun.personalcolor.feature.drape

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analysis.ToneScore
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.designsystem.BottomActions
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.domain.DiagnosisSession
import com.gun.personalcolor.core.domain.DrapeRefiner
import com.gun.personalcolor.core.domain.RefineAnswer
import com.gun.personalcolor.core.domain.RefineChoice
import com.gun.personalcolor.core.domain.RefineDiagnosisUseCase
import com.gun.personalcolor.core.domain.RefineRound
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.vision.FacePhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class RefineUiState(
    val round: RefineRound? = null,
    val answers: List<RefineAnswer> = emptyList(),
    val saving: Boolean = false,
    /** Set when finished: photo-only top tone → refined top tone. */
    val result: Pair<ToneScore, ToneScore>? = null,
)

@HiltViewModel
class RefineViewModel @Inject constructor(
    session: DiagnosisSession,
    val content: ContentRepository,
    private val refine: RefineDiagnosisUseCase,
) : ViewModel() {
    // Snapshot: publishing the refined result replaces session.current.
    val base = session.current.value?.takeIf { it.photo != null }
    private val _state = MutableStateFlow(RefineUiState(round = base?.let { DrapeRefiner.nextRound(it.scores, emptyList(), content.tones) }))
    val state = _state.asStateFlow()

    fun answer(choice: RefineChoice) {
        val s = _state.value
        val round = s.round ?: return
        val answers = s.answers + RefineAnswer(round, choice)
        val next = DrapeRefiner.nextRound(base!!.scores, answers, content.tones)
        _state.value = s.copy(round = next, answers = answers)
        if (next == null) finish()
    }

    fun previous() = _state.update { s ->
        if (s.answers.isEmpty() || s.saving || s.result != null) s
        else s.copy(round = s.answers.last().round, answers = s.answers.dropLast(1))
    }

    /** Also the "건너뛰기" action: finish with the answers so far. */
    fun finish() {
        val b = base ?: return
        if (_state.value.saving || _state.value.result != null) return
        _state.update { it.copy(saving = true, round = null) }
        viewModelScope.launch {
            val d = refine(b, _state.value.answers)
            _state.update { it.copy(saving = false, result = b.top to d.top) }
        }
    }
}

/** 정밀 진단: pairwise drape comparisons on the diagnosis photo, folded into the photo scores. Tool screen: no ads. */
@Composable
internal fun RefineRoute(onBack: () -> Unit, onDone: () -> Unit, vm: RefineViewModel = hiltViewModel()) {
    TrackScreen("refine")
    val photo = vm.base?.photo ?: run { LaunchedEffect(Unit) { onBack() }; return }
    val state by vm.state.collectAsStateWithLifecycle()
    val name: (String) -> String = { vm.content.toneOrNull(it)?.nameKo ?: it }
    RefineScreen(photo, state, name, onBack, onDone, vm::answer, vm::previous, vm::finish)
}

@Composable
internal fun RefineScreen(
    photo: FacePhoto,
    state: RefineUiState,
    toneName: (String) -> String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onAnswer: (RefineChoice) -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
) {
    val inRounds = state.round != null
    val back = { if (inRounds && state.answers.isNotEmpty()) onPrevious() else if (state.result != null) onDone() else onBack() }
    BackHandler(onBack = back)
    PcScaffold(
        title = "정밀 진단",
        onBack = back,
        actions = {
            if (inRounds && state.answers.isNotEmpty()) TextButton(onClick = onSkip) { Text("건너뛰기", maxLines = 1) }
        },
    ) { pad ->
        val result = state.result
        when {
            result != null -> Done(result, toneName, onDone, Modifier.padding(pad))
            state.round != null -> Round(photo, state.round, Modifier.padding(pad), onAnswer)
            else -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("결과를 정리하고 있어요", style = Muted)
            }
        }
    }
}

@Composable
private fun Round(photo: FacePhoto, round: RefineRound, modifier: Modifier, onAnswer: (RefineChoice) -> Unit) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.readableWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(
                progress = { (round.step + 1f) / DrapeRefiner.TOTAL_STEPS },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("${round.step + 1} / ${DrapeRefiner.TOTAL_STEPS}", style = Muted, maxLines = 1)
            Text("어느 쪽 얼굴이 더 화사하고 맑아 보여요?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        // Photos fill the space between the question and the buttons. Narrow + tall → stacked, otherwise side by side.
        BoxWithConstraints(Modifier.weight(1f).readableWidth(960.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            val stacked = maxWidth < 340.dp && maxHeight > maxWidth
            val a: @Composable (Modifier) -> Unit = { m -> Side("A", photo, round.a.hex, m) { onAnswer(RefineChoice.A) } }
            val b: @Composable (Modifier) -> Unit = { m -> Side("B", photo, round.b.hex, m) { onAnswer(RefineChoice.B) } }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { a(Modifier.weight(1f).fillMaxWidth()); b(Modifier.weight(1f).fillMaxWidth()) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { a(Modifier.weight(1f).fillMaxSize()); b(Modifier.weight(1f).fillMaxSize()) }
            }
        }
        BottomActions {
            Row(Modifier.readableWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PcButton("A", { onAnswer(RefineChoice.A) }, Modifier.weight(1f))
                PcOutlinedButton("비슷해요", { onAnswer(RefineChoice.SAME) }, Modifier.weight(1.3f))
                PcButton("B", { onAnswer(RefineChoice.B) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Side(label: String, photo: FacePhoto, hex: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClickLabel = "$label 선택", onClick = onClick)) {
        CapePhoto(photo, hexColor(hex), Modifier.fillMaxSize(), faceCrop = true)
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Done(result: Pair<ToneScore, ToneScore>, toneName: (String) -> String, onDone: () -> Unit, modifier: Modifier) {
    val (before, after) = result
    fun pct(s: ToneScore) = "${(s.probability * 100).roundToInt()}%"
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).readableWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("정밀 진단 완료", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                if (before.toneId == after.toneId) "드레이핑 결과도 같은 톤을 가리켜요" else "드레이핑 결과로 톤이 바뀌었어요",
                style = Muted, textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToneChip("사진", toneName(before.toneId), pct(before), Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                ToneChip("정밀 진단", toneName(after.toneId), pct(after), Modifier.weight(1f), highlight = true)
            }
        }
        BottomActions {
            PcButton("결과 보기", onDone, Modifier.readableWidth().padding(horizontal = 20.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun ToneChip(caption: String, name: String, pct: String, modifier: Modifier, highlight: Boolean = false) {
    val bg = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.clip(MaterialTheme.shapes.large).background(bg).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(caption, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(pct, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = fg, maxLines = 1)
    }
}
