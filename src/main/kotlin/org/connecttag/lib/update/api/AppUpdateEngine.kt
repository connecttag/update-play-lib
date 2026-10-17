package org.connecttag.lib.update.api

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import org.connecttag.lib.update.model.AppUpdateStatus
import org.connecttag.lib.update.model.AppUpdateType
import org.connecttag.lib.update.model.UpdateAvailabilityResult

/**
 * Universal interface for checking and executing app updates from Google Play.
 */
interface AppUpdateEngine {
    /** Real-time observable status of the update lifecycle. */
    val updateStatus: StateFlow<AppUpdateStatus>

    /**
     * Checks Google Play for the existence of an update.
     */
    suspend fun checkForUpdate(): UpdateAvailabilityResult

    /**
     * Initiates the update flow with Google Play.
     *
     * @param activity The host activity used by Google Play to display UI.
     * @param updateType [AppUpdateType.FLEXIBLE] for background download, or [AppUpdateType.IMMEDIATE].
     * @param requestCode Optional request code when using legacy onActivityResult.
     * @return True if Google Play successfully accepted and launched the flow.
     */
    fun startUpdateFlow(
        activity: Activity,
        updateType: AppUpdateType,
        requestCode: Int = 1001
    ): Boolean

    /**
     * Completes a flexible update that has finished downloading by prompting the user
     * and restarting the app.
     */
    fun completeUpdate()

    /**
     * Releases listeners and resources.
     */
    fun destroy()
}
