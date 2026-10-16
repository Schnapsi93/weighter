package com.aljaz.weighttracker.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightInputError
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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Logic for the "Today" screen: holds the typed text, validates it
 * and saves today's weight through the [WeightRepository].
 *
 * @param repository Source of truth for weight entries.
 * @param today Returns the current date. A parameter (instead of calling
 *              LocalDate.now() directly) so tests can use a fixed date.
 */
class AddWeightViewModel(
    private val repository: WeightRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** State owned by this screen only (what the user is typing, errors…). */
    private val form = MutableStateFlow(FormState())

    /**
     * The complete screen state: the form state combined with data from
     * the database. Re-emits automatically whenever either one changes,
     * e.g. right after a save completes.
     */
    val uiState: StateFlow<AddWeightUiState> =
        combine(form, repository.observeEntries()) { formState, entries ->
            val date = today()
            AddWeightUiState(
                date = date,
                input = formState.input,
                inputError = formState.inputError,
                isSaving = formState.isSaving,
                saveFailed = formState.saveFailed,
                todayEntry = entries.find { it.date == date },
                previousEntry = entries.lastOrNull { it.date < date },
            )
        }.stateIn(
            scope = viewModelScope,
            // Keeps collecting for 5 s after the screen disappears,
            // so a screen rotation doesn't restart the database query.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AddWeightUiState(date = today()),
        )

    /**
     * Called on every keystroke. Characters that can never form a valid
     * weight (letters, a second separator, 3+ decimals) are ignored.
     *
     * @param newValue The full new text of the field.
     */
    fun onInputChange(newValue: String) {
        if (WeightParser.isAllowedInput(newValue)) {
            form.update { it.copy(input = newValue, inputError = null, saveFailed = false) }
        }
    }

    /** Validates the input and, if valid, saves it as today's weight. */
    fun onSave() {
        if (form.value.isSaving) return  // ignore double taps

        when (val result = WeightParser.parse(form.value.input)) {
            is WeightParseResult.Invalid ->
                form.update { it.copy(inputError = result.error) }

            is WeightParseResult.Valid ->
                save(result.weightKg)
        }
    }

    /**
     * Saves [weightKg] for today in the background.
     * On success the field is cleared; the new value then appears on screen
     * automatically through [uiState], because the database Flow re-emits.
     */
    private fun save(weightKg: Double) {
        form.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.saveEntry(WeightEntry(date = today(), weightKg = weightKg))
                form.value = FormState()
            } catch (e: CancellationException) {
                throw e  // never swallow coroutine cancellation
            } catch (e: Exception) {
                form.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /** Screen-only state that does not come from the database. */
    private data class FormState(
        val input: String = "",
        val inputError: WeightInputError? = null,
        val isSaving: Boolean = false,
        val saveFailed: Boolean = false,
    )


}