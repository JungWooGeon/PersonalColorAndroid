package com.gun.personalcolor.feature.result


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.gun.personalcolor.core.share.ImageShare
import com.gun.personalcolor.core.ui.ImageSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Returns `save(name) { render() }`: asks for the storage permission on API<29, renders off the main thread,
 * saves to the gallery and shows a snackbar ("갤러리에 저장했어요" with a 공유 action, or an error).
 */
@Composable
internal fun rememberImageSave(snackbar: SnackbarHostState): (name: String, render: () -> Bitmap) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<String, () -> Bitmap>?>(null) }

    fun run(name: String, render: () -> Bitmap) {
        scope.launch {
            val bmp = runCatching { withContext(Dispatchers.Default) { render() } }.getOrNull()
            if (bmp == null || !withContext(Dispatchers.IO) { ImageSaver.save(context, bmp, name) }) {
                snackbar.showSnackbar("저장하지 못했어요. 잠시 후 다시 시도해 주세요"); return@launch
            }
            val r = snackbar.showSnackbar("갤러리에 저장했어요", actionLabel = "공유", duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) ImageShare.share(context, name) { bmp }
        }
    }

    val permission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        val p = pending; pending = null
        if (p == null) return@rememberLauncherForActivityResult
        if (granted) run(p.first, p.second) else scope.launch { snackbar.showSnackbar("저장하려면 권한이 필요해요") }
    }

    return { name, render ->
        val denied = ImageSaver.needsPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (denied) { pending = name to render; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) } else run(name, render)
    }
}
