package com.gun.personalcolor.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class AdSlotTest {
    @Test fun premiumSeesNothingEvenWhenAdFailed() {
        for (loaded in listOf(true, false)) for (failed in listOf(true, false))
            assertEquals(AdSlot.NOTHING, adSlot(adFree = true, loaded = loaded, failed = failed))
    }

    @Test fun loadingTakesNoSpace() = assertEquals(AdSlot.NOTHING, adSlot(adFree = false, loaded = false, failed = false))

    @Test fun failedShowsHouseCard() = assertEquals(AdSlot.HOUSE, adSlot(adFree = false, loaded = false, failed = true))

    @Test fun loadedAdWins() = assertEquals(AdSlot.AD, adSlot(adFree = false, loaded = true, failed = false))

    @Test fun promoUrlCarriesUtm() = assertEquals(
        "https://play.google.com/store/apps/details?id=com.pass.diary&referrer=utm_source%3Dpersonalcolor%26utm_medium%3Dsettings_row",
        OnDiaryPromo.url("settings_row"),
    )
}
