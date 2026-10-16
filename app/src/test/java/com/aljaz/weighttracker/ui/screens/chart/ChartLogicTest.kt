package com.aljaz.weighttracker.ui.screens.chart

import com.aljaz.weighttracker.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Tests for [ChartRange.filter] and [ChartStats.from]. */
class ChartLogicTest {

    private val today = LocalDate.of(2026, 10, 16)

    private val entries = listOf(
        WeightEntry(today.minusDays(40), 82.0),
        WeightEntry(today.minusDays(29), 81.0),  // first day of the 30-day range
        WeightEntry(today.minusDays(10), 80.0),
        WeightEntry(today, 79.0),
    )

    @Test
    fun days30_includesExactly30DaysWithToday() {
        val result = ChartRange.DAYS_30.filter(entries, today)
        assertEquals(listOf(81.0, 80.0, 79.0), result.map { it.weightKg })
    }

    @Test
    fun all_returnsEveryEntry() {
        assertEquals(entries, ChartRange.ALL.filter(entries, today))
    }

    @Test
    fun stats_areCalculatedCorrectly() {
        val stats = ChartStats.from(entries)!!
        assertEquals(79.0, stats.latest, 0.001)
        assertEquals(-3.0, stats.change, 0.001)
        assertEquals(79.0, stats.lowest, 0.001)
        assertEquals(82.0, stats.highest, 0.001)
        assertEquals(80.5, stats.average, 0.001)
        assertEquals(1.118, stats.standardDeviation, 0.001)
    }

    @Test
    fun stats_ofEmptyList_isNull() {
        assertNull(ChartStats.from(emptyList()))
    }
}