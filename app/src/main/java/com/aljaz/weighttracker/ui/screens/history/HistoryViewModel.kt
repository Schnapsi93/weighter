package com.aljaz.weighttracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.ui.components.editor.WeightEditorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Logic for the History screen: lists entries and handles deleting with Undo.
 * Adding and editing are delegated to the shared [WeightEditorController].
 *
 * @param repository Source of truth for weight entries.
 * @param today Returns the current date (replaceable in tests).
 */
class HistoryViewModel(
    private val repository: WeightRepository,
    today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Shared add/edit dialog logic. The screen calls it directly. */
    val editor = WeightEditorController(repository, viewModelScope, today)

    /** Last deleted entry, kept until the Undo snackbar disappears. */
    private val pendingUndo = MutableStateFlow<WeightEntry?>(null)

    /** Complete screen state, rebuilt whenever data, dialog or undo state changes. */
    val uiState: StateFlow<HistoryUiState> =
        combine(repository.observeEntries(), editor.state, pendingUndo) { entries, editorState, undo ->
            HistoryUiState(
                items = buildItems(entries),
                isLoading = false,
                editor = editorState,
                pendingUndo = undo,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    /** Opens the dialog for adding a new entry. */
    fun onAddClick() = editor.openNew()

    /** Opens the dialog for editing [entry]. */
    fun onEntryClick(entry: WeightEntry) = editor.openEdit(entry)

    /** Deletes the entry being edited and offers Undo. */
    fun onEditorDelete() {
        val deleted = editor.delete() ?: return
        pendingUndo.value = deleted
    }

    /** Restores the last deleted entry. */
    fun onUndoDelete() {
        val entry = pendingUndo.value ?: return
        pendingUndo.value = null
        viewModelScope.launch { repository.saveEntry(entry) }
    }

    /** Called when the Undo snackbar disappeared without being used. */
    fun onUndoHandled() {
        pendingUndo.value = null
    }

    /**
     * Builds list rows (newest first) with the change from the previous entry.
     * @param entries All entries, oldest first.
     */
    private fun buildItems(entries: List<WeightEntry>): List<HistoryItem> =
        entries.mapIndexed { index, entry ->
            HistoryItem(
                entry = entry,
                changeFromPrevious = if (index > 0) {
                    entry.weightKg - entries[index - 1].weightKg
                } else {
                    null
                },
            )
        }.reversed()
}