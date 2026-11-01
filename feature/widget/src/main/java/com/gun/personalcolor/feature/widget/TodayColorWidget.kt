package com.gun.personalcolor.feature.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.contentColorOn
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.renderClothingBitmap
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Optional

/** Today's outfit for the 4x2 widget. The app binds a [WidgetOutfitSource]; without one the widget shows colors only. */
data class WidgetOutfit(
    /** e.g. "카멜 트렌치 + 아이보리 니트" */
    val headline: String,
    /** e.g. "영등포구 10° 이슬비"; null hides the line. */
    val weatherLine: String?,
    /** (CLOTHING_KINDS key, "#RRGGBB"), outer → bottom order; the first 3 are drawn. */
    val pieces: List<Pair<String, String>>,
    /** True when the user has no access right now (trial over, not premium, not unlocked today). Nothing above is shown then. */
    val locked: Boolean,
)

interface WidgetOutfitSource {
    suspend fun today(): WidgetOutfit?
}

@Module
@InstallIn(SingletonComponent::class)
interface WidgetModule {
    @BindsOptionalOf
    fun outfitSource(): WidgetOutfitSource
}

/** Called by the app when the tone, today's unlock, premium or the outfit changes. */
object TodayColorWidgets {
    suspend fun refresh(context: Context) = TodayColorWidget().updateAll(context)

    /** Launcher supports the "add this widget?" system prompt (most launchers on Android 8+). */
    fun canPin(context: Context): Boolean =
        android.appwidget.AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    /** Shows the launcher's "홈 화면에 추가할까요?" prompt. Returns false if the launcher can't do it. */
    fun requestPin(context: Context): Boolean {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(android.content.ComponentName(context, TodayColorWidgetReceiver::class.java), null, null)
    }
}

class TodayColorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayColorWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun preferences(): UserPreferencesRepository
    fun content(): ContentRepository
    fun outfitSource(): Optional<WidgetOutfitSource>
}

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)

// App palette (no dynamic color) so the widget matches the app in light and dark.
private val Bg = ColorProvider(day = Color(0xFFFAF8F6), night = Color(0xFF201E1C))
private val Ink = ColorProvider(day = Color(0xFF1D1B1A), night = Color(0xFFEAE6E3))
private val Muted = ColorProvider(day = Color(0xFF6F6862), night = Color(0xFFB5ADA6))
private val Surface = ColorProvider(day = Color(0xFFF0EBE6), night = Color(0xFF2C2926))

private fun fixed(color: Color) = ColorProvider(day = color, night = color)

/** A drawn garment, ready for RemoteViews. */
private class Piece(val bitmap: Bitmap, val label: String)

class TodayColorWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val tone = entry.content().toneOrNull(entry.preferences().preferences.first().myToneId)
            ?.takeIf { it.bestColors.isNotEmpty() }
        val outfit = if (tone == null) null else runCatching { entry.outfitSource().orElse(null)?.today() }.getOrNull()
        // Never draw (or even hold) the outfit while it's locked.
        val pieces = if (outfit == null || outfit.locked) emptyList() else withContext(Dispatchers.Default) {
            val density = context.resources.displayMetrics.density
            val px = (96 * density).toInt().coerceIn(160, 288)   // crisp at the larger flat-lay size
            outfit.pieces.take(3).map { (kind, hex) ->
                Piece(renderClothingBitmap(kind, hexColor(hex), px, density), CLOTHING_KINDS[kind]?.first ?: kind)
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = launch?.let { if (tone == null) actionStartActivity(it) else actionStartActivity(coordiIntent(it)) }
        provideContent {
            var modifier = GlanceModifier.fillMaxSize().appWidgetBackground().background(Bg).widgetRadius().padding(12.dp)
            if (open != null) modifier = modifier.clickable(open)
            Box(modifier, contentAlignment = Alignment.Center) {
                when {
                    tone == null -> NoTone()
                    LocalSize.current.width >= WIDE.width -> Wide(tone, outfit, pieces)
                    else -> Small(tone)
                }
            }
        }
    }
}

/** Launcher intent + the extra MainActivity turns into the daily-outfit screen. */
private fun coordiIntent(launch: Intent) = Intent(launch).putExtra("open", "coordi")
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

private fun GlanceModifier.widgetRadius() =
    if (Build.VERSION.SDK_INT >= 31) cornerRadius(android.R.dimen.system_app_widget_background_radius) else cornerRadius(24.dp)

private fun GlanceModifier.innerRadius() =
    if (Build.VERSION.SDK_INT >= 31) cornerRadius(android.R.dimen.system_app_widget_inner_radius) else cornerRadius(14.dp)

