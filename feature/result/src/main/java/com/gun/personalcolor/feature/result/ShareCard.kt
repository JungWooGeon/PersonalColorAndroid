package com.gun.personalcolor.feature.result

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.share.ToneShare
import java.io.File

/** Top-bar share button: KakaoTalk rich card, or the image card through any app. */
@Composable
internal fun ShareMenuButton(tone: Tone) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Outlined.Share, "결과 공유") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
                text = { Text("다른 앱으로 공유(이미지)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
    val w = 1080; val h = 1350
    val bmp = createBitmap(w, h)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    c.drawColor("#FAF8F6".toColorInt())

    val colors = tone.bestColors.take(4).map { it.hex.toColorInt() }.toIntArray()
    p.shader = LinearGradient(0f, 0f, w.toFloat(), 480f, colors, null, Shader.TileMode.CLAMP)
    c.drawRoundRect(RectF(60f, 60f, w - 60f, 520f), 60f, 60f, p)
    p.shader = null

    fun text(s: String, x: Float, y: Float, size: Float, bold: Boolean, color: String) {
        p.textSize = size; p.color = color.toColorInt()
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        c.drawText(s, x, y, p)
    }
    text("나의 퍼스널컬러는", 100f, 640f, 44f, false, "#66605B")
    text(tone.nameKo, 100f, 750f, 104f, true, "#1D1B1A")
    text(tone.keywords.joinToString("  ") { "#$it" }, 100f, 830f, 40f, false, "#5B4B8A")

    val size = 128f; val gap = (w - 200f - size * 6) / 5
    tone.bestColors.take(12).forEachIndexed { i, col ->
        val x = 100f + (i % 6) * (size + gap); val y = 900f + (i / 6) * (size + 30f)
        p.color = col.hex.toColorInt()
        c.drawRoundRect(RectF(x, y, x + size, y + size), 28f, 28f, p)
    }
    text("나의 컬러는? - 퍼스널컬러", 100f, h - 80f, 36f, true, "#9A918B")

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
