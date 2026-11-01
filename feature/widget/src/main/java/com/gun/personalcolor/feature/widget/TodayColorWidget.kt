package com.gun.personalcolor.feature.widget

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.NamedColor
import com.gun.personalcolor.core.model.Tone
import com.gun.personalcolor.core.ui.contentColorOn
import com.gun.personalcolor.core.ui.hexColor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Called by the app when the user's tone changes. */
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
}

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)

// Fixed app colors (no dynamic color) so the widget matches the app.
private val WidgetColors = ColorProviders(
    light = lightColorScheme(background = Color(0xFFFAF8F6), onBackground = Color(0xFF1D1B1A)),
    dark = darkColorScheme(background = Color(0xFF201E1C), onBackground = Color(0xFFEAE6E3)),
)

class TodayColorWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val tone = entry.content().toneOrNull(entry.preferences().preferences.first().myToneId)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                var modifier = GlanceModifier.fillMaxSize()
                    .appWidgetBackground()
                    .background(GlanceTheme.colors.background)
                    .cornerRadius(24.dp)
                    .padding(10.dp)
                if (launch != null) modifier = modifier.clickable(actionStartActivity(launch))
                Box(modifier, contentAlignment = Alignment.Center) {
                    when {
                        tone == null || tone.bestColors.isEmpty() -> NoTone()
                        LocalSize.current.width >= WIDE.width -> Wide(tone)
                        else -> Small(tone)
                    }
                }
            }
        }
    }
}

/** Same rule as the home screen. */
private fun todayIndex(tone: Tone) = LocalDate.now().dayOfYear % tone.bestColors.size

private fun fixed(color: Color) = ColorProvider(color, color)

@Composable
private fun Small(tone: Tone) {
    val color = tone.bestColors[todayIndex(tone)]
    val bg = hexColor(color.hex)
    val ink = fixed(contentColorOn(bg))
    Box(
        GlanceModifier.fillMaxSize().background(bg).cornerRadius(18.dp).padding(12.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column {
            Text("오늘의 컬러", style = TextStyle(color = ink, fontSize = 11.sp), maxLines = 1)
            Text(color.name, style = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(tone.nameKo, style = TextStyle(color = ink, fontSize = 10.sp), maxLines = 1)
        }
    }
}

@Composable
private fun Wide(tone: Tone) {
    val index = todayIndex(tone)
    val color = tone.bestColors[index]
    val ink = GlanceTheme.colors.onBackground
    // Next 3 best colors after today's, wrapping around.
    val more: List<NamedColor> = (1..3).map { tone.bestColors[(index + it) % tone.bestColors.size] }.distinct() - color
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(GlanceModifier.size(90.dp).background(hexColor(color.hex)).cornerRadius(18.dp)) {}
        Spacer(GlanceModifier.width(14.dp))
        Column(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            Text("오늘의 컬러", style = TextStyle(color = ink, fontSize = 12.sp), maxLines = 1)
            Text(color.name, style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Text(tone.nameKo, style = TextStyle(color = ink, fontSize = 11.sp), maxLines = 1)
            if (more.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Row {
                    more.forEach {
                        Box(GlanceModifier.size(16.dp).background(hexColor(it.hex)).cornerRadius(8.dp)) {}
                        Spacer(GlanceModifier.width(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NoTone() {
    val ink = GlanceTheme.colors.onBackground
    Column(GlanceModifier.fillMaxWidth().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "내 퍼스널컬러를 진단하고 오늘의 컬러를 받아보세요",
            style = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 3,
        )
        Spacer(GlanceModifier.height(8.dp))
        Text("진단하기 ›", style = TextStyle(color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