/** Same rule as the app home. */
private fun todayColor(tone: Tone): NamedColor = tone.bestColors[LocalDate.now().dayOfYear % tone.bestColors.size]

/** Rounded swatch with "오늘의 컬러" on top and the color name at the bottom. */
@Composable
private fun ColorCard(color: NamedColor, modifier: GlanceModifier, big: Boolean) {
    val bg = hexColor(color.hex)
    val ink = fixed(contentColorOn(bg))
    Column(modifier.background(bg).innerRadius().padding(12.dp)) {
        Text("오늘의 컬러", style = TextStyle(color = ink, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        Spacer(GlanceModifier.defaultWeight())
        Text(
            color.name,
            style = TextStyle(color = ink, fontSize = if (big) 18.sp else 16.sp, fontWeight = FontWeight.Bold),
            maxLines = 2,
        )
    }
}

@Composable
private fun Small(tone: Tone) {
    Column(GlanceModifier.fillMaxSize()) {
        ColorCard(todayColor(tone), GlanceModifier.fillMaxWidth().defaultWeight(), big = false)
        Spacer(GlanceModifier.height(8.dp))
        Text(
            tone.nameKo,
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
            style = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun Wide(tone: Tone, outfit: WidgetOutfit?, pieces: List<Piece>) {
    val size = LocalSize.current
    // Square color card as tall as the content area; the outfit takes the rest.
    val inner = size.height - 24.dp
    val card = minOf(inner, (size.width - 24.dp) * 0.45f)
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        ColorCard(todayColor(tone), GlanceModifier.width(card).fillMaxHeight(), big = true)
        Spacer(GlanceModifier.width(12.dp))
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            when {
                outfit == null -> Palette(tone)
                outfit.locked -> Locked(tone)
                else -> Outfit(outfit, pieces, roomy = inner >= 130.dp)
            }
        }
    }
}

@Composable
private fun ColumnScope.Outfit(outfit: WidgetOutfit, pieces: List<Piece>, roomy: Boolean) {
    // Mini flat-lay on a soft surface.
    Box(
        GlanceModifier.fillMaxWidth().defaultWeight().background(Surface).innerRadius().padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Pieces share the board width equally and fill its height, so they read clearly at a glance.
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            pieces.forEach { piece ->
                Image(
                    ImageProvider(piece.bitmap),
                    contentDescription = piece.label,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
    Spacer(GlanceModifier.height(8.dp))
    Text(
        outfit.headline,
        style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        maxLines = if (roomy) 2 else 1,
    )
    outfit.weatherLine?.let {
        Text(it, style = TextStyle(color = Muted, fontSize = 11.sp), maxLines = 1)
    }
}

/** Teaser while the outfit is ad-gated: blurred-looking palette blocks + lock, nothing of the real outfit. */
@Composable
private fun ColumnScope.Locked(tone: Tone) {
    val colors = tone.bestColors.take(3)
    Box(
        GlanceModifier.fillMaxWidth().defaultWeight().background(Surface).innerRadius().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            colors.forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                Box(GlanceModifier.size(28.dp).background(hexColor(c.hex).copy(alpha = 0.35f)).cornerRadius(10.dp)) {}
            }
        }
        Image(
            ImageProvider(R.drawable.widget_lock),
            contentDescription = "잠김",
            modifier = GlanceModifier.size(28.dp),
        )
    }
    Spacer(GlanceModifier.height(8.dp))
    Text("앱에서 오늘의 코디 보기", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 2)
}

/** No outfit source yet: today's next best colors. */
@Composable
private fun Palette(tone: Tone) {
    val index = tone.bestColors.indexOf(todayColor(tone))
    val more = (1..3).map { tone.bestColors[(index + it) % tone.bestColors.size] }.distinct()
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(tone.nameKo, style = TextStyle(color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        Text("같이 입기 좋은 컬러", style = TextStyle(color = Muted, fontSize = 11.sp), maxLines = 1)
        Spacer(GlanceModifier.height(10.dp))
        Row {
            more.forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(8.dp))
                Box(GlanceModifier.size(26.dp).background(hexColor(c.hex)).cornerRadius(13.dp)) {}
            }
        }
    }
}

@Composable
private fun NoTone() {
    Column(
        GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row {
            listOf(0xFFF88379, 0xFFE8C07D, 0xFF8FB8DE, 0xFF7E5A9B).forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                Box(GlanceModifier.size(18.dp).background(Color(c)).cornerRadius(9.dp)) {}
            }
        }
        Spacer(GlanceModifier.height(12.dp))
        Text(
            "진단하고 오늘의 컬러 받기",
            style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 2,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text("탭해서 시작해요", style = TextStyle(color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center), maxLines = 1)
    }
}
