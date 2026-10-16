package com.aljaz.weighttracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.backup.BackupError
import com.aljaz.weighttracker.data.backup.BackupManager
import com.aljaz.weighttracker.data.backup.BackupScheduler
import com.aljaz.weighttracker.data.backup.BackupSettingsStore
import com.aljaz.weighttracker.data.backup.NextcloudClient
import com.aljaz.weighttracker.data.backup.NextcloudConfig
import com.aljaz.weighttracker.data.backup.NextcloudResult
import com.aljaz.weighttracker.data.csv.CsvTransfer
import com.aljaz.weighttracker.data.csv.WeightCsv
import com.aljaz.weighttracker.data.repository.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Logic for the Settings screen: CSV export/import (with preview),
 * the Nextcloud connection, manual backups and the automatic monthly backup.
 *
 * @param csvTransfer Reads and writes CSV files.
 * @param repository Used to compare imported data with existing entries.
 * @param backupManager Tests connections and creates backups.
 * @param backupSettings Saved connection, backup status and auto-backup switch.
 * @param backupScheduler Schedules/cancels the automatic monthly backup.
 * @param today Returns the current date (used in the export file name).
 */
class SettingsViewModel(
    private val csvTransfer: CsvTransfer,
    private val repository: WeightRepository,
    private val backupManager: BackupManager,
    private val backupSettings: BackupSettingsStore,
    private val backupScheduler: BackupScheduler,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** State owned by this screen (form text, dialogs, messages). */
    private val local = MutableStateFlow(SettingsUiState())

    /** Complete screen state: local state plus everything saved in settings. */
    val uiState: StateFlow<SettingsUiState> =
        combine(
            local,
            backupSettings.config,
            backupSettings.status,
            backupSettings.autoBackupEnabled,
        ) { state, config, status, autoBackup ->
            state.copy(
                isConnected = config != null,
                backupStatus = status,
                autoBackupEnabled = autoBackup,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    init {
        // Pre-fill the form with the saved connection, if there is one.
        viewModelScope.launch {
            backupSettings.config.first()?.let { config ->
                local.update {
                    it.copy(nextcloudForm = NextcloudForm(config.serverUrl, config.username, config.appPassword))
                }
            }
        }
    }

    // ---------- CSV export / import ----------

    /** Suggested name for the export file, e.g. "weighter-2026-10-16.csv". */
    fun exportFileName(): String = "weighter-${today()}.csv"

    /** Writes all entries to the file the user just created at [uri]. */
    fun onExportTo(uri: Uri) = runWork(failure = SettingsMessage.ExportFailed) {
        SettingsMessage.Exported(csvTransfer.export(uri))
    }

    /** Reads the picked file and shows a preview (nothing is saved yet). */
    fun onImportFileSelected(uri: Uri) = runWork(failure = SettingsMessage.ImportFailed) {
        val result = csvTransfer.read(uri)
        if (result.entries.isEmpty()) {
            SettingsMessage.NothingToImport(result.skippedLines.size)
        } else {
            local.update { it.copy(importPreview = buildPreview(result)) }
            null  // no snackbar – the preview dialog is shown instead
        }
    }

    /** Saves the previewed entries. */
    fun onImportConfirm() {
        val preview = local.value.importPreview ?: return
        local.update { it.copy(importPreview = null) }
        runWork(failure = SettingsMessage.ImportFailed) {
            csvTransfer.import(preview.entries)
            SettingsMessage.Imported(preview.entries.size)
        }
    }

    /** Closes the preview without importing. */
    fun onImportCancel() {
        local.update { it.copy(importPreview = null) }
    }

    // ---------- Nextcloud ----------

    fun onServerUrlChange(value: String) = updateForm { it.copy(serverUrl = value) }
    fun onUsernameChange(value: String) = updateForm { it.copy(username = value) }
    fun onPasswordChange(value: String) = updateForm { it.copy(password = value) }

    /**
     * Tests the typed details and saves them only if the connection works.
     * On success, the automatic backup is scheduled (if switched on).
     */
    fun onConnect() {
        val form = local.value.nextcloudForm
        if (form.serverUrl.isBlank() || form.username.isBlank() || form.password.isBlank()) {
            showMessage(SettingsMessage.IncompleteDetails)
            return
        }
        val url = NextcloudClient.normalizeServerUrl(form.serverUrl)
        if (url == null) {
            showMessage(SettingsMessage.ConnectionFailed(BackupError.INVALID_URL))
            return
        }

        val config = NextcloudConfig(url, form.username.trim(), form.password.trim())
        runWork(failure = SettingsMessage.ConnectionFailed(BackupError.NETWORK)) {
            when (val result = backupManager.testConnection(config)) {
                NextcloudResult.Success -> {
                    backupSettings.saveConfig(config)
                    if (backupSettings.autoBackupEnabled.first()) backupScheduler.schedule()
                    updateForm { it.copy(serverUrl = url) }  // show the cleaned-up address
                    SettingsMessage.Connected
                }
                is NextcloudResult.Failure -> SettingsMessage.ConnectionFailed(result.error)
            }
        }
    }

    /** Uploads a backup right now. */
    fun onBackupNow() = runWork(failure = SettingsMessage.BackupFailed(BackupError.NETWORK)) {
        when (val result = backupManager.backupNow()) {
            NextcloudResult.Success -> SettingsMessage.BackupDone
            is NextcloudResult.Failure -> SettingsMessage.BackupFailed(result.error)
        }
    }

    /** Switches the automatic monthly backup on or off. */
    fun onAutoBackupChange(enabled: Boolean) {
        viewModelScope.launch {
            backupSettings.setAutoBackupEnabled(enabled)
            val isConnected = backupSettings.config.first() != null
            if (enabled && isConnected) backupScheduler.schedule() else backupScheduler.cancel()
        }
    }

    /** Stops automatic backups, removes the saved connection and clears the form. */
    fun onDisconnect() {
        viewModelScope.launch {
            backupScheduler.cancel()
            backupSettings.clear()
            local.update {
                it.copy(nextcloudForm = NextcloudForm(), message = SettingsMessage.Disconnected)
            }
        }
    }

    // ---------- Messages ----------

    /** Called after the snackbar for [SettingsUiState.message] was shown. */
    fun onMessageShown() {
        local.update { it.copy(message = null) }
    }

    // ---------- Helpers ----------

    private fun updateForm(change: (NextcloudForm) -> NextcloudForm) {
        local.update { it.copy(nextcloudForm = change(it.nextcloudForm)) }
    }

    private fun showMessage(message: SettingsMessage) {
        local.update { it.copy(message = message) }
    }

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
        if (local.value.isWorking) return
        local.update { it.copy(isWorking = true) }

        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure
            }
            local.update { it.copy(isWorking = false, message = message ?: it.message) }
        }
    }
}