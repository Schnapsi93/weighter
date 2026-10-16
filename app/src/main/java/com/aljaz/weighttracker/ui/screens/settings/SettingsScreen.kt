package com.aljaz.weighttracker.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.ui.AppViewModelProvider

/** MIME type of exported files. */
private const val CSV_MIME_TYPE = "text/csv"

/**
 * File types offered when importing. File managers label CSV files
 * inconsistently, so several common types are allowed.
 */
private val IMPORT_MIME_TYPES = arrayOf(
    "text/*",
    "application/csv",
    "application/vnd.ms-excel",
    "application/octet-stream",
)

/** Maximum number of skipped line numbers listed in the preview. */
private const val MAX_LISTED_SKIPPED_LINES = 10

/**
 * Settings screen (stateful): owns the system file pickers and connects
 * [SettingsViewModel] to [SettingsContent].
 *
 * @param modifier Optional modifier from the caller.
 * @param viewModel Created automatically; can be replaced in tests.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // System "Save as" dialog. Returns the chosen file's Uri, or null if cancelled.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { uri -> uri?.let(viewModel::onExportTo) }

    // System "Open file" dialog.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::onImportFileSelected) }

    SettingsContent(
        uiState = uiState,
        onExportClick = { exportLauncher.launch(viewModel.exportFileName()) },
        onImportClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )

    uiState.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            onConfirm = viewModel::onImportConfirm,
            onDismiss = viewModel::onImportCancel,
        )
    }
}

/**
 * Settings screen layout (stateless).
 *
 * @param uiState What to display.
 * @param onExportClick Called when Export is tapped.
 * @param onImportClick Called when Import is tapped.
 * @param onMessageShown Called after a snackbar message was shown.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Read the text here: stringResource() can't be used inside LaunchedEffect.
    val messageText = uiState.message?.let { messageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DataCard(
                isWorking = uiState.isWorking,
                onExportClick = onExportClick,
                onImportClick = onImportClick,
            )
            // Milestone 7: Nextcloud backup card goes here.
        }
    }
}

/**
 * Card with the CSV export and import buttons.
 *
 * @param isWorking Disables the buttons and shows progress while true.
 * @param onExportClick Called when Export is tapped.
 * @param onImportClick Called when Import is tapped.
 */
@Composable
private fun DataCard(
    isWorking: Boolean,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_data_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_data_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onExportClick,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_export)) }

            OutlinedButton(
                onClick = onImportClick,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_import)) }

            if (isWorking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Confirmation dialog summarising what an import will change.
 *
 * @param preview Summary of the parsed file.
 * @param onConfirm Called when the user taps Import.
 * @param onDismiss Called when the user cancels.
 */
@Composable
private fun ImportPreviewDialog(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.import_dialog_new, preview.newCount))
                Text(stringResource(R.string.import_dialog_replaced, preview.replacedCount))
                Text(stringResource(R.string.import_dialog_unchanged, preview.unchangedCount))

                if (preview.skippedLines.isNotEmpty()) {
                    val listed = preview.skippedLines.take(MAX_LISTED_SKIPPED_LINES).joinToString()
                    val more = if (preview.skippedLines.size > MAX_LISTED_SKIPPED_LINES) " …" else ""
                    Text(
                        text = stringResource(R.string.import_dialog_skipped, listed + more),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Returns the user-facing text for a one-time [message]. */
@Composable
private fun messageText(message: SettingsMessage): String = when (message) {
    is SettingsMessage.Exported -> stringResource(R.string.settings_exported, message.count)
    SettingsMessage.ExportFailed -> stringResource(R.string.settings_export_failed)
    is SettingsMessage.Imported -> stringResource(R.string.settings_imported, message.count)
    SettingsMessage.ImportFailed -> stringResource(R.string.settings_import_failed)
    is SettingsMessage.NothingToImport ->
        stringResource(R.string.settings_import_nothing, message.skippedCount)
}