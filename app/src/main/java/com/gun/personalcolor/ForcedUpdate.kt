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
internal enum class FlowOutcome { UPDATING, CLOSE_APP, FAILED }

/**
 * Owner's policy: any newer version on Play is installed right away (IMMEDIATE), whatever its priority or staleness.
 * [failures] = broken flows in this process; capped so a failing Play flow can't loop (the user then just continues).
 */
internal fun decideUpdate(availability: Int, immediateAllowed: Boolean, installStatus: Int, failures: Int): UpdateAction = when {
    failures >= ForcedUpdate.MAX_FAILURES -> UpdateAction.NONE
    availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> UpdateAction.RESUME
    // INSTALLING: Play is already swapping the APK and restarts the app itself.
    availability == UpdateAvailability.UPDATE_AVAILABLE && immediateAllowed && installStatus != InstallStatus.INSTALLING -> UpdateAction.START
    else -> UpdateAction.NONE
}

/**
 * The update is mandatory: backing out of it closes the app (the next launch asks again). Anything else that isn't
 * OK (RESULT_IN_APP_UPDATE_FAILED, unknown codes) is Play's fault, so the user continues; a broken flow never locks anyone out.
 */
internal fun flowOutcome(resultCode: Int): FlowOutcome = when (resultCode) {
    Activity.RESULT_OK -> FlowOutcome.UPDATING
    Activity.RESULT_CANCELED -> FlowOutcome.CLOSE_APP
    else -> FlowOutcome.FAILED
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
        when (flowOutcome(r.resultCode)) {
            FlowOutcome.CLOSE_APP -> activity.finishAffinity()
            FlowOutcome.FAILED -> failures++
            FlowOutcome.UPDATING -> Unit
        }
    }

    fun check() {
        if (flowOpen || job?.isActive == true) return
        val m = manager ?: return
        job = activity.lifecycleScope.launch {
            try {
                val info = m.requestAppUpdateInfo()
                val action = decideUpdate(info.updateAvailability(), info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE), info.installStatus(), failures)
                if (action == UpdateAction.NONE) return@launch
                flowOpen = m.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                if (!flowOpen) failures++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) { // not from Play, no Play services, offline, launcher already unregistered…
                failures++ // the user just continues; capped so retries in onResume stop
                flowOpen = false
                Log.w(TAG, "update check failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "ForcedUpdate"
        /** Broken flows tolerated per process before giving up until the next process start. */
        const val MAX_FAILURES = 2
        private var failures = 0
    }
}
