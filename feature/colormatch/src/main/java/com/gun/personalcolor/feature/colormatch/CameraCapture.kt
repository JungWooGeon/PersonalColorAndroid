package com.gun.personalcolor.feature.colormatch

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Returns an action that takes a photo with the system camera into cache/[fileName] and hands its Uri to [onPhoto].
 * The app declares CAMERA (for the guided diagnosis camera), so ACTION_IMAGE_CAPTURE throws SecurityException until
 * it's granted: ask first, and report a denial or a missing camera app through [onError] instead of crashing.
 */
@Composable
internal fun rememberCameraCapture(fileName: String, onPhoto: (Uri) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(onPhoto) }
    val launch = {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(context.cacheDir, fileName))
            cameraUri = uri
            camera.launch(uri)
        } catch (_: ActivityNotFoundException) {
            onError("카메라 앱이 없어요. 앨범에서 골라 주세요.")
        } catch (e: RuntimeException) { // SecurityException (permission revoked meanwhile), FileProvider misconfig
            Log.w("CameraCapture", "camera launch failed", e)
            onError("카메라를 열지 못했어요. 앨범에서 골라 주세요.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launch() else onError("카메라 권한이 없어요. 앨범에서 골라 주세요.")
    }
    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launch()
        else permission.launch(Manifest.permission.CAMERA)
    }
}
