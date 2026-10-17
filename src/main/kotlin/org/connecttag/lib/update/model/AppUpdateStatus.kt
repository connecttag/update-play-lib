package org.connecttag.lib.update.model

/**
 * Real-time lifecycle status of the app update process.
 */
sealed interface AppUpdateStatus {
    /** No update check performed yet or status reset. */
    data object Idle : AppUpdateStatus

    /** Actively checking Google Play for updates. */
    data object Checking : AppUpdateStatus

    /**
     * An update is available on the store.
     */
    data class Available(
        val availableVersionCode: Int,
        val isFlexibleAllowed: Boolean,
        val isImmediateAllowed: Boolean,
        val clientPriority: Int = 0,
        val stalenessDays: Int? = null
    ) : AppUpdateStatus

    /** The app is already up to date. */
    data object NotAvailable : AppUpdateStatus

    /** An update flow is in progress. */
    data class InProgress(val type: AppUpdateType) : AppUpdateStatus

    /** Download is actively progressing (for flexible updates). */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long,
        val progressPercent: Float
    ) : AppUpdateStatus

    /** The update file has finished downloading and is ready to install. */
    data object Downloaded : AppUpdateStatus

    /** The update is actively being installed. */
    data object Installing : AppUpdateStatus

    /** The update has been completely installed. */
    data object Installed : AppUpdateStatus

    /** The user canceled the update flow. */
    data object Canceled : AppUpdateStatus

    /** The update process failed. */
    data class Failed(val errorCode: Int, val errorMessage: String?) : AppUpdateStatus
}
