package com.gun.personalcolor.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gun.personalcolor.core.model.ThemeMode
import com.gun.personalcolor.core.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun defaultsMatchModelDefaults() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        assertEquals(UserPreferences(), repo.preferences.first())
    }

    @Test fun settersRoundTrip() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        repo.setThemeMode(ThemeMode.DARK)
        repo.setOnboardingDone(true)
        repo.setSaveHistory(false)
        repo.setShowPhotoGuide(false)
        repo.setMyTone("winter_deep")
        repo.setDailyNotification(true, 21, 30)
        repo.setAutoWallpaper(true, "SOLID", "LOCK")
        val p = repo.preferences.first()
        assertEquals(ThemeMode.DARK, p.themeMode)
        assertEquals(true, p.onboardingDone)
        assertEquals(false, p.saveHistory)
        assertEquals(false, p.showPhotoGuide)
        assertEquals("winter_deep", p.myToneId)
        assertEquals(Triple(true, 21, 30), Triple(p.dailyNotification, p.notificationHour, p.notificationMinute))
        assertEquals(Triple(true, "SOLID", "LOCK"), Triple(p.autoWallpaper, p.autoWallpaperStyle, p.autoWallpaperTarget))

        repo.setMyTone(null)
        assertEquals(null, repo.preferences.first().myToneId)
    }

    @Test fun toggleFavoriteAddsThenRemoves() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        repo.toggleFavorite("a"); repo.toggleFavorite("b")
        assertEquals(setOf("a", "b"), repo.preferences.first().favoriteToneIds)
        repo.toggleFavorite("a")
        assertEquals(setOf("b"), repo.preferences.first().favoriteToneIds)
    }

    @Test fun trialStartsOnlyOnce() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        repo.startCoordiTrialIfNeeded(1_000)
        repo.startCoordiTrialIfNeeded(2_000)
        assertEquals(1_000L, repo.preferences.first().coordiTrialStartMillis)
    }

    @Test fun resetClearsAllButOnboardingAndTrial() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        repo.setThemeMode(ThemeMode.LIGHT)
        repo.setMyTone("autumn_mute")
        repo.toggleFavorite("x")
        repo.setDailyNotification(true, 7, 15)
        repo.startCoordiTrialIfNeeded(5_000)
        repo.reset()
        assertEquals(UserPreferences(onboardingDone = true, coordiTrialStartMillis = 5_000), repo.preferences.first())
        // The kept trial can't be restarted after a reset.
        repo.startCoordiTrialIfNeeded(9_000)
        assertEquals(5_000L, repo.preferences.first().coordiTrialStartMillis)
    }

    @Test fun resetWithoutTrialLeavesItUnset() = runTest {
        val repo = UserPreferencesRepository(testStore(tmp))
        repo.reset()
        assertEquals(UserPreferences(onboardingDone = true), repo.preferences.first())
    }

    @Test fun invalidThemeFallsBackToSystem() = runTest {
        val store = testStore(tmp)
        store.edit { it[stringPreferencesKey("theme_mode")] = "PURPLE" }
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository(store).preferences.first().themeMode)
    }
}
