package org.connecttag.lib.update.model

/**
 * Type of update flow to initiate with Google Play.
 */
enum class AppUpdateType {
    /**
     * Background download while the user continues using the app.
     * When completed, the user can be prompted to install and restart.
     */
    FLEXIBLE,

    /**
     * Full-screen mandatory update flow managed by Google Play.
     * The app blocks until the update is completed or canceled.
     */
    IMMEDIATE
}
