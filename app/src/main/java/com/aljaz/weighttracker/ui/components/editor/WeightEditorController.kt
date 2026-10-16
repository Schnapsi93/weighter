package com.aljaz.weighttracker.ui.components.editor

import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightFormatter
import com.aljaz.weighttracker.util.WeightParseResult
import com.aljaz.weighttracker.util.WeightParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Reusable logic behind [WeightEditorDialog]: opening, typing, validating,
 * saving and deleting an entry.
 *
 * It is a plain class owned by a ViewModel (not a ViewModel itself).
 * The owner passes its `viewModelScope` as [scope], so any running save
 * is cancelled together with that ViewModel.
 *
 * To give a new screen the dialog: create one controller in its ViewModel,
 * include [state] in the screen's UI state, and show [WeightEditorDialog].
 *
 * @param repository Where entries are saved and deleted.
 * @param scope Coroutine scope of the owning ViewModel.
 * @param today Returns the current date (replaceable in tests).
 */
class WeightEditorController(
    private val repository: WeightRepository,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate,
) {
    /** Raw dialog state; null means the dialog is closed. */
    private val editor = MutableStateFlow<EditorState?>(null)

    /**
     * Dialog state for the UI, with [EditorState.replacedEntry] filled in
     * when a new entry would overwrite an existing day.
     */
    val state: Flow<EditorState?> =
        combine(editor, repository.observeEntries()) { current, entries ->
            if (current != null && current.isNewEntry) {
                current.copy(replacedEntry = entries.find { it.date == current.date })
            } else {
                current
            }
        }

    /** Opens the dialog for a new entry dated today. */
    fun openNew() {
        editor.value = EditorState(date = today(), input = "")
    }

    /** Opens the dialog for editing [entry], pre-filled with its weight. */
    fun openEdit(entry: WeightEntry) {
        editor.value = EditorState(
            date = entry.date,
            input = WeightFormatter.formatForInput(entry.weightKg),
            originalEntry = entry,
        )
    }

    /** Changes the date of a new entry (from the date picker). */
    fun onDateChange(date: LocalDate) {
        editor.update { it?.copy(date = date) }
    }

    /** Updates the weight text, ignoring characters that can't form a weight. */
    fun onInputChange(text: String) {
        if (WeightParser.isAllowedInput(text)) {
            editor.update { it?.copy(input = text, inputError = null) }
        }
    }

    /** Closes the dialog without saving. */
    fun dismiss() {
        editor.value = null
    }

    /** Validates the input; if valid, saves the entry and closes the dialog. */
    fun save() {
        val current = editor.value ?: return
        when (val result = WeightParser.parse(current.input)) {
            is WeightParseResult.Invalid ->
                editor.update { it?.copy(inputError = result.error) }

            is WeightParseResult.Valid -> {
                editor.value = null
                scope.launch {
                    repository.saveEntry(WeightEntry(current.date, result.weightKg))
                }
            }
        }
    }

    /**
     * Deletes the entry being edited and closes the dialog.
     * @return The deleted entry (e.g. to offer Undo), or null if nothing was deleted.
     */
    fun delete(): WeightEntry? {
        val entry = editor.value?.originalEntry ?: return null
        editor.value = null
        scope.launch { repository.deleteEntry(entry.date) }
        return entry
    }
}