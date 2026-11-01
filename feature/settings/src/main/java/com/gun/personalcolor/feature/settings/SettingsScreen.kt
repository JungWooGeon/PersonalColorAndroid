package com.gun.personalcolor.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalConfiguration
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.gun.personalcolor.core.ads.LocalAdManager
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.HistoryRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.DropShape
import com.gun.personalcolor.core.designsystem.Eyebrow
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.PcButton
import com.gun.personalcolor.core.designsystem.PcCard
import com.gun.personalcolor.core.designsystem.PcSnackbarHost
import com.gun.personalcolor.core.designsystem.Tinty
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.designsystem.aurora
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Check
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.model.ThemeMode
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.SEASON_KO
import com.gun.personalcolor.core.ui.ToneAvatar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.gun.personalcolor"

internal data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val myTone: Tone? = null,
    val saveHistory: Boolean = true,
    val showPhotoGuide: Boolean = true,
    val historyEmpty: Boolean = true,
    val dailyNotification: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
)

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val history: HistoryRepository,
    content: ContentRepository,
) : ViewModel() {
    val tones: List<Tone> = content.tones

    val state: StateFlow<SettingsState> = combine(prefs.preferences, history.entries) { p, h ->
        SettingsState(
            p.themeMode, content.toneOrNull(p.myToneId), p.saveHistory, p.showPhotoGuide, h.isEmpty(),
            p.dailyNotification, p.notificationHour, p.notificationMinute,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setMyTone(id: String?) = viewModelScope.launch { prefs.setMyTone(id) }
    fun setSaveHistory(on: Boolean) = viewModelScope.launch { prefs.setSaveHistory(on) }
    fun setShowPhotoGuide(on: Boolean) = viewModelScope.launch { prefs.setShowPhotoGuide(on) }
    // The app shell reschedules the daily work whenever these preferences change (feature:coordi DailyNotificationSync).
    fun setDailyNotification(on: Boolean, hour: Int, minute: Int) = viewModelScope.launch { prefs.setDailyNotification(on, hour, minute) }

    fun clearHistory() = viewModelScope.launch {
        history.clear()
        _messages.send("진단 기록을 모두 삭제했어요")
    }

    fun resetAll() = viewModelScope.launch {
        prefs.reset()
        history.clear()
        _messages.send("모든 데이터를 초기화했어요")
    }
}

/** Callbacks for the rows that leave the screen (navigation or other apps). */
internal class SettingsActions(
    val onBack: () -> Unit,
    val onLicenses: () -> Unit,
    val onLegal: (LegalDoc) -> Unit,
    val onOnboarding: () -> Unit,
    val onPrivacyOptions: (() -> Unit)?,
    val onRate: () -> Unit,
    val onShare: () -> Unit,
    val onContact: () -> Unit,
)

private enum class Confirm { CLEAR_HISTORY, RESET_ALL }

@Composable
internal fun SettingsRoute(
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    onLegal: (LegalDoc) -> Unit,
    onOnboarding: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    TrackScreen("settings")
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ads = LocalAdManager.current
    val privacyRequired = remember(ads) { ads?.privacyOptionsRequired(context) == true }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val actions = SettingsActions(
        onBack, onLicenses, onLegal, onOnboarding,
        onPrivacyOptions = if (privacyRequired && activity != null) {{ ads?.showPrivacyOptions(activity) }} else null,
        onRate = { if (activity != null) requestReview(activity) else context.openStore() },
        onShare = { context.shareApp() },
        onContact = { context.sendFeedbackMail() },
    )
    SettingsScreen(
        state, vm.tones, actions, snackbar,
        onTheme = vm::setTheme, onMyTone = vm::setMyTone,
        onSaveHistory = vm::setSaveHistory, onPhotoGuide = vm::setShowPhotoGuide,
        onClearHistory = vm::clearHistory, onResetAll = vm::resetAll,
        onDailyNotification = vm::setDailyNotification,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    tones: List<Tone>,
    actions: SettingsActions,
    snackbar: SnackbarHostState,
    onTheme: (ThemeMode) -> Unit,
    onMyTone: (String?) -> Unit,
    onSaveHistory: (Boolean) -> Unit,
    onPhotoGuide: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onResetAll: () -> Unit,
    onDailyNotification: (Boolean, Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val version = remember { context.appVersion() }
    var themeDialog by rememberSaveable { mutableStateOf(false) }
    var tonePicker by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<Confirm?>(null) }

    PcScaffold("설정", actions.onBack) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.readableWidth(), contentPadding = padding.plusBottom(32.dp)) {
                item { RemoveAdsGroup(snackbar) }
                item {
                    Group("화면") {
                        ListRow("테마", subtitle = state.themeMode.label, icon = Icons.Outlined.DarkMode, onClick = { themeDialog = true })
                    }
                }
                item { NotificationGroup(state, snackbar, onDailyNotification) }
                item {
                    Group("진단") {
                        ListRow("내 퍼스널컬러", subtitle = state.myTone?.nameKo ?: "설정 안 됨", icon = Icons.Outlined.Palette,
                            onClick = { tonePicker = true })
                        SwitchRow("진단 기록 저장", "끄면 새 결과가 기록에 남지 않아요", Icons.Outlined.History, state.saveHistory, onSaveHistory)
                        SwitchRow("촬영 가이드 보기", "사진 진단 전에 팁을 보여줘요", Icons.Outlined.CameraAlt, state.showPhotoGuide, onPhotoGuide)
                    }
                }
                item {
                    Group("데이터") {
                        ListRow(
                            "진단 기록 전체 삭제",
                            subtitle = if (state.historyEmpty) "삭제할 기록이 없어요" else "저장된 진단 기록을 모두 지워요",
                            icon = Icons.Outlined.DeleteOutline,
                            onClick = if (state.historyEmpty) null else {{ confirm = Confirm.CLEAR_HISTORY }},
                            trailing = null,
                        )
                        ListRow("모든 데이터 초기화", subtitle = "설정·내 톤·즐겨찾기·기록 초기화",
                            icon = Icons.Outlined.RestartAlt, iconTint = MaterialTheme.colorScheme.error,
                            onClick = { confirm = Confirm.RESET_ALL }, trailing = null)
                    }
                }
                item {
                    Group("개인정보") {
                        actions.onPrivacyOptions?.let {
                            ListRow("개인정보 옵션", subtitle = "광고 개인정보 동의 변경", icon = Icons.Outlined.Shield, onClick = it)
                        }
                        ListRow("개인정보처리방침", icon = Icons.Outlined.PrivacyTip, onClick = { actions.onLegal(LegalDoc.PRIVACY) })
                        ListRow("이용약관", icon = Icons.Outlined.Description, onClick = { actions.onLegal(LegalDoc.TERMS) })
                    }
                }
                item {
                    Group("지원") {
                        ListRow("앱 평가하기", icon = Icons.Outlined.StarOutline, onClick = actions.onRate)
                        ListRow("친구에게 앱 공유하기", icon = Icons.Outlined.Share, onClick = actions.onShare)
                        ListRow("문의하기", subtitle = CONTACT_EMAIL, icon = Icons.Outlined.Mail, onClick = actions.onContact)
                    }
                }
                item {
                    Group("정보") {
                        ListRow("온보딩 다시 보기", icon = Icons.AutoMirrored.Outlined.HelpOutline, onClick = actions.onOnboarding)
                        ListRow("오픈소스 라이선스", icon = Icons.Outlined.Code, onClick = actions.onLicenses)
                        ListRow("앱 버전", icon = Icons.Outlined.Info, trailing = {
                            Text(version, style = Muted, maxLines = 1)
                        })
                    }
                }
            }
            PcSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
        }
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("테마") },
            text = {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                .selectable(state.themeMode == mode, role = Role.RadioButton) { onTheme(mode); themeDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.themeMode == mode, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ themeDialog = false }) { Text("닫기") } },
        )
    }

    if (tonePicker) {
        ModalBottomSheet(onDismissRequest = { tonePicker = false }) {
            TonePicker(tones, state.myTone?.id) { onMyTone(it); tonePicker = false }
        }
    }

    when (confirm) {
        Confirm.CLEAR_HISTORY -> ConfirmDialog(
            "진단 기록을 모두 삭제할까요?", "삭제한 기록은 되돌릴 수 없어요.", "삭제",
            onConfirm = onClearHistory, onDismiss = { confirm = null },
        )
        Confirm.RESET_ALL -> ConfirmDialog(
            "모든 데이터를 초기화할까요?",
            "설정·내 톤·즐겨찾기·기록이 모두 지워져요. 되돌릴 수 없어요.",
            "초기화", onConfirm = onResetAll, onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

private val ThemeMode.label get() = when (this) {
    ThemeMode.SYSTEM -> "시스템 설정"
    ThemeMode.LIGHT -> "라이트"
    ThemeMode.DARK -> "다크"
}

/** Eyebrow label + white card holding a group of rows. */
@Composable
private fun Group(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 24.dp)) {
        Eyebrow(label, Modifier.padding(start = 4.dp, bottom = 8.dp))
        PcCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 6.dp)) { content() } }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) =
    ListRow(title, subtitle = subtitle, icon = icon, onClick = { onChange(!checked) },
        trailing = { Switch(checked = checked, onCheckedChange = null) })

