package com.gun.personalcolor.feature.coordi

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gun.personalcolor.core.ads.AdManager
import com.gun.personalcolor.core.ads.LocalAdManager
import com.gun.personalcolor.core.ads.NativeAdCard
import com.gun.personalcolor.core.ads.RewardRequest
import com.gun.personalcolor.core.ads.rememberRewardGate
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcOutlinedButton
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.isWideWindow
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.Swatch
import com.gun.personalcolor.core.ui.Tag
import com.gun.personalcolor.core.weather.TodayWeather
import com.gun.personalcolor.core.weather.WeatherCodes
import com.gun.personalcolor.core.weather.WeatherRepository
import kotlin.math.roundToInt

@Composable
internal fun CoordiRouteScreen(onBack: () -> Unit, onTone: (String) -> Unit, onCloset: () -> Unit, onAnalyze: () -> Unit, vm: CoordiViewModel = hiltViewModel()) {
    TrackScreen("coordi")
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(vm) { vm.onResume(); onPauseOrDispose { } }

    val ads = LocalAdManager.current
    val analytics = LocalAnalytics.current
    val adFree = ads?.billing?.adFree?.collectAsStateWithLifecycle()?.value ?: false
    var watchedAd by rememberSaveable { mutableStateOf(false) }
    // Preview/no ads → unlocked. isUnlocked also covers an ad watched earlier today (until midnight).
    val unlocked = state.trialDaysLeft != null || adFree || watchedAd || (ads?.isUnlocked(AdManager.KEY_COORDI) ?: true)
    val gate = rememberRewardGate()

    CoordiScreen(
        state = state,
        unlocked = unlocked,
        showTrial = !adFree,
        onBack = onBack,
        onTone = onTone,
        onCloset = onCloset,
        onAnalyze = onAnalyze,
        onLocationResult = { vm.refreshWeather() },
        onUnlock = {
            gate(RewardRequest(AdManager.KEY_COORDI, "광고 보고 오늘의 코디 보기", "짧은 광고 하나만 보면 무료예요.", AdManager.untilMidnightMs()) {
                watchedAd = true
                analytics?.log("coordi_unlock")
            })
        },
        onNext = {
            gate(RewardRequest(AdManager.KEY_COORDI, "광고 보고 다른 코디 보기", "짧은 광고 하나만 보면 무료예요.", AdManager.untilMidnightMs()) {
                watchedAd = true
                vm.nextOutfit()
            })
        },
    )
}

@Composable
internal fun CoordiScreen(
    state: CoordiUiState,
    unlocked: Boolean,
    showTrial: Boolean,
    onBack: () -> Unit,
    onTone: (String) -> Unit,
    onCloset: () -> Unit,
    onAnalyze: () -> Unit,
    onLocationResult: () -> Unit,
    onUnlock: () -> Unit,
    onNext: () -> Unit,
) {
    PcScaffold("오늘의 코디", onBack) { padding ->
        val tone = state.tone
        when {
            tone == null && !state.loading -> EmptyState(
                Icons.Outlined.Palette, "내 퍼스널컬러를 먼저 알려 주세요",
                "내 톤과 날씨에 맞는 코디를 매일 추천해요.",
                Modifier.padding(padding),
            ) { PcButton("퍼스널컬러 진단하기", onAnalyze) }
            else -> Content(state, tone, unlocked, showTrial, padding, onTone, onCloset, onLocationResult, onUnlock, onNext)
        }
    }
}

