package com.aljaz.weighttracker.data.backup

import java.time.Instant

/**
 * Details needed to reach the user's Nextcloud.
 *
 * @property serverUrl Normalised https URL, e.g. "https://cloud.example.com".
 * @property username Nextcloud user ID (as shown in the WebDAV address).
 * @property appPassword Nextcloud app password (never the main password).
 */
data class NextcloudConfig(
    val serverUrl: String,
    val username: String,
    val appPassword: String,
) {
    /** Never print the password – e.g. if this object ends up in a log. */
    override fun toString() =
        "NextcloudConfig(serverUrl=$serverUrl, username=$username, appPassword=***)"
}

/**
 * Outcome of the latest backups, shown in Settings.
 *
 * @property lastSuccessAt When the last successful backup finished, or null if never.
 * @property lastError Why the most recent attempt failed, or null if it succeeded.
 */
data class BackupStatus(
    val lastSuccessAt: Instant? = null,
    val lastError: BackupError? = null,
)

/** Reasons a Nextcloud operation can fail. Each has a user-facing message. */
enum class BackupError {
    /** No connection details saved. */
    NOT_CONFIGURED,

    /** The server address is not a valid https URL. */
    INVALID_URL,

    /** Server answered 401: wrong username or app password. */
    UNAUTHORIZED,

    /** Server answered 404: wrong address or username. */
    NOT_FOUND,

    /** No internet, unknown host, timeout or TLS problem. */
    NETWORK,

    /** Any other unexpected server response. */
    SERVER,
}

/** Result of a Nextcloud request. */
sealed interface NextcloudResult {
    /** The operation succeeded. */
    data object Success : NextcloudResult

    /** The operation failed. @property error Why. */
    data class Failure(val error: BackupError) : NextcloudResult
}