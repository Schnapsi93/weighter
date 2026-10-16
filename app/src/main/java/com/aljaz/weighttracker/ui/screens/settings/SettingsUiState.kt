package com.aljaz.weighttracker.ui.screens.settings

import com.aljaz.weighttracker.data.backup.BackupError
import com.aljaz.weighttracker.data.backup.BackupStatus
import com.aljaz.weighttracker.model.WeightEntry

/**
 * Everything the Settings screen displays.
 *
 * @property isWorking True while an operation runs (buttons disabled).
 * @property importPreview Parsed CSV file waiting for confirmation, or null.
 * @property message One-time message to show in a snackbar, or null.
 * @property nextcloudForm What is typed into the Nextcloud fields.
 * @property isConnected True if a Nextcloud connection is saved.
 * @property backupStatus Result of the latest backups.
 */
data class SettingsUiState(
    val isWorking: Boolean = false,
    val importPreview: ImportPreview? = null,
    val message: SettingsMessage? = null,
    val nextcloudForm: NextcloudForm = NextcloudForm(),
    val isConnected: Boolean = false,
    val backupStatus: BackupStatus = BackupStatus(),
)

/** Text currently typed into the Nextcloud fields (not yet saved). */
data class NextcloudForm(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
)

/**
 * Summary of a CSV file, shown before anything is saved.
 *
 * @property entries Valid entries that will be imported.
 * @property newCount Days that don't exist in the app yet.
 * @property replacedCount Existing days whose weight will change.
 * @property unchangedCount Existing days with the same weight (no effect).
 * @property skippedLines Line numbers that could not be read.
 */
data class ImportPreview(
    val entries: List<WeightEntry>,
    val newCount: Int,
    val replacedCount: Int,
    val unchangedCount: Int,
    val skippedLines: List<Int>,
)

/** One-time results shown to the user as a snackbar. */
sealed interface SettingsMessage {
    data class Exported(val count: Int) : SettingsMessage
    data object ExportFailed : SettingsMessage
    data class Imported(val count: Int) : SettingsMessage
    data object ImportFailed : SettingsMessage
    data class NothingToImport(val skippedCount: Int) : SettingsMessage
    data object Connected : SettingsMessage
    data class ConnectionFailed(val error: BackupError) : SettingsMessage
    data object IncompleteDetails : SettingsMessage
    data object BackupDone : SettingsMessage
    data class BackupFailed(val error: BackupError) : SettingsMessage
    data object Disconnected : SettingsMessage
}