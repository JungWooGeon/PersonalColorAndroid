package com.gun.personalcolor.feature.tones

import com.gun.personalcolor.core.ads.NativeAdCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.isExpandedWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.ui.ToneDetails
import com.gun.personalcolor.core.ui.ToneHeader

@Composable
fun ToneDetailScreen(
    state: TonesUiState,
    id: String,
    onBack: () -> Unit,
    onTone: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onSetMine: () -> Unit,
    onCompare: () -> Unit,
) {
    TrackScreen("tone_detail")
    val analytics = LocalAnalytics.current
    LaunchedEffect(id) { analytics?.log("tone_view", "tone" to id) }
    val tone = state.tones.firstOrNull { it.id == id } ?: return
    val isMine = state.myToneId == id

    PcScaffold(
        tone.nameKo, onBack,
        actions = { FavoriteButton(id in state.favorites, onToggleFavorite) },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad)
                .readableWidth(if (isExpandedWindow()) 1040.dp else 680.dp)
                .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ToneHeader(tone)
            ButtonPair(
                stackBelow = 320.dp,
                first = {
                    if (isMine) PcButton("내 톤이에요", {}, it, icon = Icons.Filled.Check, enabled = false)
                    else PcButton("내 톤으로 설정", onSetMine, it)
                },
                second = { PcOutlinedButton("다른 톤과 비교", onCompare, it) },
            )
            ToneDetails(tone)
            NativeAdCard()
            SectionCard("비슷한 톤") {
                Column {
                    similarTones(tone, state.tones).forEach { ToneRow(it, onClick = { onTone(it.id) }) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ToneDetailPreview() = PcTheme {
    ToneDetailScreen(TonesUiState(previewTones()), "spring_light", {}, {}, {}, {}, {})
}
