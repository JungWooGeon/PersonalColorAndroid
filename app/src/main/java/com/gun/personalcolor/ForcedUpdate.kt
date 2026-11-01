package com.gun.personalcolor

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class UpdateAction { NONE, START, RESUME }

/**
 * Owner's policy: any newer version on Play is installed right away (IMMEDIATE), whatever its priority or staleness.
 * [starts] = flows already started in this process; capped so a Play flow that keeps failing or bouncing can't loop.
 */
internal fun decideUpdate(availability: Int, immediateAllowed: Boolean, installStatus: Int, starts: Int): UpdateAction = when {
    starts >= ForcedUpdate.MAX_STARTS -> UpdateAction.NONE
    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> UpdateAction.RESUME
    // INSTALLING: Play is already swapping the APK and restarts the app itself.
    availability == UpdateAvailability.UPDATE_AVAILABLE && immediateAllowed && installStatus != InstallStatus.INSTALLING -> UpdateAction.START
    else -> UpdateAction.NONE
}

/**
 * Forced in-app update. Fail-safe by design: apps not installed from Play (debug, sideloaded), no Play services or
 * offline all end in "nothing happens" (errors are logged, never thrown). Never blocks: the check is async.
 * Create it as an activity field (registers an activity-result launcher, which must happen before STARTED).
 */
internal class ForcedUpdate(private val activity: ComponentActivity) {
    private val manager: AppUpdateManager? by lazy {
        runCatching { AppUpdateManagerFactory.create(activity.applicationContext) }.onFailure { Log.w(TAG, "no update manager", it) }.getOrNull()
    }
    private var job: Job? = null
    private var flowOpen = false

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        flowOpen = false
        Log.i(TAG, "update flow result ${r.resultCode}")
        // Cancelled by the user: ask again, so the update can't be skipped (bounded by MAX_STARTS).
        if (r.resultCode == Activity.RESULT_CANCELED) check()
    }

    fun check() {
        if (flowOpen || job?.isActive == true) return
        val m = manager ?: return
        job = activity.lifecycleScope.launch {
            try {
                val info = m.requestAppUpdateInfo()
                val action = decideUpdate(info.updateAvailability(), info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE), info.installStatus(), starts)
                if (action == UpdateAction.NONE) return@launch
                starts++
                flowOpen = m.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) { // not from Play, no Play services, offline, launcher already unregistered…
                flowOpen = false
                Log.w(TAG, "update check failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "ForcedUpdate"
        /** ponytail: per-process cap on started flows; after that the next cold start asks again. */
        const val MAX_STARTS = 5
        private var starts = 0
    }
}
