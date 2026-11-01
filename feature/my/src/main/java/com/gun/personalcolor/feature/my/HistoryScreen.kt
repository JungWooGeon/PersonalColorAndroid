package com.gun.personalcolor.feature.my

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.HistoryEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: MyUiState,
    onBack: () -> Unit,
    onTone: (String) -> Unit,
    onAnalyze: () -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
) {
    TrackScreen("history")
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val entries = state.entries

    PcScaffold(
        "진단 기록", onBack,
        actions = { if (entries.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("전체 삭제", maxLines = 1) } },
    ) { pad ->
        when {
            state.loading -> Unit
            entries.isEmpty() -> EmptyState(
                Icons.Outlined.History, "아직 진단 기록이 없어요", "진단 결과는 이 기기에만 저장돼요.",
                Modifier.padding(pad),
                action = { PcButton("지금 진단하기", onAnalyze) },
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().readableWidth(),
                contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp),
            ) {
                // One in-feed ad near the top (after the newest month), never on an empty list.
                var adPlaced = false
                entries.groupBy { it.dateTime().format(MONTH) }.forEach { (month, group) ->
                    stickyHeader(key = month) {
                        Text(month, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = ScreenPadding, vertical = 10.dp))
                    }
                    items(group, key = { it.timeMillis }) { e ->
                        SwipeRow(e, state, onTone = { onTone(e.toneId) }, onRequestDelete = { pendingDelete = e.timeMillis })
                    }
                    if (!adPlaced) {
                        adPlaced = true
                        item(key = "ad") { NativeAdCard(Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp)) }
                    }
                }
            }
        }
    }

    if (confirmClear) ConfirmDialog("기록을 모두 삭제할까요?", "삭제한 기록은 되돌릴 수 없어요.", "전체 삭제",
        onConfirm = { onClear(); confirmClear = false }, onDismiss = { confirmClear = false })
    pendingDelete?.let { time ->
        ConfirmDialog("이 기록을 삭제할까요?", "삭제한 기록은 되돌릴 수 없어요.", "삭제",
            onConfirm = { onDelete(time); pendingDelete = null }, onDismiss = { pendingDelete = null })
    }
}

/** Swipe left to delete (asks for confirmation, so the row springs back if cancelled). */
@Composable
private fun SwipeRow(e: HistoryEntry, state: MyUiState, onTone: () -> Unit, onRequestDelete: () -> Unit) {
    val tone = state.tones.getValue(e.toneId)
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        onDismiss = { onRequestDelete(); scope.launch { swipe.reset() } },
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("삭제") { onRequestDelete(); true })
        },
        backgroundContent = {
            if (swipe.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
    ) {
        ToneEntryRow(
            tone, "${e.source.label} · ${e.dateTime().format(DAY_TIME)}", onTone,
            Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 4.dp),
            extra = e.runnersUp(state.tones),
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )

@Preview
@Composable
private fun HistoryPreview() = PcTheme { HistoryScreen(previewState(true), {}, {}, {}, {}, {}) }
