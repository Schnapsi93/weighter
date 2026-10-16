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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.data.backup.BackupError
import com.aljaz.weighttracker.data.backup.BackupStatus
import com.aljaz.weighttracker.ui.AppViewModelProvider
import com.aljaz.weighttracker.util.WeightFormatter

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

/** Maximum number of skipped line numbers listed in the import preview. */
private const val MAX_LISTED_SKIPPED_LINES = 10

/** User actions of the Nextcloud card, grouped to keep parameter lists short. */
data class NextcloudActions(
    val onServerUrlChange: (String) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onConnect: () -> Unit,
    val onBackupNow: () -> Unit,
    val onDisconnect: () -> Unit,
)

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

    val nextcloudActions = remember(viewModel) {
        NextcloudActions(
            onServerUrlChange = viewModel::onServerUrlChange,
            onUsernameChange = viewModel::onUsernameChange,
            onPasswordChange = viewModel::onPasswordChange,
            onConnect = viewModel::onConnect,
            onBackupNow = viewModel::onBackupNow,
            onDisconnect = viewModel::onDisconnect,
        )
    }

    SettingsContent(
        uiState = uiState,
        onExportClick = { exportLauncher.launch(viewModel.exportFileName()) },
        onImportClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
        nextcloudActions = nextcloudActions,
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
 * @param nextcloudActions Callbacks for the Nextcloud card.
 * @param onMessageShown Called after a snackbar message was shown.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    nextcloudActions: NextcloudActions,
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
        // The app's root Scaffold already handles system bar insets.
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

            NextcloudCard(
                form = uiState.nextcloudForm,
                isConnected = uiState.isConnected,
                status = uiState.backupStatus,
                isWorking = uiState.isWorking,
                actions = nextcloudActions,
            )
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
 * Card for the Nextcloud connection: login fields, Connect, and,
 * once connected, the backup status with Back up now / Disconnect.
 *
 * @param form Text currently in the fields.
 * @param isConnected True if a connection is saved.
 * @param status Result of the latest backups.
 * @param isWorking Disables the buttons while an operation runs.
 * @param actions Callbacks for user actions.
 */
@Composable
private fun NextcloudCard(
    form: NextcloudForm,
    isConnected: Boolean,
    status: BackupStatus,
    isWorking: Boolean,
    actions: NextcloudActions,
) {
    // Pure UI state, so it lives here rather than in the ViewModel.
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.nextcloud_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.nextcloud_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = form.serverUrl,
                onValueChange = actions.onServerUrlChange,
                label = { Text(stringResource(R.string.nextcloud_server_url)) },
                placeholder = { Text(stringResource(R.string.nextcloud_server_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.username,
                onValueChange = actions.onUsernameChange,
                label = { Text(stringResource(R.string.nextcloud_username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.password,
                onValueChange = actions.onPasswordChange,
                label = { Text(stringResource(R.string.nextcloud_app_password)) },
                supportingText = { Text(stringResource(R.string.nextcloud_app_password_help)) },
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = stringResource(
                                if (showPassword) {
                                    R.string.nextcloud_hide_password
                                } else {
                                    R.string.nextcloud_show_password
                                }
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = actions.onConnect,
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.nextcloud_connect)) }

            if (isConnected) {
                BackupStatusText(status)

                Button(
                    onClick = actions.onBackupNow,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.nextcloud_backup_now)) }

                OutlinedButton(
                    onClick = actions.onDisconnect,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.nextcloud_disconnect)) }
            }
        }
    }
}

/**
 * "Last backup: …" line, plus the latest error if the last attempt failed.
 *
 * @param status Result of the latest backups.
 */
@Composable
private fun BackupStatusText(status: BackupStatus) {
    val lastSuccess = status.lastSuccessAt
    Text(
        text = if (lastSuccess != null) {
            stringResource(
                R.string.nextcloud_last_backup,
                WeightFormatter.formatDateTime(lastSuccess),
            )
        } else {
            stringResource(R.string.nextcloud_last_backup_never)
        },
        style = MaterialTheme.typography.bodyMedium,
    )
    status.lastError?.let { error ->
        Text(
            text = stringResource(R.string.nextcloud_last_error, backupErrorText(error)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
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
                    val listed = preview.skippedLines
                        .take(MAX_LISTED_SKIPPED_LINES)
                        .joinToString()
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
    is SettingsMessage.Exported ->
        stringResource(R.string.settings_exported, message.count)

    SettingsMessage.ExportFailed ->
        stringResource(R.string.settings_export_failed)

    is SettingsMessage.Imported ->
        stringResource(R.string.settings_imported, message.count)

    SettingsMessage.ImportFailed ->
        stringResource(R.string.settings_import_failed)

    is SettingsMessage.NothingToImport ->
        stringResource(R.string.settings_import_nothing, message.skippedCount)

    SettingsMessage.Connected ->
        stringResource(R.string.nextcloud_connected)

    is SettingsMessage.ConnectionFailed ->
        stringResource(R.string.nextcloud_connection_failed, backupErrorText(message.error))

    SettingsMessage.IncompleteDetails ->
        stringResource(R.string.nextcloud_incomplete)

    SettingsMessage.BackupDone ->
        stringResource(R.string.nextcloud_backup_done)

    is SettingsMessage.BackupFailed ->
        stringResource(R.string.nextcloud_backup_failed, backupErrorText(message.error))

    SettingsMessage.Disconnected ->
        stringResource(R.string.nextcloud_disconnected)
}

/** Returns the user-facing description of a backup [error]. */
@Composable
private fun backupErrorText(error: BackupError): String = stringResource(
    when (error) {
        BackupError.NOT_CONFIGURED -> R.string.backup_error_not_configured
        BackupError.INVALID_URL -> R.string.backup_error_invalid_url
        BackupError.UNAUTHORIZED -> R.string.backup_error_unauthorized
        BackupError.NOT_FOUND -> R.string.backup_error_not_found
        BackupError.NETWORK -> R.string.backup_error_network
        BackupError.SERVER -> R.string.backup_error_server
    }
)