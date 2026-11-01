package com.gun.personalcolor.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** What to unlock with a rewarded ad. [title]/[message] are shown in the opt-in dialog. */
class RewardRequest(
    val key: String,
    val title: String,
    val message: String,
    val unlockMs: Long = AdManager.SESSION_UNLOCK_MS,
    val action: () -> Unit,
)

/**
 * Returns a function that runs a request's action — directly if unlocked/ad-free/no ad ready, otherwise after the
 * user opts in to a rewarded ad via a dialog. Call the returned function from click handlers.
 */
@Composable
fun rememberRewardGate(): (RewardRequest) -> Unit {
    val ads = LocalAdManager.current
    val activity = LocalActivity.current
    var pending by remember { mutableStateOf<RewardRequest?>(null) }
    pending?.let { r ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(r.title) },
            text = { Text(r.message) },
            confirmButton = {
                TextButton(onClick = { pending = null; ads?.showRewarded(activity, r.key, r.unlockMs, r.action) ?: r.action() }) { Text("광고 보기") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("취소") } },
        )
    }
    return remember(ads) { { r -> if (ads?.shouldOfferReward(r.key) == true) pending = r else r.action() } }
}
