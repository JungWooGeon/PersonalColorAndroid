package com.gun.personalcolor.feature.colormatch

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.model.Verdict
import com.gun.personalcolor.core.vision.BitmapLoader
import com.gun.personalcolor.core.vision.GarmentCutter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class AddMode { PHOTO, MANUAL }

data class ClosetAddUiState(
    val mode: AddMode = AddMode.PHOTO,
    val tones: List<Tone> = emptyList(),
    val tone: Tone? = null,               // my tone; null = no verdict, saved as neutral
    val photo: Bitmap? = null,            // cut-out (or the uncut photo if cutting failed)
    val processing: Boolean = false,
    val cutFailed: Boolean = false,
    val point: Pair<Float, Float>? = null, // tapped point (fractions); null = auto color
    val photoColor: Rgb? = null,
    val hsv: List<Float> = listOf(30f, 0.35f, 0.8f),
    val manualColor: Rgb = hsvToRgb(listOf(30f, 0.35f, 0.8f)),
    val kind: String? = null,
    val name: String = "",
    val nameEdited: Boolean = false,
    val verdict: ColorVerdict? = null,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val color: Rgb? get() = if (mode == AddMode.PHOTO) photoColor else manualColor
    val canSave: Boolean get() = color != null && kind != null && !processing && !saved
}

@HiltViewModel
class ClosetAddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val closet: ClosetRepository,
    private val cutter: GarmentCutter,
) : ViewModel() {
    private val _state = MutableStateFlow(ClosetAddUiState())
    val state: StateFlow<ClosetAddUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tones = withContext(Dispatchers.IO) { content.tones }
            prefs.preferences.collect { p ->
                _state.update { it.copy(tones = tones, tone = tones.firstOrNull { t -> t.id == p.myToneId }) }
                refresh()
            }
        }
    }

    /** Fresh form each time the add screen opens (the VM outlives it). */
    fun reset() = _state.update { ClosetAddUiState(tones = it.tones, tone = it.tone) }.also { refresh() }

    fun setMode(m: AddMode) { _state.update { it.copy(mode = m, error = null) }; refresh() }

    fun loadPhoto(uri: Uri) {
        _state.update { it.copy(processing = true, error = null, mode = AddMode.PHOTO) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val bmp = BitmapLoader.load(context, uri, 1600) ?: return@withContext null
                val cut = cutter.cutout(bmp)
                val shown = cut ?: scaleDown(bmp, 1024)
                Triple(shown, cut == null, cutter.dominantColor(shown))
            }
            if (result == null) {
                _state.update { it.copy(processing = false, error = "사진을 불러오지 못했어요. 다른 사진으로 해 볼까요?") }
                return@launch
            }
            val (bmp, failed, color) = result
            _state.update { it.copy(processing = false, photo = bmp, cutFailed = failed, point = null, photoColor = color) }
            refresh()
        }
    }

    /** Re-pick the color at a tapped point (fractions of the photo); transparent pixels are skipped. */
    fun pickPoint(fx: Float, fy: Float) {
        val bmp = _state.value.photo ?: return
        val x = fx.coerceIn(0f, 1f); val y = fy.coerceIn(0f, 1f)
        val rgb = sampleOpaque(bmp, (x * (bmp.width - 1)).roundToInt(), (y * (bmp.height - 1)).roundToInt()) ?: return
        _state.update { it.copy(point = x to y, photoColor = rgb) }
        refresh()
    }

    fun autoColor() {
        val bmp = _state.value.photo ?: return
        viewModelScope.launch {
            val c = withContext(Dispatchers.Default) { cutter.dominantColor(bmp) }
            _state.update { it.copy(point = null, photoColor = c) }
            refresh()
        }
    }

    fun setHsv(hsv: List<Float>) { _state.update { it.copy(hsv = hsv, manualColor = hsvToRgb(hsv)) }; refresh() }

    fun setManualHex(hex: String) {
        val rgb = hexToRgb(hex)
        val hsv = FloatArray(3).also { android.graphics.Color.RGBToHSV(rgb.r, rgb.g, rgb.b, it) }.toList()
        _state.update { it.copy(hsv = hsv, manualColor = rgb) }
        refresh()
    }

    fun setKind(k: String) { _state.update { it.copy(kind = k) }; refresh() }
    fun setName(n: String) = _state.update { it.copy(name = n.take(30), nameEdited = true) }
    fun showError(message: String) = _state.update { it.copy(error = message) }

    fun save() {
        val s = _state.value
        val color = s.color ?: return
        val kind = s.kind ?: return
        if (!s.canSave) return
        _state.update { it.copy(saved = true) }
        viewModelScope.launch {
            val id = System.currentTimeMillis()
            val file = if (s.mode == AddMode.PHOTO) s.photo?.let { closet.saveImage(it, id) } else null
            val v = s.verdict
            closet.add(
                ClosetItem(
                    id = id, hex = color.toHex(), name = s.name.trim().ifBlank { defaultName(s) },
                    category = CLOTHING_KINDS[kind]?.second ?: "기타", toneId = s.tone?.id.orEmpty(),
                    verdict = v?.level?.verdict ?: Verdict.OK, score = v?.score ?: 50,
                    imageFile = file, kind = kind,
                ),
            )
        }
    }

    /** Re-judges the color and refreshes the suggested name unless the user typed one. */
    private fun refresh() = _state.update { s ->
        val c = s.color
        val v = if (c != null && s.tone != null) ColorMatcher.evaluate(c, s.tone, s.tones) else null
        val next = s.copy(verdict = v)
        if (s.nameEdited) next else next.copy(name = defaultName(next))
    }

    private fun defaultName(s: ClosetAddUiState): String {
        val c = s.color ?: return ""
        val colorName = s.verdict?.nameHint ?: nearestName(c, s.tones) ?: c.toHex()
        return listOfNotNull(colorName, s.kind?.let { CLOTHING_KINDS[it]?.first }).joinToString(" ")
    }

    private fun nearestName(c: Rgb, tones: List<Tone>): String? {
        val lab = com.gun.personalcolor.core.analysis.ColorMath.rgbToLab(c)
        return tones.flatMap { it.bestColors + it.worstColors }
            .minByOrNull { deltaE2000(lab, com.gun.personalcolor.core.analysis.ColorMath.rgbToLab(hexToRgb(it.hex))) }?.name
    }

    private fun sampleOpaque(bmp: Bitmap, cx: Int, cy: Int): Rgb? {
        val r = sampleRadius(bmp)
        val x0 = max(0, cx - r); val y0 = max(0, cy - r)
        val w = min(bmp.width, cx + r + 1) - x0; val h = min(bmp.height, cy + r + 1) - y0
        val buf = IntArray(w * h).also { bmp.getPixels(it, 0, w, x0, y0, w, h) }
        var n = 0; var rs = 0L; var gs = 0L; var bs = 0L
        for (p in buf) if ((p ushr 24) >= 200) { n++; rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF }
        return if (n == 0) null else Rgb((rs / n).toInt(), (gs / n).toInt(), (bs / n).toInt())
    }

    private fun scaleDown(b: Bitmap, maxSide: Int): Bitmap {
        val s = maxSide.toFloat() / max(b.width, b.height)
        return if (s >= 1f) b else Bitmap.createScaledBitmap(b, (b.width * s).roundToInt(), (b.height * s).roundToInt(), true)
    }
}

internal fun hsvToRgb(hsv: List<Float>): Rgb {
    val c = android.graphics.Color.HSVToColor(hsv.toFloatArray())
    return Rgb((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
}
