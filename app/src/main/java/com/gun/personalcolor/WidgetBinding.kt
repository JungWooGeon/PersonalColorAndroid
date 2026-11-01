package com.gun.personalcolor

import com.gun.personalcolor.core.ads.AdManager
import com.gun.personalcolor.core.ads.Billing
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
import java.time.LocalDate
import java.util.concurrent.TimeUnit
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
    private val billing: Billing,
    private val ads: AdManager,
) : WidgetOutfitSource {
    override suspend fun today(): WidgetOutfit? {
        val p = prefs.preferences.first()
        val tone = content.toneOrNull(p.myToneId) ?: return null
        val trialStart = p.coordiTrialStartMillis
        val inTrial = trialStart != null &&
            System.currentTimeMillis() - trialStart < TimeUnit.DAYS.toMillis(COORDI_TRIAL_DAYS.toLong())
        // Not started yet = the user never opened 오늘의 코디; show it locked so the trial starts in the app.
        val unlocked = billing.adFree.value || inTrial || ads.isUnlocked(AdManager.KEY_COORDI)
        if (!unlocked) return WidgetOutfit("", null, emptyList(), locked = true)
        val w = weather.today()
        val outfit = coordi.outfit(tone, w, closet.items.first(), 0, LocalDate.now())
        return WidgetOutfit(
            headline = outfit.headline,
            weatherLine = w?.let(TodayCoordiProvider::weatherLine),
            pieces = outfit.slots.filter { it.kind != null }.map { it.kind!! to it.color.hex },
            locked = false,
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetBindingModule {
    @Binds abstract fun widgetOutfit(impl: AppWidgetOutfitSource): WidgetOutfitSource
}
