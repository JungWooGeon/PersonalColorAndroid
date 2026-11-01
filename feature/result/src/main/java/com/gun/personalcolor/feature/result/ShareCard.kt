package com.gun.personalcolor.feature.result

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.share.ToneShare
import com.gun.personalcolor.core.ui.ReportRenderer
import java.io.File

/** Share choices (KakaoTalk rich card, or the image card through any app) opened from [anchor]'s click. */
@Composable
internal fun ShareMenu(tone: Tone, anchor: @Composable (open: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    var open by remember { mutableStateOf(false) }
    Box {
        anchor { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            DropdownMenuItem(
                text = { Text("카카오톡으로 공유", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.ChatBubbleOutline, null) },
                onClick = {
                    open = false
                    analytics?.log("share", "channel" to "kakao", "tone" to tone.id)
                    context.findActivity()?.let { ToneShare.share(it, tone) }
                },
            )
            DropdownMenuItem(
                text = { Text("이미지로 공유", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Image, null) },
                onClick = {
                    open = false
                    analytics?.log("share", "channel" to "image", "tone" to tone.id)
                    shareResult(context, tone)
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

/** Shares a result card image. The face photo is never included. */
internal fun shareResult(context: Context, tone: Tone) {
    val bmp = ReportRenderer.renderCard(tone)
    val file = File(context.cacheDir, "result.png")
    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "나의 퍼스널컬러는 ${tone.nameKo}! #퍼스널컬러 #${tone.nameKo.replace(" ", "")}\n${ToneShare.pageUrl(tone.id)}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "결과 공유"))
}
