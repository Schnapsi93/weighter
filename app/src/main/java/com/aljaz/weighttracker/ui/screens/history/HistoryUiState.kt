package com.aljaz.weighttracker.ui.screens.history

import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.ui.components.editor.EditorState


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
