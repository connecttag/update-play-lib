package org.connecttag.lib.update.play

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType as PlayUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.connecttag.lib.update.api.AppUpdateEngine
import org.connecttag.lib.update.api.AppUpdateLogger
import org.connecttag.lib.update.model.AppUpdateStatus
import org.connecttag.lib.update.model.AppUpdateType
import org.connecttag.lib.update.model.UpdateAvailabilityResult
import kotlin.coroutines.resume

/**
 * Production-ready implementation of [AppUpdateEngine] using Google Play In-App Updates.
 */
class PlayAppUpdateEngine(
    context: Context,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext),
    private val logger: AppUpdateLogger = AppUpdateLogger.DEFAULT
) : AppUpdateEngine, InstallStateUpdatedListener {

    private val _updateStatus = MutableStateFlow<AppUpdateStatus>(AppUpdateStatus.Idle)
    override val updateStatus: StateFlow<AppUpdateStatus> = _updateStatus.asStateFlow()

    private var cachedAppUpdateInfo: AppUpdateInfo? = null

    companion object {
        private const val TAG = "PlayAppUpdateEngine"
    }

    init {
        appUpdateManager.registerListener(this)
    }

    override suspend fun checkForUpdate(): UpdateAvailabilityResult {
        _updateStatus.value = AppUpdateStatus.Checking
        return suspendCancellableCoroutine { continuation ->
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    cachedAppUpdateInfo = info
                    val result = handleAppUpdateInfo(info)
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    logger.w(TAG, "Failed to check for updates", exception)
                    val errorResult = UpdateAvailabilityResult.Error(
                        errorCode = -1,
                        message = exception.message
                    )
                    _updateStatus.value = AppUpdateStatus.Failed(-1, exception.message)
                    if (continuation.isActive) continuation.resume(errorResult)
                }
        }
    }

    private fun handleAppUpdateInfo(info: AppUpdateInfo): UpdateAvailabilityResult {
        val availability = info.updateAvailability()

        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _updateStatus.value = AppUpdateStatus.Downloaded
            return UpdateAvailabilityResult.Available(
                availableVersionCode = info.availableVersionCode(),
                isFlexibleAllowed = info.isFlexibleUpdateAllowed,
                isImmediateAllowed = info.isImmediateUpdateAllowed,
                clientPriority = info.updatePriority(),
                stalenessDays = info.clientVersionStalenessDays(),
                rawUpdateInfo = info
            )
        }

        return when (availability) {
            UpdateAvailability.UPDATE_AVAILABLE -> {
                val status = AppUpdateStatus.Available(
                    availableVersionCode = info.availableVersionCode(),
                    isFlexibleAllowed = info.isFlexibleUpdateAllowed,
                    isImmediateAllowed = info.isImmediateUpdateAllowed,
                    clientPriority = info.updatePriority(),
                    stalenessDays = info.clientVersionStalenessDays()
                )
                _updateStatus.value = status
                UpdateAvailabilityResult.Available(
                    availableVersionCode = info.availableVersionCode(),
                    isFlexibleAllowed = info.isFlexibleUpdateAllowed,
                    isImmediateAllowed = info.isImmediateUpdateAllowed,
                    clientPriority = info.updatePriority(),
                    stalenessDays = info.clientVersionStalenessDays(),
                    rawUpdateInfo = info
                )
            }

            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                _updateStatus.value = AppUpdateStatus.InProgress(AppUpdateType.IMMEDIATE)
                UpdateAvailabilityResult.InProgress(AppUpdateType.IMMEDIATE, info)
            }

            else -> {
                _updateStatus.value = AppUpdateStatus.NotAvailable
                UpdateAvailabilityResult.NotAvailable
            }
        }
    }

    override fun startUpdateFlow(
        activity: Activity,
        updateType: AppUpdateType,
        requestCode: Int
    ): Boolean {
        val info = cachedAppUpdateInfo
        if (info == null) {
            logger.w(TAG, "Cannot start update flow: AppUpdateInfo is null. Did you call checkForUpdate() first?")
            return false
        }

        val playType = when (updateType) {
            AppUpdateType.FLEXIBLE -> PlayUpdateType.FLEXIBLE
            AppUpdateType.IMMEDIATE -> PlayUpdateType.IMMEDIATE
        }

        val isAllowed = when (updateType) {
            AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
            AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
        }

        if (!isAllowed) {
            logger.w(TAG, "Requested update type $updateType is not allowed by Google Play for this release.")
            return false
        }

        return try {
            val options = AppUpdateOptions.newBuilder(playType).build()
            _updateStatus.value = AppUpdateStatus.InProgress(updateType)
            appUpdateManager.startUpdateFlowForResult(info, activity, options, requestCode)
        } catch (e: Exception) {
            logger.e(TAG, "Error launching startUpdateFlowForResult", e)
            _updateStatus.value = AppUpdateStatus.Failed(-1, e.message)
            false
        }
    }

    override fun onStateUpdate(state: InstallState) {
        val total = state.totalBytesToDownload()
        val downloaded = state.bytesDownloaded()
        val percent = if (total > 0) (downloaded.toFloat() / total.toFloat()) * 100f else 0f

        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _updateStatus.value = AppUpdateStatus.Downloading(
                    bytesDownloaded = downloaded,
                    totalBytesToDownload = total,
                    progressPercent = percent
                )
            }

            InstallStatus.DOWNLOADED -> {
                logger.d(TAG, "Update downloaded and ready to complete.")
                _updateStatus.value = AppUpdateStatus.Downloaded
            }

            InstallStatus.INSTALLING -> {
                _updateStatus.value = AppUpdateStatus.Installing
            }

            InstallStatus.INSTALLED -> {
                _updateStatus.value = AppUpdateStatus.Installed
            }

            InstallStatus.CANCELED -> {
                _updateStatus.value = AppUpdateStatus.Canceled
            }

            InstallStatus.FAILED -> {
                _updateStatus.value = AppUpdateStatus.Failed(
                    errorCode = state.installErrorCode(),
                    errorMessage = "Installation failed with error code ${state.installErrorCode()}"
                )
            }

            else -> Unit
        }
    }

    override fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            logger.e(TAG, "Error calling completeUpdate()", e)
        }
    }

    override fun destroy() {
        try {
            appUpdateManager.unregisterListener(this)
        } catch (e: Exception) {
            logger.w(TAG, "Error unregistering install state listener", e)
        }
    }
}
