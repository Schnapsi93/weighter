package com.aljaz.weighttracker.ui.screens.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.repository.WeightRepository
import com.aljaz.weighttracker.ui.components.editor.WeightEditorController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Logic for the Chart (home) screen: filters entries by range, calculates
 * statistics, and lets the user enter or update today's weight.
 *
 * @param repository Source of truth for weight entries.
 * @param today Returns the current date (replaceable in tests).
 */
class ChartViewModel(
    repository: WeightRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** Shared add/edit dialog logic. The screen calls it directly. */
    val editor = WeightEditorController(repository, viewModelScope, today)

    /** The range chosen by the user. */
    private val selectedRange = MutableStateFlow(ChartRange.ALL)

    /** Complete screen state; updates when data, range or dialog state changes. */
    val uiState: StateFlow<ChartUiState> =
        combine(repository.observeEntries(), selectedRange, editor.state) { entries, range, editorState ->
            val date = today()
            val visible = range.filter(entries, date)
            ChartUiState(
                selectedRange = range,
                entries = visible,
                stats = ChartStats.from(visible),
                todayEntry = entries.find { it.date == date },
                editor = editorState,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ChartUiState(),
        )

    /** Called when the user taps a range chip. */
    fun onRangeSelected(range: ChartRange) {
        selectedRange.value = range
    }

    /**
     * Floating button: edits today's entry if it exists,
     * otherwise opens a new entry for today.
     */
    fun onTodayClick() {
        val todayEntry = uiState.value.todayEntry
        if (todayEntry != null) editor.openEdit(todayEntry) else editor.openNew()
    }
}