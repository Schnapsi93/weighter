package com.aljaz.weighttracker.ui.screens.settings

import com.aljaz.weighttracker.model.WeightEntry

/**
 * Everything the Settings screen displays.
 *
 * @property isWorking True while an export/import runs (buttons disabled).
 * @property importPreview Parsed file waiting for confirmation, or null.
 * @property message One-time message to show in a snackbar, or null.
 */
data class SettingsUiState(
    val isWorking: Boolean = false,
    val importPreview: ImportPreview? = null,
    val message: SettingsMessage? = null,
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
}