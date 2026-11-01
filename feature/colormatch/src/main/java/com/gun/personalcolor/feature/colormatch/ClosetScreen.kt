package com.gun.personalcolor.feature.colormatch

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import java.io.File
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.CLOSET_CATEGORIES
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.ui.ToneAvatar
import com.gun.personalcolor.core.ui.seasonKo
import com.gun.personalcolor.core.ui.temperatureKo

@Composable
fun ClosetRoute(onBack: () -> Unit, onColorMatch: () -> Unit, vm: ClosetViewModel = hiltViewModel()) {
    TrackScreen("closet")
    val state by vm.state.collectAsStateWithLifecycle()
    // The add flow is a full-screen state over the closet (no extra route, so the app's nav graph stays as is).
    var adding by rememberSaveable { mutableStateOf(false) }
    if (adding) ClosetAddRoute(onClose = { adding = false })
    else ClosetScreen(
        state, onBack, onColorMatch, onAdd = { adding = true }, imageFile = vm::imageFile,
        onUpdate = vm::update, onDelete = vm::delete, onClear = vm::clear,
    )
}

@Composable
fun ClosetScreen(
    state: ClosetUiState,
    onBack: () -> Unit,
    onColorMatch: () -> Unit,
    onAdd: () -> Unit,
    imageFile: (String) -> File,
    onUpdate: (ClosetItem) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var levelFilter by rememberSaveable { mutableStateOf<MatchLevel?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    PcScaffold("내 옷장", onBack, actions = {
        if (state.entries.isNotEmpty()) Box {
            IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "더보기") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("전체 삭제") }, { menu = false; confirmClear = true }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) })
            }
        }
    }) { pad ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) { CircularProgressIndicator() }
            state.entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
                EmptyState(Icons.Rounded.Checkroom, "옷장이 비어 있어요", "내 옷을 찍어 두면 어울리는지 한눈에 볼 수 있어요", action = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PcButton("옷 추가", onAdd, icon = Icons.Rounded.Add)
                        TextButton(onColorMatch, Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Rounded.Colorize, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("컬러 체크하기", maxLines = 1)
                        }
                    }
                })
            }
            else -> Box(Modifier.fillMaxSize()) {
                val categories = CLOSET_CATEGORIES.filter { c -> state.entries.any { it.item.category == c } }
                val shown = state.entries.filter {
                    (levelFilter == null || it.level == levelFilter) && (categoryFilter == null || it.item.category == categoryFilter)
                }
                val dir = LocalLayoutDirection.current
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.fillMaxSize().readableWidth(960.dp),
                    contentPadding = PaddingValues(
                        start = pad.calculateStartPadding(dir) + ScreenPadding,
                        end = pad.calculateEndPadding(dir) + ScreenPadding,
                        top = pad.calculateTopPadding(),
                        bottom = pad.calculateBottomPadding() + 96.dp, // room for the FAB
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ChipRow {
                                FilterChip(levelFilter == null, { levelFilter = null }, { Text("전체", maxLines = 1) })
                                MatchLevel.entries.forEach { l ->
                                    FilterChip(levelFilter == l, { levelFilter = if (levelFilter == l) null else l }, { Text(l.label, maxLines = 1) })
                                }
                            }
                            if (categories.size > 1) ChipRow {
                                FilterChip(categoryFilter == null, { categoryFilter = null }, { Text("모든 종류", maxLines = 1) })
                                categories.forEach { c ->
                                    FilterChip(categoryFilter == c, { categoryFilter = if (categoryFilter == c) null else c }, { Text(c, maxLines = 1) })
                                }
                            }
                            val count = { l: MatchLevel -> state.entries.count { it.level == l } }
                            Text(
                                "어울림 ${count(MatchLevel.GOOD)} · 무난 ${count(MatchLevel.NEUTRAL)} · 피함 ${count(MatchLevel.AVOID)}",
                                style = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                    if (shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("조건에 맞는 옷이 없어요", style = Muted, modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    // One in-feed ad after the first 6 tiles (or after all of them if fewer).
                    items(shown.take(6), key = { it.item.id }) { e -> ClosetTile(e, imageFile) { selectedId = e.item.id } }
                    if (shown.isNotEmpty()) item(key = "ad", span = { GridItemSpan(maxLineSpan) }) { NativeAdCard() }
                    items(shown.drop(6), key = { it.item.id }) { e -> ClosetTile(e, imageFile) { selectedId = e.item.id } }
                }
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("옷 추가", maxLines = 1) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = pad.calculateEndPadding(dir) + ScreenPadding, bottom = pad.calculateBottomPadding() + 16.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }

    state.entries.firstOrNull { it.item.id == selectedId }?.let { e ->
        ItemSheet(
            e, imageFile,
            onUpdate = onUpdate,
            onDelete = { onDelete(e.item.id); selectedId = null },
            onDismiss = { selectedId = null },
        )
    }

    if (confirmClear) ConfirmDialog(
        title = "옷장을 비울까요?",
        message = "저장한 옷 ${state.entries.size}개가 모두 지워져요.",
        confirm = "전체 삭제",
        onConfirm = { onClear(); confirmClear = false },
        onDismiss = { confirmClear = false },
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun ClosetTile(e: ClosetEntry, imageFile: (String) -> File, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                Alignment.Center,
            ) {
                val photo = e.item.imageFile?.let(imageFile)
                GarmentThumb(e.item, photo, if (photo != null) Modifier.fillMaxSize().padding(10.dp) else Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(2.dp))
            Text(e.item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(e.item.hex, Modifier.size(12.dp), CircleShape)
                Spacer(Modifier.width(6.dp))
                Text(kindLabel(e.item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${e.score}점", style = MaterialTheme.typography.labelLarge, color = levelColors(e.level).accent, maxLines = 1)
            }
            VerdictBadge(e.level, small = true)
            if (e.reEvaluated) Text("현재 톤 기준", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ItemSheet(e: ClosetEntry, imageFile: (String) -> File, onUpdate: (ClosetItem) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val item = e.item
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var kind by rememberSaveable(item.id) { mutableStateOf(item.kind) }
    var confirmDelete by remember { mutableStateOf(false) }
    val changed = name.isNotBlank() && (name.trim() != item.name || kind != item.kind)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.readableWidth().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPadding).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val photo = item.imageFile?.let(imageFile)
            if (photo != null) Box(
                Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                Alignment.Center,
            ) { GarmentThumb(item, photo, Modifier.fillMaxSize().padding(16.dp), maxSide = 1024) }
            else GarmentThumb(item, null, Modifier.fillMaxWidth().height(140.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(item.hex, Modifier.size(28.dp), CircleShape)
                Spacer(Modifier.width(10.dp))
                Text(item.hex, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
                Text("${e.score}점", style = MaterialTheme.typography.titleMedium, color = levelColors(e.level).accent, maxLines = 1)
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it.take(30) }, label = { Text("이름") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text("종류", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            KindChips(kind) { kind = it }
            if (changed) PcButton("변경 저장", {
                onUpdate(item.copy(name = name.trim(), kind = kind, category = kind?.let { CLOTHING_KINDS[it]?.second } ?: item.category))
            }, Modifier.fillMaxWidth())

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictBadge(e.level)
                    Spacer(Modifier.width(8.dp))
                    e.refTone?.let { Text("${it.nameKo} 기준", style = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                e.verdict?.let { Text(it.reason, style = MaterialTheme.typography.bodyLarge) }
            }

            e.verdict?.topTones?.takeIf { it.isNotEmpty() }?.let { top ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("이 색이 어울리는 톤", style = MaterialTheme.typography.titleSmall)
                    top.forEachIndexed { i, m ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                            ToneAvatar(m.tone, 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.tone.nameKo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${m.tone.seasonKo} · ${m.tone.temperatureKo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (m.tone.id == e.refTone?.id) Tag("기준 톤", color = MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }
            }

            PcOutlinedButton("삭제", { confirmDelete = true }, Modifier.fillMaxWidth(), icon = Icons.Outlined.DeleteOutline)
        }
    }

    if (confirmDelete) ConfirmDialog(
        title = "삭제할까요?",
        message = if (item.imageFile != null) "'${item.name}' 항목과 사진이 옷장에서 지워져요." else "'${item.name}' 항목이 옷장에서 지워져요.",
        confirm = "삭제",
        onConfirm = { confirmDelete = false; onDelete() },
        onDismiss = { confirmDelete = false },
    )
}

/** Name field + category chips, shared by the save sheet and the item sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClosetFields(name: String, onName: (String) -> Unit, category: String, onCategory: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { onName(it.take(30)) }, label = { Text("이름") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Text("종류", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CLOSET_CATEGORIES.forEach { c -> FilterChip(c == category, { onCategory(c) }, { Text(c, maxLines = 1) }) }
        }
    }
}

private fun kindLabel(item: ClosetItem) = item.kind?.let { CLOTHING_KINDS[it]?.first } ?: item.category

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun ClosetPreview() = PcTheme {
    val items = listOf(
        ClosetItem(1, "#C0603A", "테라코타 니트", "상의", "autumn_mute", Verdict.GOOD, 88, kind = "knit"),
        ClosetItem(2, "#8A7650", "올리브 하의", "하의", "autumn_mute", Verdict.OK, 55),
        ClosetItem(3, "#FF2D95", "핫핑크 립·메이크업", "립·메이크업", "autumn_mute", Verdict.AVOID, 20),
    )
    ClosetScreen(
        ClosetUiState(false, items.map { ClosetEntry(it, MatchLevel.of(it.verdict), it.score, it.id == 2L, null, null) }),
        onBack = {}, onColorMatch = {}, onAdd = {}, imageFile = { File(it) }, onUpdate = {}, onDelete = {}, onClear = {},
    )
}
