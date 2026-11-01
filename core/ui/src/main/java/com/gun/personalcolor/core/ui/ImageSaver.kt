package com.gun.personalcolor.core.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Saves a generated image (report, wallpaper) to the gallery under Pictures/나의 컬러.
 * API 29+: MediaStore, no permission. API 26–28: needs WRITE_EXTERNAL_STORAGE (declared with maxSdkVersion 28 in the app manifest);
 * callers request it first on those versions. Returns true on success.
 */
object ImageSaver {
    private const val FOLDER = "나의 컬러"

    val needsPermission get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun save(context: Context, bitmap: Bitmap, name: String): Boolean = runCatching {
        val file = "$name-${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER).apply { mkdirs() }
            val out = File(dir, file)
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(out.path), arrayOf("image/png"), null)
        }
        true
    }.getOrDefault(false)
}
