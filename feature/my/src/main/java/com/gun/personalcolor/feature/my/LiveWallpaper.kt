package com.gun.personalcolor.feature.my

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.WallpaperRenderer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** What the live wallpaper draws. [toneId] null = follow my tone. */
internal data class LiveChoice(val style: WallpaperRenderer.Style, val dark: Boolean, val toneId: String?)

/** Small SharedPreferences of this module (keeps core:model untouched). */
internal object LiveWallpaperPrefs {
    private fun sp(context: Context) = context.getSharedPreferences("live_wallpaper", Context.MODE_PRIVATE)

    fun read(context: Context): LiveChoice = sp(context).let { p ->
        val (style, dark) = decodeStyle(p.getString("style", null) ?: "")
        LiveChoice(if (style.animated) style else WallpaperRenderer.Style.AURORA, dark, p.getString("tone", null))
    }

    fun write(context: Context, choice: LiveChoice) =
        sp(context).edit().putString("style", encodeStyle(choice.style, choice.dark)).putString("tone", choice.toneId).apply()
}

internal fun isLiveWallpaperActive(context: Context): Boolean =
    WallpaperManager.getInstance(context).wallpaperInfo?.component == ComponentName(context, PersonalColorLiveWallpaper::class.java)

/** Opens the system preview of our live wallpaper (it has the apply button). */
internal fun liveWallpaperIntent(context: Context) = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, PersonalColorLiveWallpaper::class.java))

/** Third-party live wallpapers rarely show on the lock screen, so the lock screen gets the same design as a still. */
internal suspend fun setLiveLockFrame(context: Context, tone: Tone, choice: LiveChoice): Boolean =
    setWallpaper(context, WallpaperTarget.LOCK) { w, h -> WallpaperRenderer.render(tone, choice.style, w, h, choice.dark) }

/** The tone the live wallpaper uses: the chosen one, else my tone, else the first. */
internal suspend fun liveTone(context: Context, choice: LiveChoice): Tone? = withContext(Dispatchers.IO) {
    val entry = EntryPointAccessors.fromApplication(context.applicationContext, AutoWallpaperEntryPoint::class.java)
    val content = entry.content()
    content.toneOrNull(choice.toneId ?: entry.preferences().preferences.first().myToneId) ?: content.tones.firstOrNull()
}

/**
 * Animated versions of the soft styles. Draws on a hardware canvas at ~30 fps only while visible; stops (and drops the
 * sensor) as soon as it's hidden. Subtle parallax from home-screen page scrolling and device tilt.
 */
class PersonalColorLiveWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine = LiveEngine()

    private inner class LiveEngine : Engine(), Choreographer.FrameCallback, SensorEventListener {
        private val scope = MainScope()
        private val choreographer = Choreographer.getInstance()
        private val sensors = getSystemService(SensorManager::class.java)
        private val rotationSensor = sensors?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        private var tone: Tone? = null
        private var choice = LiveWallpaperPrefs.read(this@PersonalColorLiveWallpaper)
        private var width = 0
        private var height = 0
        private var running = false
        private val start = SystemClock.uptimeMillis()
        private var pageX = .5f

        // Tilt relative to a slowly following rest pose, so any holding angle is "neutral".
        private val rot = FloatArray(9)
        private val angles = FloatArray(3)
        private var rest: FloatArray? = null
        private var tiltX = 0f
        private var tiltY = 0f
        private var shownX = 0f
        private var shownY = 0f

        private var frameNs = 0L
        private var frames = 0

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            setOffsetNotificationsEnabled(true)
            load()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width; this.height = height
            drawFrame()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) = drawFrame()

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                load() // picks up a new style/tone chosen in the app
                if (!running) {
                    running = true
                    choreographer.postFrameCallback(this)
                    rest = null
                    rotationSensor?.let { sensors?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                }
            } else stop()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) = stop()

        override fun onDestroy() {
            stop(); scope.cancel()
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            pageX = xOffset
        }

        private fun stop() {
            running = false
            choreographer.removeFrameCallback(this)
            sensors?.unregisterListener(this)
        }

        private fun load() {
            scope.launch {
                choice = LiveWallpaperPrefs.read(applicationContext)
                tone = liveTone(applicationContext, choice)
                drawFrame()
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            drawFrame()
            choreographer.postFrameCallbackDelayed(this, FRAME_DELAY_MS)
        }

        private fun drawFrame() {
            val tone = tone ?: return
            val holder = surfaceHolder
            if (width == 0 || !holder.surface.isValid) return
            val begin = System.nanoTime()
            val canvas = try { holder.lockHardwareCanvas() } catch (e: Exception) { null } ?: return
            try {
                shownX += (tiltX - shownX) * .15f; shownY += (tiltY - shownY) * .15f // smooth the ~16 Hz sensor
                val tilt = width * .02f
                // Wraps daily; float seconds stay precise and the design changes at midnight anyway.
                val t = (SystemClock.uptimeMillis() - start) % 86_400_000L / 1000f
                WallpaperRenderer.draw(canvas, tone, choice.style, width, height, choice.dark, LocalDate.now(), t,
                    dx = (.5f - pageX) * width * .05f - shownX * tilt, dy = -shownY * tilt)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
            // adb shell setprop log.tag.PcLiveWallpaper DEBUG → average CPU time per frame.
            frameNs += System.nanoTime() - begin
            if (++frames == 300) {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "${choice.style} avg frame %.2f ms".format(frameNs / 300 / 1e6))
                frames = 0; frameNs = 0
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rot, event.values)
            SensorManager.getOrientation(rot, angles) // [1] pitch, [2] roll
            val r = rest ?: floatArrayOf(angles[1], angles[2]).also { rest = it }
            r[0] += (angles[1] - r[0]) * .02f; r[1] += (angles[2] - r[1]) * .02f
            tiltY = ((angles[1] - r[0]) / MAX_TILT).coerceIn(-1f, 1f)
            tiltX = ((angles[2] - r[1]) / MAX_TILT).coerceIn(-1f, 1f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private companion object {
        const val TAG = "PcLiveWallpaper"
        /** Next vsync after this → ~30 fps on 60/90/120 Hz screens. */
        const val FRAME_DELAY_MS = 25L
        /** ~20° of tilt gives the full shift. */
        const val MAX_TILT = .35f
    }
}
