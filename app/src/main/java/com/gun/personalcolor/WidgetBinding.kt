package com.gun.personalcolor

import com.gun.personalcolor.core.ads.AdManager
import com.gun.personalcolor.core.data.ClosetRepository
import com.gun.personalcolor.core.data.ContentRepository
import com.gun.personalcolor.core.data.UserPreferencesRepository
import com.gun.personalcolor.core.model.COORDI_TRIAL_DAYS
import com.gun.personalcolor.core.weather.WeatherRepository
import com.gun.personalcolor.feature.coordi.TodayCoordiProvider
import com.gun.personalcolor.feature.widget.WidgetOutfit
import com.gun.personalcolor.feature.widget.WidgetOutfitSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import javax.inject.Inject

/**
 * Today's outfit for the home-screen widget. The widget must never be a way around the app's ad gate:
 * [WidgetOutfit.locked] is true unless the user is in the free trial, has premium, or unlocked today with an ad.
 */
class AppWidgetOutfitSource @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val content: ContentRepository,
    private val closet: ClosetRepository,
    private val weather: WeatherRepository,
    private val coordi: TodayCoordiProvider,
    private val ads: AdManager,
) : WidgetOutfitSource {
    override suspend fun today(): WidgetOutfit? {
        val p = prefs.preferences.first()
        val tone = content.toneOrNull(p.myToneId) ?: return null
        // AdManager.isUnlocked already covers premium (Billing.adFree) and today's reward ad.
        val unlocked = coordiOpen(p.coordiTrialStartMillis, System.currentTimeMillis(), ads.isUnlocked(AdManager.KEY_COORDI))
        if (!unlocked) return WidgetOutfit("", null, emptyList(), locked = true)
        // Never hang the widget on the network; no weather = the outfit without it, as on failure.
        val w = withTimeoutOrNull(WEATHER_TIMEOUT_MS) { weather.today() }
        val outfit = coordi.outfit(tone, w, closet.items.first(), 0, LocalDate.now())
        return WidgetOutfit(
            headline = outfit.headline,
            weatherLine = w?.let(TodayCoordiProvider::weatherLine),
            pieces = outfit.slots.mapNotNull { s -> s.kind?.let { it to s.color.hex } },
            locked = false,
        )
    }

    private companion object {
        /** Under the widget's 5 s OUTFIT_TIMEOUT_MS, or slow weather would show the outfit as locked. */
        const val WEATHER_TIMEOUT_MS = 4_000L
    }
}

/**
 * Same access rule as 오늘의 코디 (CoordiViewModel.trialDaysLeft + CoordiScreen): trial days left > 0, where a trial
 * not started yet counts as day one (opening the outfit starts it), or [unlocked] (premium or today's reward ad).
 */
internal fun coordiOpen(trialStart: Long?, now: Long, unlocked: Boolean): Boolean {
    // ponytail: mirrors CoordiViewModel's trialDaysLeft formula; move both to core/model once that module is free to edit.
    val start = trialStart ?: now
    return unlocked || COORDI_TRIAL_DAYS - (now - start) / DAY_MS > 0
}

private const val DAY_MS = 24 * 60 * 60_000L

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetBindingModule {
    @Binds abstract fun widgetOutfit(impl: AppWidgetOutfitSource): WidgetOutfitSource
}
