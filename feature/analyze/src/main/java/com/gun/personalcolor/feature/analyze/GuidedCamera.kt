package com.gun.personalcolor.feature.analyze

import android.util.Log
import kotlinx.coroutines.CancellationException

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import android.util.Size
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.gun.personalcolor.core.analytics.TrackScreen
import com.gun.personalcolor.core.designsystem.PcTheme
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

// Brand "best"/"okay" colors for dark backgrounds (readable on the camera feed).
internal val CameraGreen = Color(0xFF8FD3BC)
internal val CameraWarn = Color(0xFFE3C77A)
/** 틴티 ink, translucent: hint pill / chips over the feed. */
private val CameraInk = Color(0xCC2B2236)
private const val AUTO_CAPTURE_MS = 1200

/**
 * In-app camera that checks face position and lighting ~5 times a second and shoots automatically once
 * everything has looked right for [AUTO_CAPTURE_MS]. Assumes CAMERA permission is granted.
 * [onCaptured] gets the saved JPEG (cache dir) and whether it was an auto capture.
 */
@Composable
internal fun GuidedCamera(onClose: () -> Unit, onCaptured: (Uri, Boolean) -> Unit) {
    TrackScreen("camera")
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val captured by rememberUpdatedState(onCaptured)
    var front by rememberSaveable { mutableStateOf(true) }
    var check by remember { mutableStateOf(FrameCheck.Waiting) }
    var surface by remember { mutableStateOf<SurfaceRequest?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val checker = remember { FaceChecker(context) }
    val ratio43 = remember {
        ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
    }
    val preview = remember { Preview.Builder().setResolutionSelector(ratio43).build().apply { setSurfaceProvider { surface = it } } }
    val capture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setResolutionSelector(ratio43)
            .build()
    }
    val analysis = remember {
        val main = ContextCompat.getMainExecutor(context)
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .apply { setAnalyzer(checker.executor, checker.analyzer { r -> main.execute { check = r } }) }
    }

    LaunchedEffect(front) {
        // awaitInstance (CameraX init) and hasCamera (CameraInfoUnavailableException) can throw on broken camera HALs.
        try {
            val p = ProcessCameraProvider.awaitInstance(context).also { provider = it }
            val selector = when {
                front && p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                else -> CameraSelector.DEFAULT_FRONT_CAMERA
            }
            p.unbindAll()
            check = FrameCheck.Waiting
            p.bindToLifecycle(owner, selector, preview, analysis, capture)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("GuidedCamera", "camera unavailable", e)
            check = FrameCheck(Check.entries.toSet(), "카메라를 열 수 없어요 · 앨범에서 골라 주세요")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            provider?.unbindAll()
            analysis.clearAnalyzer()
            checker.close()
        }
    }

    fun shoot(auto: Boolean) {
        if (capturing) return
        capturing = true
        val file = File(context.cacheDir, "guided_${System.currentTimeMillis()}.jpg")
        // Save the real (unmirrored) image; only the preview is mirrored for the front camera.
        val options = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = false })
            .build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = captured(Uri.fromFile(file), auto)
            override fun onError(exception: ImageCaptureException) {
                capturing = false
                Toast.makeText(context, "촬영하지 못했어요. 다시 시도해 주세요", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Countdown ring: restarts whenever a frame fails, shoots when it completes.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(check.ok, capturing, confirm) {
        if (check.ok && !capturing && !confirm) {
            progress.animateTo(1f, tween(AUTO_CAPTURE_MS, easing = LinearEasing))
            shoot(auto = true)
        } else {
            progress.snapTo(0f)
        }
    }

    val onShutter = { if (check.ok) shoot(auto = false) else confirm = true }
    val onFlip = { front = !front }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("아직 조건이 맞지 않아요") },
            text = { Text("${check.hint}\n이대로 찍으면 결과가 달라질 수 있어요.") },
            confirmButton = { TextButton(onClick = { confirm = false; shoot(auto = false) }) { Text("그래도 촬영") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } },
        )
    }

    PcTheme(darkTheme = true) { CompositionLocalProvider(LocalContentColor provides Color.White) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val insets = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            if (maxWidth > maxHeight) {
                Row(insets) {
                    Viewfinder(surface, check, 4f / 3f, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(240.dp).fillMaxHeight().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        CameraTopBar(onClose)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Check.entries.forEach { CheckChip(it, it !in check.failed) }
                        }
                        Spacer(Modifier.height(8.dp))
                        ShutterRow(progress.value, !capturing, onShutter, onFlip)
                    }
                }
            } else {
                Column(insets) {
                    CameraTopBar(onClose, Modifier.padding(horizontal = 4.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) { Check.entries.forEach { CheckChip(it, it !in check.failed) } }
                    Viewfinder(surface, check, 3f / 4f, Modifier.weight(1f).fillMaxWidth())
                    ShutterRow(progress.value, !capturing, onShutter, onFlip, Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                }
            }
        }
    } }
}

