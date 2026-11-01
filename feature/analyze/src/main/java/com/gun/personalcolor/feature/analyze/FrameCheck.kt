package com.gun.personalcolor.feature.analyze

import androidx.compose.ui.geometry.Offset
import com.gun.personalcolor.core.analysis.ColorMath
import com.gun.personalcolor.core.analysis.Lab
import com.gun.personalcolor.core.analysis.Rgb
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

/** Capture conditions checked on every analyzed preview frame. */
enum class Check(val label: String) {
    FACE("얼굴 1명"), CENTER("중앙"), SIZE("크기"), FRONT("정면"), LIGHT("밝기"), EVEN("조명 균일"), TINT("조명 색"),
}

data class FrameCheck(val failed: Set<Check>, val hint: String) {
    val ok get() = failed.isEmpty()

    companion object {
        val Waiting = FrameCheck(Check.entries.toSet(), "얼굴을 타원 안에 맞춰 주세요")
    }
}

/** Center of the oval guide, as a fraction of the frame (slightly above the middle). */
internal const val GUIDE_CENTER_Y = 0.46f

// Face Mesh landmark indices.
private const val EYE_OUTER_L = 33
private const val EYE_OUTER_R = 263
private const val NOSE_TIP = 1
private const val FACE_SIDE_L = 234
private const val FACE_SIDE_R = 454
private const val CHEEK_L = 50
private const val CHEEK_R = 280
private const val FOREHEAD = 151

/**
 * Judges one upright frame. [faces] = landmarks (normalized 0..1) of each detected face; [pixels] = ARGB of a [w]x[h] frame.
 * Thresholds are deliberately lenient: the goal is to stop clearly bad shots, not to nag.
 */
internal fun checkFrame(faces: List<List<Offset>>, pixels: IntArray, w: Int, h: Int): FrameCheck {
    if (faces.isEmpty()) return FrameCheck.Waiting
    if (faces.size > 1) return FrameCheck.Waiting.copy(hint = "한 명만 화면에 나오게 해 주세요")
    val p = faces[0]
    val failed = mutableSetOf<Check>()
    var hint: String? = null
    fun fail(c: Check, h: String) { failed += c; if (hint == null) hint = h }

    val minX = p.minOf { it.x }; val maxX = p.maxOf { it.x }
    val minY = p.minOf { it.y }; val maxY = p.maxOf { it.y }
    val height = maxY - minY
    when {
        height < 0.40f -> fail(Check.SIZE, "조금 더 가까이 와 주세요")
        height > 0.70f -> fail(Check.SIZE, "조금만 멀리 떨어져 주세요")
    }
    if (abs((minX + maxX) / 2 - 0.5f) > 0.12f || abs((minY + maxY) / 2 - GUIDE_CENTER_Y) > 0.12f) {
        fail(Check.CENTER, "얼굴을 타원 가운데로 옮겨 주세요")
    }
    // Roll: tilt of the eye line (in pixels). Yaw: where the nose tip sits between the face sides.
    val eyeL = p[EYE_OUTER_L]; val eyeR = p[EYE_OUTER_R]
    val roll = Math.toDegrees(atan2(((eyeR.y - eyeL.y) * h).toDouble(), ((eyeR.x - eyeL.x) * w).toDouble()))
    val rollOff = minOf(abs(roll), abs(abs(roll) - 180))
    val sideL = p[FACE_SIDE_L].x; val sideR = p[FACE_SIDE_R].x
    val yaw = if (sideR != sideL) (p[NOSE_TIP].x - sideL) / (sideR - sideL) else 0.5f
    if (rollOff > 12 || yaw !in 0.36f..0.64f) fail(Check.FRONT, "얼굴을 정면으로 향해 주세요")

    val r = max(2, ((maxX - minX) * w * 0.05f).roundToInt())
    val left = ColorMath.rgbToLab(patch(pixels, w, h, p[CHEEK_L], r))
    val right = ColorMath.rgbToLab(patch(pixels, w, h, p[CHEEK_R], r))
    val forehead = ColorMath.rgbToLab(patch(pixels, w, h, p[FOREHEAD], r))
    val skin = Lab((left.l + right.l + forehead.l) / 3, (left.a + right.a + forehead.a) / 3, (left.b + right.b + forehead.b) / 3)
    when {
        skin.l < 42 -> fail(Check.LIGHT, "더 밝은 곳으로 가 주세요")
        skin.l > 88 -> fail(Check.LIGHT, "빛이 너무 강해요 · 직사광선을 피해 주세요")
    }
    if (abs(left.l - right.l) > 10) fail(Check.EVEN, "한쪽만 밝아요 · 창을 정면으로 봐 주세요")
    // Typical skin sits around hue 40–65° in Lab. Only far-off casts fail.
    when {
        skin.hue > 82 || skin.b > 38 -> fail(Check.TINT, "노란 조명이에요 · 자연광에서 찍어 주세요")
        skin.b < 4 || skin.hue < 15 -> fail(Check.TINT, "푸른 조명이에요 · 자연광에서 찍어 주세요")
    }
    return FrameCheck(failed, hint ?: "좋아요! 그대로 잠시 멈춰 주세요")
}

/** Average color of a square patch around normalized point [c]. */
internal fun patch(pixels: IntArray, w: Int, h: Int, c: Offset, r: Int): Rgb {
    val cx = (c.x * w).roundToInt(); val cy = (c.y * h).roundToInt()
    var sr = 0L; var sg = 0L; var sb = 0L; var n = 0
    for (y in (cy - r).coerceAtLeast(0)..(cy + r).coerceAtMost(h - 1)) {
        for (x in (cx - r).coerceAtLeast(0)..(cx + r).coerceAtMost(w - 1)) {
            val px = pixels[y * w + x]
            sr += (px shr 16) and 0xFF; sg += (px shr 8) and 0xFF; sb += px and 0xFF; n++
        }
    }
    if (n == 0) return Rgb(0, 0, 0)
    return Rgb((sr / n).toInt(), (sg / n).toInt(), (sb / n).toInt())
}
