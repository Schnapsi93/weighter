package com.aljaz.weighttracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.ui.AppViewModelProvider
import com.aljaz.weighttracker.ui.theme.WeightTrackerTheme
import com.aljaz.weighttracker.util.WeightFormatter
import java.time.LocalDate

/**
 * History screen (stateful): connects [HistoryViewModel] to the UI.
 *
 * @param modifier Optional modifier from the caller.
 * @param viewModel Created automatically; can be replaced in tests.
 */
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryContent(
        uiState = uiState,
        onEntryClick = viewModel::onEntryClick,
        onAddClick = viewModel::onAddClick,
        onUndoDelete = viewModel::onUndoDelete,
        onUndoHandled = viewModel::onUndoHandled,
        modifier = modifier,
    )

    uiState.editor?.let { editor ->
        WeightEditorDialog(
            state = editor,
            onDateChange = viewModel::onEditorDateChange,
            onInputChange = viewModel::onEditorInputChange,
            onSave = viewModel::onEditorSave,
            onDelete = viewModel::onEditorDelete,
            onDismiss = viewModel::onEditorDismiss,
        )
    }
}

/**
 * History screen layout (stateless).
 *
 * @param uiState What to display.
 * @param onEntryClick Called when a row is tapped.
 * @param onAddClick Called when the + button is tapped.
 * @param onUndoDelete Called when Undo is tapped on the snackbar.
 * @param onUndoHandled Called when the snackbar disappears without Undo.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onEntryClick: (WeightEntry) -> Unit,
    onAddClick: () -> Unit,
    onUndoDelete: () -> Unit,
    onUndoHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Strings must be read here: stringResource() can't be called inside LaunchedEffect.
    val undoEntry = uiState.pendingUndo
    val deletedMessage = undoEntry?.let {
        stringResource(R.string.history_deleted, WeightFormatter.formatDate(it.date))
    }
    val undoLabel = stringResource(R.string.history_undo)

    // Shows the Undo snackbar whenever a new entry is deleted.
    LaunchedEffect(undoEntry) {
        if (deletedMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onUndoDelete() else onUndoHandled()
        }
    }

    Scaffold(
        modifier = modifier,
        // The app's root Scaffold already handles system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.history_add))
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit  // brief moment on start – show nothing
            uiState.items.isEmpty() -> EmptyHistory(Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                // Extra bottom space so the + button never covers the last row.
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(
                    items = uiState.items,
                    // Stable keys let Compose animate and update rows efficiently.
                    key = { it.entry.date.toEpochDay() },
                ) { item ->
                    HistoryRow(item = item, onClick = { onEntryClick(item.entry) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * One list row: weight, date and change from the previous entry.
 *
 * @param item Data for the row.
 * @param onClick Called when the row is tapped.
 * @param modifier Optional modifier from the caller.
 */
@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(WeightFormatter.formatKg(item.entry.weightKg)) },
        supportingContent = { Text(WeightFormatter.formatDate(item.entry.date)) },
        trailingContent = {
            item.changeFromPrevious?.let { change ->
                Text(
                    text = WeightFormatter.formatKgChange(change),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}

/** Message shown when there are no entries yet. */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.history_empty),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Design-time preview with sample data. */
@Preview(showBackground = true)
@Composable
private fun HistoryContentPreview() {
    val entries = listOf(
        WeightEntry(LocalDate.of(2026, 10, 14), 80.6),
        WeightEntry(LocalDate.of(2026, 10, 15), 80.1),
        WeightEntry(LocalDate.of(2026, 10, 16), 79.5),
    )
    WeightTrackerTheme {
        HistoryContent(
            uiState = HistoryUiState(
                items = entries.mapIndexed { i, e ->
                    HistoryItem(e, if (i > 0) e.weightKg - entries[i - 1].weightKg else null)
                }.reversed(),
                isLoading = false,
            ),
            onEntryClick = {},
            onAddClick = {},
            onUndoDelete = {},
            onUndoHandled = {},
        )
    }
}