package org.connecttag.lib.update.model

/**
 * Result returned from querying update availability.
 */
sealed interface UpdateAvailabilityResult {
    data class Available(
        val availableVersionCode: Int,
        val isFlexibleAllowed: Boolean,
        val isImmediateAllowed: Boolean,
        val clientPriority: Int = 0,
        val stalenessDays: Int? = null,
        val rawUpdateInfo: Any? = null
    ) : UpdateAvailabilityResult

    data object NotAvailable : UpdateAvailabilityResult

    data class InProgress(
        val type: AppUpdateType,
        val rawUpdateInfo: Any? = null
    ) : UpdateAvailabilityResult

    data class Error(val errorCode: Int, val message: String?) : UpdateAvailabilityResult
}
