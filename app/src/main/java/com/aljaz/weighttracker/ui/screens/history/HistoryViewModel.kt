package com.aljaz.weighttracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightFormatter
import com.aljaz.weighttracker.util.WeightParseResult
import com.aljaz.weighttracker.util.WeightParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Logic for the History screen: lists entries and handles adding,
 * editing, deleting and undoing a delete.
 *
 * @param repository Source of truth for weight entries.
 * @param today Returns the current date (replaceable in tests).
 */
class HistoryViewModel(
    private val repository: WeightRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Open dialog state, or null when no dialog is shown. */
    private val editor = MutableStateFlow<EditorState?>(null)

    /** Last deleted entry, kept until the Undo snackbar disappears. */
    private val pendingUndo = MutableStateFlow<WeightEntry?>(null)

    /** Complete screen state, rebuilt whenever data or dialog state changes. */
    val uiState: StateFlow<HistoryUiState> =
        combine(repository.observeEntries(), editor, pendingUndo) { entries, editorState, undo ->
            HistoryUiState(
                items = buildItems(entries),
                isLoading = false,
                editor = editorState?.withReplacedEntry(entries),
                pendingUndo = undo,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    // ---------- List actions ----------

    /** Opens the dialog for adding a new entry (date defaults to today). */
    fun onAddClick() {
        editor.value = EditorState(date = today(), input = "")
    }

    /** Opens the dialog for editing [entry], pre-filled with its weight. */
    fun onEntryClick(entry: WeightEntry) {
        editor.value = EditorState(
            date = entry.date,
            input = WeightFormatter.formatForInput(entry.weightKg),
            originalEntry = entry,
        )
    }

    // ---------- Dialog actions ----------

    /** Changes the date of a new entry (from the date picker). */
    fun onEditorDateChange(date: LocalDate) {
        editor.update { it?.copy(date = date) }
    }

    /** Updates the weight text, ignoring characters that can't form a weight. */
    fun onEditorInputChange(text: String) {
        if (WeightParser.isAllowedInput(text)) {
            editor.update { it?.copy(input = text, inputError = null) }
        }
    }

    /** Closes the dialog without saving. */
    fun onEditorDismiss() {
        editor.value = null
    }

    /** Validates the input; if valid, saves the entry and closes the dialog. */
    fun onEditorSave() {
        val current = editor.value ?: return
        when (val result = WeightParser.parse(current.input)) {
            is WeightParseResult.Invalid ->
                editor.update { it?.copy(inputError = result.error) }

            is WeightParseResult.Valid -> {
                editor.value = null
                viewModelScope.launch {
                    repository.saveEntry(WeightEntry(current.date, result.weightKg))
                }
            }
        }
    }

    /** Deletes the entry being edited and offers Undo. */
    fun onEditorDelete() {
        val entry = editor.value?.originalEntry ?: return
        editor.value = null
        viewModelScope.launch {
            repository.deleteEntry(entry.date)
            pendingUndo.value = entry
        }
    }

    // ---------- Undo ----------

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

    // ---------- Helpers ----------

    /**
     * Builds list rows (newest first) with the change from the previous day.
     * @param entries All entries, oldest first (as the repository returns them).
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

    /** When adding, finds an existing entry on the chosen date that would be overwritten. */
    private fun EditorState.withReplacedEntry(entries: List<WeightEntry>): EditorState =
        if (isNewEntry) copy(replacedEntry = entries.find { it.date == date }) else this
}