@Composable
private fun TonePicker(tones: List<Tone>, selectedId: String?, onPick: (String?) -> Unit) {
    LazyColumn(Modifier.readableWidth().navigationBarsPadding()) {
        item {
            Text("내 퍼스널컬러", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = ScreenPadding).padding(bottom = 4.dp))
            Text("홈과 컬러 체크의 기준이 돼요", style = Muted,
                modifier = Modifier.padding(horizontal = ScreenPadding).padding(bottom = 8.dp))
            ListRow("설정 해제", icon = Icons.Outlined.Block, onClick = { onPick(null) },
                trailing = if (selectedId == null) {{ Icon(Icons.Outlined.Check, "선택됨", tint = MaterialTheme.colorScheme.primary) }} else null)
        }
        tones.groupBy { it.season }.forEach { (season, group) ->
            item(key = season) {
                Text(
                    SEASON_KO[season].orEmpty(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = ScreenPadding).padding(top = 16.dp, bottom = 4.dp),
                )
            }
            items(group, key = { it.id }) { tone ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(tone.id == selectedId, role = Role.RadioButton) { onPick(tone.id) }
                        .padding(horizontal = ScreenPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToneAvatar(tone, size = 36.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(tone.nameKo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    if (tone.id == selectedId) Icon(Icons.Outlined.Check, "선택됨", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Spacer(Modifier.padding(12.dp)) }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton({ onConfirm(); onDismiss() }) { Text(confirmText, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
    )

// --- Intents ---

private fun Context.appVersion(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "-"

private fun requestReview(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        else activity.openStore()
    }
}

private fun Context.openStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        openUrl("https://play.google.com/store/apps/details?id=$packageName")
    }
}

private fun Context.shareApp() {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "사진 한 장으로 내 퍼스널컬러 찾기\n나의 컬러는? - 퍼스널컬러\n$PLAY_URL")
    startActivity(Intent.createChooser(send, "앱 공유하기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.sendFeedbackMail() {
    val subject = "[나의 컬러는?] 문의"
    val body = "\n\n\n----------\n앱 버전: ${appVersion()}\nAndroid: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n기기: ${Build.MANUFACTURER} ${Build.MODEL}"
    // Subject/body go in both the URI and extras: mail apps differ in which one they read.
    val uri = Uri.parse("mailto:$CONTACT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
    val intent = Intent(Intent.ACTION_SENDTO, uri)
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, subject)
        .putExtra(Intent.EXTRA_TEXT, body)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "메일 앱이 없어요. $CONTACT_EMAIL 로 보내 주세요", Toast.LENGTH_LONG).show()
    }
}

@Preview
@Composable
private fun SettingsPreview() = PcTheme {
    SettingsScreen(
        SettingsState(), emptyList(),
        SettingsActions({}, {}, {}, {}, null, {}, {}, {}), remember { SnackbarHostState() },
        {}, {}, {}, {}, {}, {}, { _, _, _ -> },
    )
}

/** "프리미엄" one-time purchase (no ads + unlimited daily outfit + premium tools) + restore. Hidden in previews. */
@Composable
private fun RemoveAdsGroup(snackbar: SnackbarHostState) {
    val billing = LocalAdManager.current?.billing ?: return
    val activity = LocalActivity.current
    val adFree by billing.adFree.collectAsStateWithLifecycle()
    val price by billing.price.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    PremiumCard(
        adFree, price,
        onPurchase = {
            val started = activity != null && billing.purchase(activity)
            if (!started) scope.launch { snackbar.showSnackbar("지금은 구매할 수 없어요. 잠시 후 다시 시도해 주세요.") }
        },
        onRestore = {
            billing.restore()
            scope.launch { snackbar.showSnackbar("구매 내역을 확인했어요") }
        },
    )
}

/** Soft aurora card with the drop corner: benefits + one-time purchase, or 틴티 cheering once it's bought. */
@Composable
private fun PremiumCard(adFree: Boolean, price: String?, onPurchase: () -> Unit, onRestore: () -> Unit) {
    Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 12.dp)) {
        PcCard(Modifier.fillMaxWidth(), shape = DropShape, elevation = 2) {
            Box(Modifier.aurora(PcTheme.colors)) {
                if (adFree) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Tinty(Modifier.size(80.dp), expression = TintyExpression.Cheer, animated = false)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Eyebrow("프리미엄")
                            Text("프리미엄 이용 중", style = MaterialTheme.typography.titleLarge)
                            Text("광고 없이 모든 기능을 쓰고 있어요", style = Muted)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Eyebrow("프리미엄")
                            Text("광고 없이 더 편하게", style = MaterialTheme.typography.titleLarge)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("광고 없이 보기", "오늘의 코디 무제한", "립 체험·리포트 바로 보기").forEach { Benefit(it) }
                        }
                        Text((price?.let { "$it · " } ?: "") + "한 번만 결제하면 계속 써요", style = Muted)
                        PcButton("프리미엄 시작하기", onPurchase, Modifier.fillMaxWidth())
                        TextButton(onRestore, Modifier.align(Alignment.CenterHorizontally)) { Text("구매 복원") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Benefit(text: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = PcTheme.colors.best)
    Spacer(Modifier.width(8.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

/** "알림": daily 오늘의 컬러·코디 notification switch + time. Asks for POST_NOTIFICATIONS on API 33+ when turning on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationGroup(state: SettingsState, snackbar: SnackbarHostState, onChange: (Boolean, Int, Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var timeDialog by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onChange(true, state.notificationHour, state.notificationMinute)
        else scope.launch { snackbar.showSnackbar("설정에서 알림 권한을 허용해 주세요") }
    }
    val enable = {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else onChange(true, state.notificationHour, state.notificationMinute)
    }
    Group("알림") {
        SwitchRow("오늘의 컬러 알림", "매일 컬러와 날씨 맞춤 코디를 알려줘요", Icons.Outlined.Notifications, state.dailyNotification) { on ->
            if (on) enable() else onChange(false, state.notificationHour, state.notificationMinute)
        }
        ListRow("알림 시간", subtitle = timeLabel(state.notificationHour, state.notificationMinute),
            icon = Icons.Outlined.Schedule, onClick = { timeDialog = true })
    }
    if (timeDialog) {
        val picker = rememberTimePickerState(state.notificationHour, state.notificationMinute, is24Hour = false)
        val tall = LocalConfiguration.current.screenHeightDp >= 600
        AlertDialog(
            onDismissRequest = { timeDialog = false },
            title = { Text("알림 시간") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (tall) TimePicker(picker) else TimeInput(picker)
                }
            },
            confirmButton = {
                TextButton({
                    timeDialog = false
                    onChange(state.dailyNotification, picker.hour, picker.minute)
                }) { Text("확인") }
            },
            dismissButton = { TextButton({ timeDialog = false }) { Text("취소") } },
        )
    }
}

private fun timeLabel(hour: Int, minute: Int) =
    "${if (hour < 12) "오전" else "오후"} ${(hour + 11) % 12 + 1}:${minute.toString().padStart(2, '0')}"

@Preview
@Composable
private fun PremiumPreview() = PcTheme {
    Column { PremiumCard(false, "₩3,900", {}, {}); PremiumCard(true, null, {}, {}) }
}
