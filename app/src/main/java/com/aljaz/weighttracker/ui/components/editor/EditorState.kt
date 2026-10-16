package com.aljaz.weighttracker.ui.components.editor

import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightInputError
import java.time.LocalDate



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