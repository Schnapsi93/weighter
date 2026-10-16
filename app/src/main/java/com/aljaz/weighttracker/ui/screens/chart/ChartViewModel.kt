package com.aljaz.weighttracker.ui.screens.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aljaz.weighttracker.data.repository.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Logic for the Chart screen: filters entries by the selected range
 * and calculates statistics.
 *
 * @param repository Source of truth for weight entries.
 * @param today Returns the current date (replaceable in tests).
 */
class ChartViewModel(
    repository: WeightRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    /** The range chosen by the user. */
    private val selectedRange = MutableStateFlow(ChartRange.DAYS_30)

    /** Complete screen state; updates when data or the selected range changes. */
    val uiState: StateFlow<ChartUiState> =
        combine(repository.observeEntries(), selectedRange) { entries, range ->
            val visible = range.filter(entries, today())
            ChartUiState(
                selectedRange = range,
                entries = visible,
                stats = ChartStats.from(visible),
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
}