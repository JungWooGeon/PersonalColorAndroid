package com.gun.personalcolor.core.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Shares a generated PNG through the app's FileProvider (cache dir) with a working chooser preview. */
object ImageShare {
    /** Renders and writes off the main thread, then opens the chooser. Returns false (never throws) on any failure. */
    suspend fun share(context: Context, name: String, chooserTitle: String = "공유", text: String? = null, render: () -> Bitmap): Boolean {
        val uri = runCatching {
            withContext(Dispatchers.Default) {
                val bmp = render()
                val file = File(context.cacheDir, "$name.png")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            }
        }.onFailure { Log.w(TAG, "render failed", it) }.getOrNull() ?: return false
        return runCatching { context.startActivity(chooser(uri, chooserTitle, text)) }
            .onFailure { Log.w(TAG, "chooser failed", it) }.isSuccess
    }

    /** ClipData + read grant on the chooser intent itself: without them the share sheet can't load the preview thumbnail. */
    fun chooser(uri: Uri, title: String, text: String? = null): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text != null) putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title).apply {
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private const val TAG = "ImageShare"
}
