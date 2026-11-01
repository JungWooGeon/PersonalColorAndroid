package com.gun.personalcolor.feature.colormatch

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gun.personalcolor.core.analysis.Rgb
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.model.ClosetItem
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.vision.BitmapLoader
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

enum class ColorSource { NONE, PHOTO, MANUAL }

data class ColorMatchUiState(
    val ready: Boolean = false,          // preferences + tones loaded
    val tones: List<Tone> = emptyList(),
    val myToneId: String? = null,
    val refTone: Tone? = null,           // tone the color is judged against (my tone unless switched here)
    val source: ColorSource = ColorSource.NONE,
    val photo: Bitmap? = null,
    val photoLoading: Boolean = false,
    val pointX: Float = 0.5f,            // picked point as a fraction of the photo
    val pointY: Float = 0.5f,
    val hsv: List<Float> = listOf(18f, 0.7f, 0.75f),
    val color: Rgb? = null,
    val verdict: ColorVerdict? = null,
    val commit: Int = 0,                 // bumps when a pick is final (photo loaded, tap, drag end, slider release)
    val error: String? = null,
)

@HiltViewModel
class ColorMatchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val content: ContentRepository,
    private val prefs: UserPreferencesRepository,
    private val closet: ClosetRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ColorMatchUiState())
    val state: StateFlow<ColorMatchUiState> = _state.asStateFlow()

    private var overrideToneId: String? = null
    private var sampleBuf = IntArray(0)

    init {
        viewModelScope.launch {
            val tones = withContext(Dispatchers.IO) { content.tones }
            prefs.preferences.collect { p ->
                _state.update { it.copy(ready = true, tones = tones, myToneId = p.myToneId) }
                refreshTone()
            }
        }
    }

    /** From the empty state: saves the tone as "my tone". */
    fun setMyTone(id: String) {
        overrideToneId = null
        viewModelScope.launch { prefs.setMyTone(id) }
    }

    /** From the "기준 톤" chip: only changes the reference on this screen. */
    fun setReferenceTone(id: String) {
        overrideToneId = id.takeIf { it != _state.value.myToneId }
        refreshTone()
        _state.update { it.copy(commit = if (it.verdict != null) it.commit + 1 else it.commit) }
    }

    private fun refreshTone() {
        val s = _state.value
        val id = overrideToneId ?: s.myToneId
        _state.update { it.copy(refTone = s.tones.firstOrNull { t -> t.id == id }) }
        evaluate()
    }

    fun loadPhoto(uri: Uri) {
        _state.update { it.copy(photoLoading = true, error = null) }
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) { BitmapLoader.load(context, uri, 1600) }
            if (bmp == null) {
                _state.update { it.copy(photoLoading = false, error = "사진을 불러오지 못했어요. 다른 사진으로 시도해 주세요.") }
                return@launch
            }
            val r = sampleRadius(bmp)
            sampleBuf = IntArray((2 * r + 1) * (2 * r + 1))
            _state.update { it.copy(photo = bmp, photoLoading = false, source = ColorSource.PHOTO) }
            pick(0.5f, 0.5f, final = true)
        }
    }

    /** Saves the current color + verdict to "내 옷장" (color only, never the photo). */
    fun saveToCloset(name: String, category: String) {
        val s = _state.value
        val color = s.color ?: return
        val tone = s.refTone ?: return
        val v = s.verdict ?: return
        viewModelScope.launch {
            closet.add(ClosetItem(System.currentTimeMillis(), color.toHex(), name.trim(), category, tone.id, v.level.verdict, v.score))
        }
    }

    fun showError(message: String) = _state.update { it.copy(error = message) }

    /** Called on every drag frame; cheap: reads only the pixels inside the sample square. */
    fun pick(fx: Float, fy: Float, final: Boolean) {
        val bmp = _state.value.photo ?: return
        val x = fx.coerceIn(0f, 1f)
        val y = fy.coerceIn(0f, 1f)
        val rgb = sample(bmp, (x * (bmp.width - 1)).roundToInt(), (y * (bmp.height - 1)).roundToInt())
        _state.update { it.copy(pointX = x, pointY = y, color = rgb) }
        evaluate()
        if (final) commit()
    }

    fun commit() = _state.update { it.copy(commit = it.commit + 1) }

    fun startManual() {
        _state.update { it.copy(source = ColorSource.MANUAL, error = null) }
        setHsv(_state.value.hsv, final = true)
    }

    fun backToPhoto() {
        if (_state.value.photo == null) return
        _state.update { it.copy(source = ColorSource.PHOTO) }
        val s = _state.value
        pick(s.pointX, s.pointY, final = true)
    }

    fun setHsv(hsv: List<Float>, final: Boolean = false) {
        val c = android.graphics.Color.HSVToColor(hsv.toFloatArray())
        _state.update { it.copy(hsv = hsv, color = Rgb((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)) }
        evaluate()
        if (final) commit()
    }

    private fun evaluate() {
        val s = _state.value
        val v = if (s.color != null && s.refTone != null && s.source != ColorSource.NONE)
            ColorMatcher.evaluate(s.color, s.refTone, s.tones) else null
        _state.update { it.copy(verdict = v) }
    }

    /** Average color in a square of radius ~1.2% of the shorter side around (cx, cy). */
    private fun sample(bmp: Bitmap, cx: Int, cy: Int): Rgb {
        val r = sampleRadius(bmp)
        val x0 = max(0, cx - r)
        val y0 = max(0, cy - r)
        val w = min(bmp.width, cx + r + 1) - x0
        val h = min(bmp.height, cy + r + 1) - y0
        if (sampleBuf.size < w * h) sampleBuf = IntArray(w * h)
        bmp.getPixels(sampleBuf, 0, w, x0, y0, w, h)
        var rs = 0L; var gs = 0L; var bs = 0L
        for (i in 0 until w * h) {
            val p = sampleBuf[i]
            rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF
        }
        val n = w * h
        return Rgb((rs / n).toInt(), (gs / n).toInt(), (bs / n).toInt())
    }
}

internal fun sampleRadius(bmp: Bitmap) = max(3, (min(bmp.width, bmp.height) * 0.012f).roundToInt())
