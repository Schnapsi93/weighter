package com.aljaz.weighttracker.ui.screens.add

import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightInputError
import java.time.LocalDate

/**
 * Everything the "Today" screen displays.
 *
 * @property date The day the entry will be saved for (today).
 * @property input Current text in the weight field.
 * @property inputError Validation error to show under the field, or null.
 * @property todayEntry Already saved entry for [date], or null.
 * @property previousEntry Most recent entry before [date], or null.
 * @property isSaving True while a save is in progress (disables the button).
 * @property saveFailed True if the last save attempt failed.
 */
data class AddWeightUiState(
    val date: LocalDate,
    val input: String = "",
    val inputError: WeightInputError? = null,
    val todayEntry: WeightEntry? = null,
    val previousEntry: WeightEntry? = null,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    /** Difference between today's and the previous entry in kg, or null if either is missing. */
    val changeSincePrevious: Double?
        get() = if (todayEntry != null && previousEntry != null) {
            todayEntry.weightKg - previousEntry.weightKg
        } else {
            null
        }
}