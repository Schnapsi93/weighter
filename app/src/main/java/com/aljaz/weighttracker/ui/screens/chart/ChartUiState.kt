package com.aljaz.weighttracker.ui.screens.chart

import androidx.annotation.StringRes
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.model.WeightEntry
import java.time.LocalDate
import kotlin.math.sqrt


/**
 * Everything the Chart screen displays.
 *
 * @property selectedRange The currently selected time range.
 * @property entries Entries inside the selected range, oldest first.
 * @property stats Statistics for [entries], or null if there are none.
 * @property isLoading True until the first data arrives.
 */
data class ChartUiState(
    val selectedRange: ChartRange = ChartRange.DAYS_30,
    val entries: List<WeightEntry> = emptyList(),
    val stats: ChartStats? = null,
    val isLoading: Boolean = true,
)

/**
 * Time ranges the user can choose for the chart.
 * To add a range (e.g. 7 days), add one line here and a string resource.
 *
 * @property days Number of days to show (including today), or null for all data.
 * @property labelRes Label shown on the filter chip.
 */
enum class ChartRange(val days: Long?, @StringRes val labelRes: Int) {
    DAYS_30(30, R.string.chart_range_30),
    DAYS_90(90, R.string.chart_range_90),
    YEAR(365, R.string.chart_range_365),
    ALL(null, R.string.chart_range_all);

    /**
     * Returns only the entries that fall inside this range.
     *
     * @param entries All entries, oldest first.
     * @param today The current date; the range ends on this day.
     * @return Entries from the last [days] days (today included), oldest first.
     */
    fun filter(entries: List<WeightEntry>, today: LocalDate): List<WeightEntry> {
        val days = days ?: return entries
        val firstDay = today.minusDays(days - 1)
        return entries.filter { !it.date.isBefore(firstDay) }
    }
}

/**
 * Summary numbers for a list of entries (all in kg).
 *
 * @property latest Most recent weight.
 * @property change Latest minus first weight in the period (negative = lost weight).
 * @property lowest Lowest weight in the period.
 * @property highest Highest weight in the period.
 * @property average Mean weight in the period.
 * @property standardDeviation How much the weight typically varies around
 *           [average] in the period (population standard deviation).
 */
data class ChartStats(
    val latest: Double,
    val change: Double,
    val lowest: Double,
    val highest: Double,
    val average: Double,
    val standardDeviation: Double,
) {
    companion object {
        /**
         * Calculates statistics for [entries] (oldest first).
         * @return The statistics, or null if [entries] is empty.
         */
        fun from(entries: List<WeightEntry>): ChartStats? {
            if (entries.isEmpty()) return null
            val weights = entries.map { it.weightKg}
            val average = weights.average()
            // Population standard deviation: the square root of the average
            // squared distance from the mean. It describes this period's data
            // itself and is defined even for a single entry (result 0).

            val variance = weights.sumOf { (it - average) * (it - average) } / weights.size

            return ChartStats(
                latest = weights.last(),
                change = weights.last() - weights.first(),
                lowest = weights.min(),
                highest = weights.max(),
                average = average,
                standardDeviation = sqrt(variance)


            )
        }
    }
}