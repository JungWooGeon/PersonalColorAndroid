package com.gun.personalcolor

import org.junit.Assert.assertEquals
import org.junit.Test

/** Plain ints (Play's UpdateAvailability / InstallStatus values), so no Play classes are needed. */
class ForcedUpdateTest {
    private val unknown = 0; private val notAvailable = 1; private val available = 2; private val inProgress = 3
    private val pending = 1; private val installing = 3; private val idle = 0

    @Test fun `newer version forces an immediate update`() =
        assertEquals(UpdateAction.START, decideUpdate(available, immediateAllowed = true, installStatus = idle, starts = 0))

    @Test fun `in-progress immediate update is resumed`() =
        assertEquals(UpdateAction.RESUME, decideUpdate(inProgress, immediateAllowed = false, installStatus = installing, starts = 0))

    @Test fun `no update, unknown (sideloaded, debug) or immediate not allowed does nothing`() {
        assertEquals(UpdateAction.NONE, decideUpdate(notAvailable, true, idle, 0))
        assertEquals(UpdateAction.NONE, decideUpdate(unknown, true, idle, 0))
        assertEquals(UpdateAction.NONE, decideUpdate(available, false, idle, 0))
    }

    @Test fun `already installing does nothing, pending still starts`() {
        assertEquals(UpdateAction.NONE, decideUpdate(available, true, installing, 0))
        assertEquals(UpdateAction.START, decideUpdate(available, true, pending, 0))
    }

    @Test fun `repeated flows are capped so it can never loop endlessly`() {
        assertEquals(UpdateAction.START, decideUpdate(available, true, idle, ForcedUpdate.MAX_STARTS - 1))
        assertEquals(UpdateAction.NONE, decideUpdate(available, true, idle, ForcedUpdate.MAX_STARTS))
        assertEquals(UpdateAction.NONE, decideUpdate(inProgress, true, idle, ForcedUpdate.MAX_STARTS))
    }
}
