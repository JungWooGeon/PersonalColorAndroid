package com.gun.personalcolor.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.EmptyState
import com.gun.personalcolor.core.designsystem.ListRow
import com.gun.personalcolor.core.designsystem.Muted
import com.gun.personalcolor.core.designsystem.PcScaffold
import com.gun.personalcolor.core.designsystem.PcTheme
import com.gun.personalcolor.core.designsystem.ScreenPadding
import com.gun.personalcolor.core.designsystem.SectionCard
import com.gun.personalcolor.core.designsystem.readableWidth
import com.gun.personalcolor.core.ui.Bullet
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "링크를 열 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show()
    }
}

/** Native, typeset privacy policy / terms. */
@Composable
internal fun LegalScreen(doc: LegalDoc, onBack: () -> Unit) {
    TrackScreen(if (doc == LegalDoc.PRIVACY) "legal_privacy" else "legal_terms")
    val document = legalDocument(doc)
    val context = LocalContext.current
    PcScaffold(document.title, onBack) { padding ->
        LazyColumn(
            Modifier.readableWidth(),
            contentPadding = padding.plusBottom(32.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 8.dp, bottom = 8.dp)) {
                    Text(document.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("시행일: ${document.effectiveDate}", style = Muted)
                    Spacer(Modifier.height(16.dp))
                    Text(document.intro, style = MaterialTheme.typography.bodyMedium)
                }
            }
            itemsIndexed(document.sections) { i, section ->
                Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("${i + 1}. ${section.heading}", style = MaterialTheme.typography.titleMedium)
                    section.paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    section.bullets.forEach { Bullet(it) }
                }
            }
            item {
                Column(Modifier.padding(horizontal = ScreenPadding).padding(top = 32.dp)) {
                    SectionCard(null) {
                        ListRow(
                            "웹에서 보기",
                            subtitle = document.webUrl.removePrefix("https://"),
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = { context.openUrl(document.webUrl) },
                        )
                    }
                }
            }
        }
    }
}

/** Open-source licenses from the app's generated `res/raw/aboutlibraries.json`. */
@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    TrackScreen("licenses")
    val context = LocalContext.current
    // null = loading; Result.failure = json missing or unreadable.
    val libs by produceState<Result<Libs>?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DiscouragedApi") // the json lives in the app module, so no R reference here
                val id = context.resources.getIdentifier("aboutlibraries", "raw", context.packageName)
                check(id != 0) { "aboutlibraries.json not found" }
                Libs.Builder().withJson(context, id).build()
            }
        }
    }
    PcScaffold("오픈소스 라이선스", onBack) { padding ->
        val loaded = libs
        when {
            loaded == null -> Unit
            loaded.isFailure -> EmptyState(
                Icons.Outlined.Inventory2, "라이선스 정보를 불러오지 못했어요",
                "잠시 후 다시 시도해 주세요", Modifier.padding(padding),
            )
            else -> LibrariesContainer(
                libraries = loaded.getOrThrow(),
                modifier = Modifier.readableWidth(),
                contentPadding = padding.plusBottom(24.dp),
                header = {
                    item {
                        Column(Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp)) {
                            SectionCard(null) {
                                Text("Face Landmarker 모델", style = MaterialTheme.typography.titleMedium)
                                Text("Google · Apache License 2.0", style = Muted)
                                Text("얼굴 특징점 인식에 사용하는 MediaPipe 모델이에요.", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            SectionCard(null) {
                                Text("날씨 데이터 · Open-Meteo.com", style = MaterialTheme.typography.titleMedium)
                                Text("CC BY 4.0", style = Muted)
                                Text("오늘의 코디 날씨 정보에 사용해요.", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(10.dp))
                            SectionCard(null) {
                                Text("Pretendard 글꼴", style = MaterialTheme.typography.titleMedium)
                                Text("Kil Hyung-jin · SIL Open Font License 1.1", style = Muted)
                                Text("앱과 공유 이미지의 글꼴이에요.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
internal fun PaddingValues.plusBottom(extra: androidx.compose.ui.unit.Dp): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(dir), top = calculateTopPadding(),
        end = calculateEndPadding(dir), bottom = calculateBottomPadding() + extra,
    )
}

@Preview
@Composable
private fun LegalPreview() = PcTheme { LegalScreen(LegalDoc.TERMS) {} }
