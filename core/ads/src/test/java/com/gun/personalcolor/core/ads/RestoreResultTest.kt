package com.gun.personalcolor.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreResultTest {
    @Test fun mapsPlayAnswer() {
        assertEquals(RestoreResult.FAILED, restoreResult(ok = false, owned = false))
        assertEquals(RestoreResult.RESTORED, restoreResult(ok = true, owned = true))
        assertEquals(RestoreResult.NOTHING, restoreResult(ok = true, owned = false))
    }

    @Test fun deliversOnlyFirstResult() {
        val got = mutableListOf<RestoreResult>()
        val once = OnceResult { got += it }
        once(RestoreResult.FAILED); once(RestoreResult.RESTORED)
        assertEquals(listOf(RestoreResult.FAILED), got)
    }
}
