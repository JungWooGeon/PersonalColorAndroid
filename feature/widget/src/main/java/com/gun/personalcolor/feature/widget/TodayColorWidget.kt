package com.gun.personalcolor.feature.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as FixedColor
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.designsystem.TintyColors
import com.gun.personalcolor.core.designsystem.TintyExpression
import com.gun.personalcolor.core.model.CLOTHING_KINDS
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.contentColorOn
import com.gun.personalcolor.core.ui.hexColor
import com.gun.personalcolor.core.ui.renderClothingBitmap
import com.gun.personalcolor.core.ui.tintyColors
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
import java.time.LocalTime
import java.util.Optional
import kotlin.math.max
import kotlin.math.min

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


// App palette (BRAND_SYSTEM tokens, no dynamic color) so the widget matches the app in light and dark.
private val Paper = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1E1C1B))
private val Card = ColorProvider(day = Color(0xFFF7F4F2), night = Color(0xFF2A2725))
private val Ink = ColorProvider(day = Color(0xFF1D1B1A), night = Color(0xFFEAE6E3))
private val Muted = ColorProvider(day = Color(0xFF66605B), night = Color(0xFFB8B0AA))
private val Accent = ColorProvider(day = Color(0xFF5B4B8A), night = Color(0xFFCBBEF5))
private val AccentFill = ColorProvider(day = Color(0xFF5B4B8A), night = Color(0xFF6E5DA6))
private val BrandPeach = Color(0xFFFFCBB0)

/** 틴티 by time of day: morning wink, daytime default, asleep from 22:00. */
private fun tintyMood(hour: Int = LocalTime.now().hour) = when (hour) {
    in 5..9 -> TintyExpression.Happy
    in 10..21 -> TintyExpression.Default
    else -> TintyExpression.Sleep
}

/** Same rule as the app home. */
private fun todayColor(tone: Tone): NamedColor = tone.bestColors[LocalDate.now().dayOfYear % tone.bestColors.size]

/**
 * Three sizes, three jobs:
 * - [Small] (compact 2x2): today's color, nothing else. Name big, 틴티 standing in a pool of that color.
 * - [Mid] (2x2 on 4-column homes and bigger squares): the same moment plus the colors that go with it, as paint dots.
 * - [Wide] (4x2): today's outfit as a little lookbook card that 틴티 presents (locked: a frosted teaser).
 * While our live wallpaper is on, 틴티 is already on the home screen, so the widget shows a drop of paint instead.
 */
private enum class Kind { Small, Mid, Wide }

private fun kindOf(size: DpSize) = when {
    size.width >= 250.dp && size.width >= size.height * 1.25f -> Kind.Wide
    min(size.width.value, size.height.value) >= 160f -> Kind.Mid
    else -> Kind.Small
}

/** One 틴티 per screen: our live wallpaper already shows one. */
private fun ourLiveWallpaperOn(context: Context) =
    runCatching { android.app.WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName }.getOrDefault(false)

/** Everything one update shows, loaded off the main thread. Garment bitmaps only exist when the outfit is unlocked. */
private class WidgetData(
    val tone: Tone?,
    val outfit: WidgetOutfit?,
    /** (CLOTHING_KINDS key, art) */
    val pieces: List<Pair<String, Bitmap>>,
    val labels: List<String>,
    /** Generic pieces in the tone's colors for the locked teaser. */
    val teaser: List<Pair<String, Bitmap>>,
    val mood: TintyExpression,
    /** False while our live wallpaper shows 틴티. */
    val character: Boolean,
)

/** Overrides are for debug snapshots only; the real widget always asks the app and the system. */
class TodayColorWidget internal constructor(
    private val outfitOverride: WidgetOutfit?,
    private val characterOverride: Boolean? = null,
) : GlanceAppWidget() {
    constructor() : this(null)

    // Exact: the art is drawn for the real size (portrait and landscape), never stretched.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val tone = entry.content().toneOrNull(entry.preferences().preferences.first().myToneId)
            ?.takeIf { it.bestColors.isNotEmpty() }
        val outfit = if (tone == null) null else outfitOverride ?: runCatching { entry.outfitSource().orElse(null)?.today() }.getOrNull()
        val density = context.resources.displayMetrics.density
        val px = (120 * density).toInt().coerceIn(200, 360)
        val character = characterOverride ?: !ourLiveWallpaperOn(context)
        val data = withContext(Dispatchers.Default) {
            // Never draw (or even hold) the outfit while it's locked.
            val real = if (outfit == null || outfit.locked) emptyList() else outfit.pieces.take(4)
            val teaser = if (tone != null && outfit?.locked == true) {
                listOf("coat", "knit", "slacks").mapIndexed { i, kind ->
                    kind to renderClothingBitmap(kind, hexColor(tone.bestColors[(i * 3 + 1) % tone.bestColors.size].hex), px / 2, density)
                }
            } else emptyList()
            WidgetData(
                tone, outfit,
                real.map { (kind, hex) -> kind to renderClothingBitmap(kind, hexColor(hex), px, density) },
                real.map { (kind, _) -> CLOTHING_KINDS[kind]?.first ?: kind },
                teaser, tintyMood(), character,
            )
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val open = launch?.let { if (tone == null) actionStartActivity(it) else actionStartActivity(coordiIntent(it)) }
        provideContent {
            var modifier = GlanceModifier.fillMaxSize().appWidgetBackground().background(Paper).widgetRadius()
            if (open != null) modifier = modifier.clickable(open)
            Box(modifier) { WidgetContent(data, density) }
        }
    }
}

