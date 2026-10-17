package com.aljaz.weighttracker.data.csv

import com.aljaz.weighttracker.model.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Tests for [WeightCsv]. */
class WeightCsvTest {

    private val day1 = LocalDate.of(2026, 10, 15)
    private val day2 = LocalDate.of(2026, 10, 16)

    @Test
    fun write_producesHeaderAndDotDecimals() {
        val csv = WeightCsv.write(listOf(WeightEntry(day1, 80.0), WeightEntry(day2, 79.5)))
        assertEquals("date,weight_kg\n2026-10-15,80\n2026-10-16,79.5\n", csv)
    }

    @Test
    fun writeThenParse_returnsSameEntries() {
        val entries = listOf(WeightEntry(day1, 80.1), WeightEntry(day2, 79.55))
        val result = WeightCsv.parse(WeightCsv.write(entries))
        assertEquals(entries, result.entries)
        assertTrue(result.skippedLines.isEmpty())
    }

    @Test
    fun parse_excelStyleSemicolonsAndDecimalComma() {
        val result = WeightCsv.parse("date;weight_kg\n2026-10-16;79,5;\n")
        assertEquals(listOf(WeightEntry(day2, 79.5)), result.entries)
    }

    @Test
    fun parse_ignoresBomBlankLinesAndQuotes() {
        val result = WeightCsv.parse("\uFEFFdate,weight_kg\n\n\"2026-10-16\",\"79.5\"\n")
        assertEquals(listOf(WeightEntry(day2, 79.5)), result.entries)
    }

    @Test
    fun parse_reportsInvalidLinesByNumber() {
        val text = "date,weight_kg\n2026-10-15,80\nnot a date,80\n2026-10-16,5\n2026-10-16,79,5\n"
        val result = WeightCsv.parse(text)
        assertEquals(listOf(WeightEntry(day1, 80.0)), result.entries)
        // line 3: bad date, line 4: weight out of range, line 5: ambiguous decimal comma
        assertEquals(listOf(3, 4, 5), result.skippedLines)
    }

    @Test
    fun parse_duplicateDate_lastOneWins() {
        val result = WeightCsv.parse("2026-10-16,80\n2026-10-16,79.5\n")
        assertEquals(listOf(WeightEntry(day2, 79.5)), result.entries)
    }

    @Test
    fun parse_sortsEntriesByDate() {
        val result = WeightCsv.parse("2026-10-16,79.5\n2026-10-15,80\n")
        assertEquals(listOf(day1, day2), result.entries.map { it.date })
    }

    @Test
    fun parse_emptyWeight_isIgnoredNotSkipped() {
        val result = WeightCsv.parse("date,weight_kg\n2026-08-18,64.4\n2026-08-19,\n2026-08-20;\n2026-08-21,63.9")
        assertEquals(listOf(LocalDate.of(2026, 8, 18), LocalDate.of(2026, 8, 21)), result.entries.map { it.date })
        assertEquals(emptyList<Int>(), result.skippedLines)
    }
}