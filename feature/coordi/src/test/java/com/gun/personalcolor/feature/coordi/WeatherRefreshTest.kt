package com.gun.personalcolor.feature.coordi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRefreshTest {
    private val fallback = WeatherState(loading = false, weather = null, hasPermission = true)

    @Test fun refreshesWhenPermissionChanges() =
        assertTrue(shouldRefreshOnResume(true, WeatherState(loading = false, hasPermission = false), 0))

    @Test fun retriesFallbackOnlyAfterDebounce() {
        assertFalse(shouldRefreshOnResume(true, fallback, 5_000))
        assertTrue(shouldRefreshOnResume(true, fallback, 60_000))
    }

    @Test fun leavesRunningFetchAlone() =
        assertFalse(shouldRefreshOnResume(true, WeatherState(loading = true, hasPermission = true), 60_000))

    @Test fun noRetryWithoutPermission() =
        assertFalse(shouldRefreshOnResume(false, WeatherState(loading = false, hasPermission = false), 60_000))
}
