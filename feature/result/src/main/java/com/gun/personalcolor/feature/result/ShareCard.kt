package com.gun.personalcolor.feature.result

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.gun.personalcolor.core.analytics.LocalAnalytics
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.share.ImageShare
import com.gun.personalcolor.core.share.ToneShare
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.gun.personalcolor.core.ui.ReportRenderer

/** Share choices (KakaoTalk rich card, or the image card through any app) opened from [anchor]'s click. */
@Composable
internal fun ShareMenu(tone: Tone, anchor: @Composable (open: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        // Rendering the card takes a moment: dim the anchor, show a spinner and ignore taps until the chooser opens.
        Box(Modifier.alpha(if (busy) 0.35f else 1f)) { anchor { if (!busy) open = true } }
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            DropdownMenuItem(
                text = { Text("카카오톡으로 공유", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.ChatBubbleOutline, null) },
                onClick = {
                    open = false
                    analytics?.log("share", "channel" to "kakao", "tone" to tone.id)
                    context.findActivity()?.let { runCatching { ToneShare.share(it, tone) } }
                },
            )
            DropdownMenuItem(
                text = { Text("이미지로 공유", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Image, null) },
                onClick = {
                    open = false
                    analytics?.log("share", "channel" to "image", "tone" to tone.id)
                    busy = true
                    scope.launch { shareResult(context, tone); busy = false }
                },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shares a result card image. The face photo is never included. Render, file or chooser failures only show a toast. */
internal suspend fun shareResult(context: Context, tone: Tone) {
    val text = "나의 퍼스널컬러는 ${tone.nameKo}! #퍼스널컬러 #${tone.nameKo.replace(" ", "")}\n${ToneShare.pageUrl(tone.id)}"
    if (!ImageShare.share(context, "result", "결과 공유", text) { ReportRenderer.renderCard(context, tone) }) {
        Toast.makeText(context, "공유하지 못했어요. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
    }
}
