package com.aljaz.weighttracker.ui.screens.chart

import com.aljaz.weighttracker.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SplitIntoSegmentsTest {
    private fun e(day: Int) = WeightEntry(LocalDate.of(2026, 8, day), 64.0)

    @Test
    fun breaksAtMissingDays() {
        val segments = splitIntoSegments(listOf(e(18), e(19), e(21), e(31)))
        assertEquals(listOf(listOf(e(18), e(19)), listOf(e(21)), listOf(e(31))), segments)
    }

    @Test
    fun emptyList_givesNoSegments() {
        assertEquals(emptyList<List<WeightEntry>>(), splitIntoSegments(emptyList()))
    }
}