/** Launcher intent + the extra MainActivity turns into the daily-outfit screen. */
private fun coordiIntent(launch: Intent) = Intent(launch).putExtra("open", "coordi")
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

private fun GlanceModifier.widgetRadius() =
    if (Build.VERSION.SDK_INT >= 31) cornerRadius(android.R.dimen.system_app_widget_background_radius) else cornerRadius(24.dp)

private fun GlanceModifier.innerRadius() =
    if (Build.VERSION.SDK_INT >= 31) cornerRadius(android.R.dimen.system_app_widget_inner_radius) else cornerRadius(16.dp)

private const val PAD = 14f

/** Layout of one size in dp; the art bitmap and the Glance text are both placed from it. */
private class Geometry(val kind: Kind, val w: Float, val h: Float) {
    val tinty: Float
    val tintyX: Float
    val surface: Float
    /** Width the name may use without running into 틴티. */
    val textWidth: Float
    var cardLeft = 0f

    /** Portrait cells (2x2 on many phones): 틴티 centered under the name instead of off to the side. */
    val tall = h >= w * 1.25f

    init {
        when {
            kind != Kind.Wide && tall -> {
                tinty = min(w * .7f, h * (if (kind == Kind.Mid) .38f else .42f))
                surface = h - max(h * (if (kind == Kind.Mid) .3f else .26f), if (kind == Kind.Mid) 66f else 46f)
                tintyX = w / 2; textWidth = w - 2 * PAD
            }
            kind == Kind.Small -> {
                tinty = min(h * .58f, w * .6f); surface = h * .72f
                tintyX = w - 8f - tinty * .31f; textWidth = w * .64f - PAD
            }
            kind == Kind.Mid -> {
                tinty = min(h * .56f, w * .56f); surface = h * .64f
                tintyX = w - 10f - tinty * .31f; textWidth = w * .64f - PAD
            }
            else -> {
                cardLeft = w - 12f - max(150f, w * .5f)
                // Keep at least ~80dp for the name next to 틴티.
                tinty = min(h * .66f, (cardLeft - 8f - PAD - 80f) / .6f).coerceAtLeast(40f)
                surface = h * .7f
                tintyX = cardLeft - 8f - tinty * .31f // hands clear of the card
                textWidth = tintyX - tinty * .29f - PAD - 4f
            }
        }
    }

    fun scene(density: Float, dots: List<Int>, character: Boolean) = WidgetArt.Scene(
        w * density, h * density, density, surface * density, tinty * density, tintyX * density, character,
        card = if (kind == Kind.Wide) RectF(cardLeft * density, 12 * density, (w - 12) * density, (h - 12) * density) else null,
        dots = if (kind == Kind.Mid) dots else emptyList(),
        dotY = (h - PAD - 9) * density, dotR = 9 * density, dotX = PAD * density,
    )
}

