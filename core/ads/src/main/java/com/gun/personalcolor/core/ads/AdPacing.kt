package com.gun.personalcolor.core.ads

/** Pure time/unlock bookkeeping behind [AdManager] (no SDK types, so it is unit-testable with a fake clock). */
internal class AdPacing(private val clock: () -> Long) {
    private var lastFullScreenAt = 0L
    private val unlockedUntil = mutableMapOf<String, Long>()

    fun isUnlocked(key: String) = (unlockedUntil[key] ?: 0L) > clock()
    fun unlock(key: String, unlockMs: Long) { unlockedUntil[key] = clock() + unlockMs }
    fun consumeUnlock(key: String): Boolean = unlockedUntil.remove(key) != null
    fun onFullScreenShown() { lastFullScreenAt = clock() }

    /** Every result gets an interstitial, except right after another full-screen ad (no back-to-back ads). */
    fun interstitialDue() = lastFullScreenAt == 0L || clock() - lastFullScreenAt > AdManager.BACK_TO_BACK_GAP_MS
}