@Composable
private fun CameraTopBar(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "뒤로") }
        Text("가이드 촬영", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Preview fitted to a 3:4 (portrait) or 4:3 (landscape) box, with the oval guide and the main hint. */
@Composable
private fun Viewfinder(surface: SurfaceRequest?, check: FrameCheck, ratio: Float, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.aspectRatio(ratio)) {
            surface?.let { CameraXViewfinder(it, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            val color by animateColorAsState(if (check.ok) CameraGreen else Color.White, label = "oval")
            Canvas(Modifier.fillMaxSize()) {
                val ovalH = size.height * 0.58f
                val ovalW = min(ovalH * 0.74f, size.width * 0.8f)
                val oval = Rect(Offset(size.width / 2 - ovalW / 2, size.height * GUIDE_CENTER_Y - ovalH / 2), androidx.compose.ui.geometry.Size(ovalW, ovalH))
                val dim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addOval(oval)
                }
                drawPath(dim, Color(0xFF1B1530).copy(alpha = 0.5f))
                drawOval(color, oval.topLeft, oval.size, style = Stroke(3.dp.toPx()))
            }
            Surface(
                color = CameraInk,
                contentColor = if (check.ok) CameraGreen else Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) {
                Text(
                    check.hint, Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CheckChip(check: Check, ok: Boolean) {
    Surface(color = CameraInk, contentColor = Color.White, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Rounded.Check else Icons.Rounded.PriorityHigh, if (ok) "통과" else "확인 필요",
                Modifier.size(16.dp), tint = if (ok) CameraGreen else CameraWarn,
            )
            Spacer(Modifier.width(6.dp))
            Text(check.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Shutter with the auto-capture countdown ring, plus the camera switch button. */
@Composable
private fun ShutterRow(progress: Float, enabled: Boolean, onShutter: () -> Unit, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Spacer(Modifier.size(48.dp))
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 4.dp.toPx()
                drawCircle(Color.White.copy(alpha = 0.35f), radius = size.minDimension / 2 - w / 2, style = Stroke(w))
                drawArc(CameraGreen, -90f, 360f * progress, false, style = Stroke(w, cap = StrokeCap.Round),
                    topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w))
            }
            Surface(onClick = onShutter, enabled = enabled, shape = CircleShape, color = Color.White, modifier = Modifier.size(66.dp)) {}
        }
        IconButton(onClick = onFlip, Modifier.size(48.dp)) { Icon(Icons.Outlined.Cameraswitch, "카메라 전환") }
    }
}

/** Runs MediaPipe on a throttled, downscaled preview frame on its own thread. */
private class FaceChecker(context: Context) {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val appContext = context.applicationContext
    private var lastMs = 0L

    // Only touched on [executor]. Null if the model can't load (then only manual capture is useful).
    private var tried = false
    private var model: FaceLandmarker? = null
    private val landmarker: FaceLandmarker?
        get() {
            if (!tried) { tried = true; model = load() }
            return model
        }

    private fun load(): FaceLandmarker? =
        runCatching {
            FaceLandmarker.createFromOptions(
                appContext,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(2)
                    .build(),
            )
        }.getOrNull()

    fun analyzer(onResult: (FrameCheck) -> Unit) = ImageAnalysis.Analyzer { image ->
        image.use {
            val now = SystemClock.elapsedRealtime()
            if (now - lastMs < 200) return@use
            lastMs = now
            onResult(runCatching { check(it) }.getOrElse { Unavailable })
        }
    }

    private fun check(image: ImageProxy): FrameCheck {
        val lm = landmarker ?: return Unavailable
        val raw = image.toBitmap()
        val deg = image.imageInfo.rotationDegrees
        val bmp = if (deg == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(deg.toFloat()) }, true)
        val faces = lm.detect(BitmapImageBuilder(bmp).build()).faceLandmarks().map { f -> f.map { Offset(it.x(), it.y()) } }
        val pixels = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
        return checkFrame(faces, pixels, bmp.width, bmp.height)
    }

    fun close() {
        executor.execute { model?.close(); model = null }
        executor.shutdown()
    }

    private companion object {
        val Unavailable = FrameCheck(Check.entries.toSet(), "자동 확인을 쓸 수 없어요 · 직접 찍어 주세요")
    }
}