@Composable
private fun WidgetContent(d: WidgetData, density: Float) {
    val size = LocalSize.current
    val tone = d.tone
    val kind = if (tone == null) Kind.Small else kindOf(size)
    val g = Geometry(kind, size.width.value, size.height.value)
    val color = tone?.let { hexColor(todayColor(it).hex) } ?: BrandPeach
    val art = remember(size) {
        val dots = tone?.let { t -> val i = t.bestColors.indexOf(todayColor(t)); (1..3).map { hexColor(t.bestColors[(i + it * 2) % t.bestColors.size].hex).toArgb() } }
        WidgetArt.render(g.scene(density, dots.orEmpty(), d.character), color.toArgb(), tone?.tintyColors ?: TintyColors.Brand, d.mood)
    }
    val onPool = FixedColor(contentColorOn(color).copy(alpha = .86f))
    Image(ImageProvider(art), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    if (tone == null) {
        Column(GlanceModifier.fillMaxSize().padding(PAD.dp)) {
            Label("반가워요")
            Spacer(GlanceModifier.height(2.dp))
            Name("내 컬러를\n찾아볼까요?", 17.sp, g.textWidth)
        }
        PoolLine("탭해서 진단해요", onPool)
        return
    }
    val today = todayColor(tone)
    Column(GlanceModifier.fillMaxSize().padding(start = PAD.dp, top = PAD.dp, end = PAD.dp)) {
        Label("오늘의 컬러")
        Spacer(GlanceModifier.height(2.dp))
        // As big as the space allows; long names (라이트 페리윙클) step down so they still fit in two lines.
        val big = when { kind == Kind.Mid -> 28f; kind == Kind.Small -> 24f; g.h < 140f -> 19f; else -> 23f }
        Name(today.name, (if (today.name.length >= 6) big - 4 else big).sp, g.textWidth)
    }
    when (kind) {
        Kind.Small -> PoolLine(tone.nameKo, onPool)
        Kind.Mid -> PoolLine("같이 입으면 예뻐요", onPool, bottom = PAD + 18 + 6)
        Kind.Wide -> {
            if (g.h >= 140f) PoolLine(tone.nameKo, onPool) // short 4x2: the name needs that room
            Box(GlanceModifier.fillMaxSize().padding(start = g.cardLeft.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
                LookCard(d, tone, g, density)
            }
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text, style = TextStyle(color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)

@Composable
private fun Name(text: String, size: TextUnit, width: Float) =
    Text(text, style = TextStyle(color = Ink, fontSize = size, fontWeight = FontWeight.Bold), maxLines = 2,
        modifier = GlanceModifier.width(width.dp))

/** A short line sitting in the paint, bottom-left. */
@Composable
private fun PoolLine(text: String, color: FixedColor, bottom: Float = PAD - 2) =
    Box(GlanceModifier.fillMaxSize().padding(start = PAD.dp, bottom = bottom.dp), contentAlignment = Alignment.BottomStart) {
        Text(text, style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }

/** 4x2 right side: today's outfit as a mini lookbook; locked = frosted teaser; no outfit source = palette. */
@Composable
private fun LookCard(d: WidgetData, tone: Tone, g: Geometry, density: Float) {
    val outfit = d.outfit
    val roomy = g.h >= 150f
    Column(GlanceModifier.fillMaxSize().background(Card).innerRadius().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Label("오늘의 코디")
            val weather = outfit?.weatherLine?.takeIf { !outfit.locked }
            if (weather != null) {
                Spacer(GlanceModifier.defaultWeight())
                Text(weather, style = TextStyle(color = Muted, fontSize = 10.sp), maxLines = 1)
            }
        }
        // Image height left after the label (and the headline when there's room).
        val twoLines = g.h >= 175f
        val imageH = g.h - 24 - 20 - 14 - (if (twoLines) 40 else if (roomy) 22 else 0)
        val boardW = ((g.w - 12 - g.cardLeft - 24) * density).toInt().coerceAtLeast(24)
        val boardH = (imageH * density).toInt().coerceAtLeast(24)
        when {
            outfit == null -> Palette(tone, GlanceModifier.defaultWeight())
            outfit.locked -> {
                val img = remember(boardW, boardH) { WidgetArt.lookbook(d.teaser, boardW, boardH, density, frosted = true) }
                Box(GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(img), contentDescription = null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    Row(GlanceModifier.background(AccentFill).cornerRadius(999.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(ImageProvider(R.drawable.widget_lock), contentDescription = null, modifier = GlanceModifier.size(13.dp))
                        Spacer(GlanceModifier.width(4.dp))
                        Text("열어보기", style = TextStyle(color = FixedColor(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                }
                if (roomy) Text("오늘 입을 옷이 준비됐어요", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
            else -> {
                val img = remember(boardW, boardH) { WidgetArt.lookbook(d.pieces, boardW, boardH, density, frosted = false) }
                Image(ImageProvider(img), contentDescription = d.labels.joinToString(", "),
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentScale = ContentScale.Fit)
                if (roomy) Text(outfit.headline, style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = if (twoLines) 2 else 1)
            }
        }
    }
}

/** No outfit source: the colors that go with today's, as chips. */
@Composable
private fun Palette(tone: Tone, modifier: GlanceModifier) {
    val index = tone.bestColors.indexOf(todayColor(tone))
    val more = (1..4).map { tone.bestColors[(index + it * 2) % tone.bestColors.size] }.distinct()
    Column(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("같이 입으면 예뻐요", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Spacer(GlanceModifier.height(8.dp))
        Row {
            more.forEachIndexed { i, c ->
                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                Box(GlanceModifier.size(22.dp).background(hexColor(c.hex)).cornerRadius(11.dp)) {}
            }
        }
    }
}
