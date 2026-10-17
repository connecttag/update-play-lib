package org.connecttag.lib.update.fake

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.connecttag.lib.update.api.AppUpdateEngine
import org.connecttag.lib.update.model.AppUpdateStatus
import org.connecttag.lib.update.model.AppUpdateType
import org.connecttag.lib.update.model.UpdateAvailabilityResult

/**
 * Mock implementation of [AppUpdateEngine] for unit tests, UI previews, and developer environments.
 */
class FakeAppUpdateEngine(
    initialStatus: AppUpdateStatus = AppUpdateStatus.Idle
) : AppUpdateEngine {

    private val _updateStatus = MutableStateFlow(initialStatus)
    override val updateStatus: StateFlow<AppUpdateStatus> = _updateStatus.asStateFlow()

    var isUpdateAvailable: Boolean = true
    var isFlexibleAllowed: Boolean = true
    var isImmediateAllowed: Boolean = true
    var availableVersionCode: Int = 200
    var nextLaunchSuccess: Boolean = true

    override suspend fun checkForUpdate(): UpdateAvailabilityResult {
        _updateStatus.value = AppUpdateStatus.Checking
        return if (isUpdateAvailable) {
            val status = AppUpdateStatus.Available(
                availableVersionCode = availableVersionCode,
                isFlexibleAllowed = isFlexibleAllowed,
                isImmediateAllowed = isImmediateAllowed
            )
            _updateStatus.value = status
            UpdateAvailabilityResult.Available(
                availableVersionCode = availableVersionCode,
                isFlexibleAllowed = isFlexibleAllowed,
                isImmediateAllowed = isImmediateAllowed
            )
        } else {
            _updateStatus.value = AppUpdateStatus.NotAvailable
            UpdateAvailabilityResult.NotAvailable
        }
    }

    override fun startUpdateFlow(
        activity: Activity,
        updateType: AppUpdateType,
        requestCode: Int
    ): Boolean {
        if (!nextLaunchSuccess) {
            _updateStatus.value = AppUpdateStatus.Failed(-1, "Launch rejected")
            return false
        }

        _updateStatus.value = AppUpdateStatus.InProgress(updateType)
        if (updateType == AppUpdateType.FLEXIBLE) {
            simulateDownloading(50f)
        }
        return true
    }

    fun simulateDownloading(percent: Float) {
        val total = 10_000_000L
        val downloaded = (total * (percent / 100f)).toLong()
        _updateStatus.value = AppUpdateStatus.Downloading(
            bytesDownloaded = downloaded,
            totalBytesToDownload = total,
            progressPercent = percent
        )
    }

    fun simulateDownloaded() {
        _updateStatus.value = AppUpdateStatus.Downloaded
    }

    fun simulateInstalling() {
        _updateStatus.value = AppUpdateStatus.Installing
    }

    fun simulateInstalled() {
        _updateStatus.value = AppUpdateStatus.Installed
    }

    fun simulateCanceled() {
        _updateStatus.value = AppUpdateStatus.Canceled
    }

    fun simulateFailed(errorCode: Int = -1, errorMessage: String? = "Mock Failure") {
        _updateStatus.value = AppUpdateStatus.Failed(errorCode, errorMessage)
    }

    override fun completeUpdate() {
        _updateStatus.value = AppUpdateStatus.Installed
    }

    override fun destroy() {
        _updateStatus.value = AppUpdateStatus.Idle
    }
}
