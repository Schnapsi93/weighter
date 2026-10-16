package com.aljaz.weighttracker.ui.screens.history

import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightInputError
import java.time.LocalDate

/**
 * Everything the History screen displays.
 *
 * @property items Entries to list, newest first.
 * @property isLoading True until the first data arrives from the database.
 * @property editor State of the add/edit dialog, or null when it is closed.
 * @property pendingUndo Entry that was just deleted and can still be restored, or null.
 */
data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val editor: EditorState? = null,
    val pendingUndo: WeightEntry? = null,
)

/**
 * One row in the history list.
 *
 * @property entry The weight entry.
 * @property changeFromPrevious Difference to the previous (older) entry in kg,
 *           or null for the oldest entry.
 */
data class HistoryItem(
    val entry: WeightEntry,
    val changeFromPrevious: Double?,
)

/**
 * State of the add/edit dialog.
 *
 * @property date Day the entry is for. Changeable only when adding.
 * @property input Text in the weight field.
 * @property inputError Validation error, or null.
 * @property originalEntry The entry being edited; null when adding a new one.
 * @property replacedEntry When adding: an existing entry on [date] that saving
 *           would overwrite (shown as a warning), otherwise null.
 */
data class EditorState(
    val date: LocalDate,
    val input: String,
    val inputError: WeightInputError? = null,
    val originalEntry: WeightEntry? = null,
    val replacedEntry: WeightEntry? = null,
) {
    /** True when adding a new entry, false when editing an existing one. */
    val isNewEntry: Boolean get() = originalEntry == null
}