@Composable
private fun Content(
    state: CoordiUiState,
    tone: Tone?,
    unlocked: Boolean,
    showTrial: Boolean,
    padding: PaddingValues,
    onTone: (String) -> Unit,
    onCloset: () -> Unit,
    onLocationResult: () -> Unit,
    onUnlock: () -> Unit,
    onNext: () -> Unit,
) {
    val wide = isWideWindow()
    val outfit = state.outfit
    val top: @Composable ColumnScope.() -> Unit = {
        WeatherCard(state.weather, state.outfit?.layerLine)
        if (!state.weather.hasPermission && !state.weather.loading) LocationCard(onLocationResult)
        if (showTrial && state.trialDaysLeft != null) {
            Text("무료 체험 ${state.trialDaysLeft}일 남음", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
    }
    val main: @Composable ColumnScope.() -> Unit = {
        when {
            outfit == null || tone == null -> LoadingCard()
            !unlocked -> LockedCard(onUnlock)
            else -> {
                OutfitCard(outfit, tone) { onTone(tone.id) }
                MakeupCard(outfit)
                PointCard(outfit)
                TipCard(outfit.tip)
                PcButton("다른 코디 보기", onNext, Modifier.fillMaxWidth(), icon = Icons.Outlined.Refresh)
                PcOutlinedButton("내 옷장", onCloset, Modifier.fillMaxWidth(), icon = Icons.Outlined.Checkroom)
            }
        }
        NativeAdCard()
        Text("날씨 데이터: Open-Meteo.com", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val inner = Modifier.readableWidth(if (wide) 1040.dp else 680.dp)
            .padding(horizontal = ScreenPadding).padding(top = 8.dp, bottom = 24.dp)
        if (wide) {
            Row(inner, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = top)
                Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(16.dp), content = main)
            }
        } else {
            Column(inner, verticalArrangement = Arrangement.spacedBy(16.dp)) { top(); main() }
        }
    }
}

// --- Weather ---

private fun weatherIcon(key: String): ImageVector = when (key) {
    WeatherCodes.CLEAR -> Icons.Outlined.WbSunny
    WeatherCodes.PARTLY -> Icons.Outlined.FilterDrama
    WeatherCodes.FOG -> Icons.Outlined.Dehaze
    WeatherCodes.DRIZZLE, WeatherCodes.SHOWER -> Icons.Outlined.Grain
    WeatherCodes.RAIN -> Icons.Outlined.Umbrella
    WeatherCodes.SNOW -> Icons.Outlined.AcUnit
    WeatherCodes.THUNDER -> Icons.Outlined.Thunderstorm
    else -> Icons.Outlined.Cloud
}

@Composable
private fun WeatherCard(ws: WeatherState, layerLine: String?) {
    val w = ws.weather
    SectionCard(null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                val icon = w?.let { weatherIcon(it.icon) } ?: Icons.Outlined.Thermostat
                Icon(icon, null, Modifier.padding(12.dp).size(28.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    w?.placeLabel ?: if (w != null) "내 주변 날씨" else "오늘의 날씨",
                    style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val sub = when {
                    ws.loading -> "날씨를 불러오는 중이에요"
                    w == null && ws.hasPermission -> "날씨를 불러오지 못했어요 · 계절 기준 추천"
                    w == null -> "계절 기준으로 추천해요"
                    WeatherRepository.isStale(w) -> "${w.condition} · 최근 정보"
                    else -> w.condition
                }
                Text(sub, style = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (ws.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else if (w != null) {
                Spacer(Modifier.width(12.dp))
                Text("${w.temp.roundToInt()}°", style = MaterialTheme.typography.displaySmall, maxLines = 1)
            }
        }
        if (w != null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("체감 ${w.feelsLike.roundToInt()}°")
                Tag("최저 ${w.min.roundToInt()}° · 최고 ${w.max.roundToInt()}°")
                Tag("강수확률 ${w.precipProb}%")
                w.uv?.let { Tag("자외선 ${uvLabel(it)}") }
            }
        }
        if (layerLine != null) Text(layerLine, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun uvLabel(uv: Double) = when {
    uv >= 8 -> "매우 높음"
    uv >= 6 -> "높음"
    uv >= 3 -> "보통"
    else -> "낮음"
}

@Composable
private fun LocationCard(onResult: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        onResult()
    }
    // After a denial without rationale, the system won't show the dialog again — send the user to app settings.
    val blocked = denied && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
    SectionCard(null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text("내 위치 날씨로 추천받기", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            if (denied) "지금은 계절 기준으로 추천하고 있어요."
            else "대략적인 위치로 오늘 날씨에 맞는 코디를 골라요.",
            style = Muted,
        )
        PcOutlinedButton(
            if (blocked) "설정에서 위치 허용" else "위치 허용하기",
            onClick = {
                if (blocked) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
                } else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --- Outfit ---

@Composable
private fun OutfitCard(outfit: Outfit, tone: Tone, onTone: () -> Unit) {
    SectionCard("오늘의 코디", action = { TextButton(onTone) { Text(tone.nameKo, maxLines = 1, overflow = TextOverflow.Ellipsis) } }) {
        Text(outfit.headline, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        outfit.slots.forEach { SlotRow(it) }
        if (outfit.extras.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("챙길 것", style = Muted, maxLines = 1)
                outfit.extras.forEach { Tag(it, color = MaterialTheme.colorScheme.secondaryContainer) }
            }
        }
    }
}

@Composable
private fun SlotRow(slot: OutfitSlot) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Swatch(slot.color.hex, Modifier.size(48.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(slot.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(slot.item, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (slot.fromCloset) { Spacer(Modifier.width(8.dp)); Tag("내 옷장", color = MaterialTheme.colorScheme.primaryContainer) }
    }
}

@Composable
private fun MakeupCard(outfit: Outfit) {
    SectionCard("메이크업") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorCell("립", outfit.lip, Modifier.weight(1f))
            ColorCell("아이", outfit.eye, Modifier.weight(1f))
            ColorCell("블러셔", outfit.blush, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColorCell(label: String, color: NamedColor, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Swatch(color.hex, Modifier.size(44.dp), CircleShape)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(color.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PointCard(outfit: Outfit) {
    SectionCard("포인트") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Swatch(outfit.moodColor.hex, Modifier.size(40.dp), CircleShape)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("이달의 무드 컬러", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(outfit.moodColor.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(Icons.Outlined.Palette, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("액세서리", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text("${outfit.metal} 톤 액세서리", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TipCard(tip: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.fillMaxWidth().padding(20.dp)) {
            Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("오늘의 팁", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    SectionCard(null) {
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

/** Teaser after the trial: placeholder outfit (no real data, so nothing leaks where blur is unsupported) + unlock. */
@Composable
private fun LockedCard(onUnlock: () -> Unit) {
    SectionCard("오늘의 코디") {
        Box {
            Column(Modifier.blur(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)))
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.fillMaxWidth(0.6f).height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)))
                    }
                }
            }
            Surface(Modifier.align(Alignment.Center), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Outlined.Lock, null, Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text("오늘의 코디가 준비됐어요", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("짧은 광고 하나만 보면 무료예요.", style = Muted)
        PcButton("광고 보고 오늘의 코디 보기", onUnlock, Modifier.fillMaxWidth())
        Text("프리미엄은 광고 없이 무제한", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center)
    }
}

// --- Previews ---

private val previewOutfit = Outfit(
    layerLine = "체감 14°C · 자켓·트렌치 날씨",
    slots = listOf(
        OutfitSlot("아우터", NamedColor("카멜", "#C19A6B"), "카멜 트렌치코트"),
        OutfitSlot("상의", NamedColor("웜 아이보리", "#F3E9D2"), "내 옷장의 '아이보리 니트'", fromCloset = true),
        OutfitSlot("하의", NamedColor("초콜릿 브라운", "#5A3A2A"), "초콜릿 브라운 슬랙스"),
        OutfitSlot("포인트", NamedColor("머스터드", "#D2A23A"), "머스터드 스카프"),
    ),
    lip = NamedColor("브릭", "#A0453A"), lipFromCloset = false,
    eye = NamedColor("브론즈", "#8C6239"), blush = NamedColor("테라코타", "#C0674A"),
    metal = "골드", moodColor = NamedColor("카멜 브라운", "#A9794C"),
    extras = listOf("선글라스"), tip = "일교차가 커요. 벗고 입기 쉬운 겉옷을 챙기세요.",
    headline = "카멜 트렌치코트 + 아이보리 니트",
)

private val previewTone = Tone("autumn_true", "autumn", "가을 웜", "Autumn True", emptyList(), "", "", emptyList(), emptyList(),
    com.gun.personalcolor.core.model.Makeup("", emptyList(), "", emptyList(), "", emptyList(), ""),
    com.gun.personalcolor.core.model.Hair(emptyList(), ""), com.gun.personalcolor.core.model.Fashion("", emptyList()),
    com.gun.personalcolor.core.model.Accessory("골드", ""), "")

private val previewWeather = TodayWeather("마포구 망원동", 16.2, 14.0, 9.0, 19.0, 20, "구름 조금", 2, 4.0, 8.0, System.currentTimeMillis())

@Preview
@Composable
private fun CoordiPreview() = PcTheme {
    CoordiScreen(
        CoordiUiState(false, previewTone, WeatherState(false, previewWeather, true), previewOutfit, 5),
        unlocked = true, showTrial = true, {}, {}, {}, {}, {}, {}, {},
    )
}

@Preview
@Composable
private fun CoordiLockedPreview() = PcTheme(darkTheme = true) {
    CoordiScreen(
        CoordiUiState(false, previewTone, WeatherState(false, null, false), previewOutfit, null),
        unlocked = false, showTrial = true, {}, {}, {}, {}, {}, {}, {},
    )
}
