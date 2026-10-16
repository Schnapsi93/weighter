package com.aljaz.weighttracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.csv.CsvTransfer
import com.aljaz.weighttracker.data.csv.WeightCsv
import com.aljaz.weighttracker.data.repository.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Logic for the Settings screen: CSV export, and CSV import with a
 * preview step so nothing is saved before the user confirms.
 *
 * @param csvTransfer Reads and writes CSV files.
 * @param repository Used to compare imported data with existing entries.
 * @param today Returns the current date (used in the export file name).
 */
class SettingsViewModel(
    private val csvTransfer: CsvTransfer,
    private val repository: WeightRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    /** Current screen state. */
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Suggested name for the export file, e.g. "weight-tracker-2026-10-16.csv". */
    fun exportFileName(): String = "weight-tracker-${today()}.csv"

    // ---------- Export ----------

    /** Writes all entries to the file the user just created at [uri]. */
    fun onExportTo(uri: Uri) = runWork(failure = SettingsMessage.ExportFailed) {
        SettingsMessage.Exported(csvTransfer.export(uri))
    }

    // ---------- Import ----------

    /** Reads the picked file and shows a preview (nothing is saved yet). */
    fun onImportFileSelected(uri: Uri) = runWork(failure = SettingsMessage.ImportFailed) {
        val result = csvTransfer.read(uri)
        if (result.entries.isEmpty()) {
            SettingsMessage.NothingToImport(result.skippedLines.size)
        } else {
            val preview = buildPreview(result)
            _uiState.update { it.copy(importPreview = preview) }
            null  // no snackbar – the preview dialog is shown instead
        }
    }

    /** Saves the previewed entries. */
    fun onImportConfirm() {
        val preview = _uiState.value.importPreview ?: return
        _uiState.update { it.copy(importPreview = null) }
        runWork(failure = SettingsMessage.ImportFailed) {
            csvTransfer.import(preview.entries)
            SettingsMessage.Imported(preview.entries.size)
        }
    }

    /** Closes the preview without importing. */
    fun onImportCancel() {
        _uiState.update { it.copy(importPreview = null) }
    }

    /** Called after the snackbar for [SettingsUiState.message] was shown. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    // ---------- Helpers ----------

    /** Compares the parsed file with existing entries to count new/replaced/unchanged days. */
    private suspend fun buildPreview(result: WeightCsv.ParseResult): ImportPreview {
        val existing = repository.getAllEntries().associateBy { it.date }
        var new = 0
        var replaced = 0
        var unchanged = 0
        result.entries.forEach { entry ->
            val current = existing[entry.date]
            when {
                current == null -> new++
                current.weightKg == entry.weightKg -> unchanged++
                else -> replaced++
            }
        }
        return ImportPreview(result.entries, new, replaced, unchanged, result.skippedLines)
    }

    /**
     * Runs [block] in the background while showing the "working" state.
     * The message [block] returns (or [failure] if it throws) is shown afterwards.
     * Ignored if another operation is already running.
     */
    private fun runWork(
        failure: SettingsMessage,
        block: suspend () -> SettingsMessage?,
    ) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true) }

        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: CancellationException) {
                throw e  // never swallow coroutine cancellation
            } catch (e: Exception) {
                failure
            }
            _uiState.update { it.copy(isWorking = false, message = message ?: it.message) }
        }
    }
}